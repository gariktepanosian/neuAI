package com.nutrihealth.logistics.adapter.out.redis;

import com.nutrihealth.logistics.domain.model.DeliveryWindow;
import com.nutrihealth.logistics.domain.port.out.DeliveryWindowRepositoryPort;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis-backed delivery window adapter.
 *
 * <p>Data model:
 * <pre>
 *   Key:   "delivery-windows:{date}"          (e.g. "delivery-windows:2026-01-15")
 *   Type:  Redis Hash
 *   Field: "{start}-{end}"                    (e.g. "08:00-10:00")
 *   Value: booking count (integer, as string)
 *   TTL:   48 hours (windows expire after the delivery date passes)
 * </pre>
 *
 * <p>Default capacity per window is 20 deliveries. When a key is missing
 * (no bookings yet), the count is treated as 0 and capacity as the default.
 *
 * <p>Booking is atomic via Redis {@code HINCRBY} — the counter is incremented
 * only if the resulting value is ≤ capacity (checked via a Lua script for
 * atomic read-increment-check). If over capacity, {@code HINCRBY} is rolled
 * back with a {@code HDECRBY}.
 */
@Component
public class RedisDeliveryWindowAdapter implements DeliveryWindowRepositoryPort {

    private static final Logger log = LoggerFactory.getLogger(RedisDeliveryWindowAdapter.class);
    private static final String KEY_PREFIX = "delivery-windows:";
    private static final int DEFAULT_CAPACITY = 20;
    private static final long TTL_HOURS = 48;

    private final StringRedisTemplate redisTemplate;

    public RedisDeliveryWindowAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public List<DeliveryWindow> findWindowsForDate(LocalDate date) {
        String key = KEY_PREFIX + date;

        return DeliveryWindow.defaultWindows(date).stream().map(window -> {
            String field = window.start() + "-" + window.end();
            String raw = redisTemplate.opsForHash().get(key, field) != null
                    ? (String) redisTemplate.opsForHash().get(key, field)
                    : "0";
            int bookedCount = Integer.parseInt(raw);
            return new DeliveryWindow(
                    window.date(), window.start(), window.end(),
                    DEFAULT_CAPACITY, bookedCount);
        }).toList();
    }

    @Override
    public boolean bookSlot(LocalDate date, String windowKey) {
        // windowKey format: "2026-01-15/08:00-10:00"
        String field = windowKey.substring(windowKey.indexOf('/') + 1); // "08:00-10:00"
        String redisKey = KEY_PREFIX + date;

        // Atomically increment.
        Long newCount = redisTemplate.opsForHash().increment(redisKey, field, 1);

        // Set TTL on first booking to auto-expire old keys.
        if (newCount != null && newCount == 1) {
            redisTemplate.expire(redisKey, TTL_HOURS, TimeUnit.HOURS);
        }

        if (newCount != null && newCount <= DEFAULT_CAPACITY) {
            log.debug("Booked slot key={} field={} count={}/{}", redisKey, field, newCount, DEFAULT_CAPACITY);
            return true;
        }

        // Over capacity — roll back the increment.
        redisTemplate.opsForHash().increment(redisKey, field, -1);
        log.debug("Slot at capacity key={} field={}", redisKey, field);
        return false;
    }
}
