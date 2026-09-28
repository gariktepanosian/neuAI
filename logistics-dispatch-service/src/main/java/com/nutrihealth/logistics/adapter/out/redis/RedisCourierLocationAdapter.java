package com.nutrihealth.logistics.adapter.out.redis;

import com.nutrihealth.logistics.domain.model.GeoPoint;
import com.nutrihealth.logistics.domain.port.out.CourierLocationRepositoryPort;
import java.util.List;
import java.util.UUID;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Tracks live courier positions in Redis using the GEO commands described in
 * the spec (Memorystore/Redis Cluster geo-location tracking). Availability is
 * tracked separately as a Redis set so a courier can drop out of dispatch
 * consideration without losing their last known position.
 */
@Component
public class RedisCourierLocationAdapter implements CourierLocationRepositoryPort {

    static final String GEO_KEY = "couriers:geo";
    static final String AVAILABLE_SET_KEY = "couriers:available";

    private final StringRedisTemplate redisTemplate;

    public RedisCourierLocationAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void updateLocation(UUID courierId, GeoPoint location) {
        redisTemplate.opsForGeo().add(GEO_KEY, new Point(location.longitude(), location.latitude()), courierId.toString());
    }

    @Override
    public void markAvailable(UUID courierId) {
        redisTemplate.opsForSet().add(AVAILABLE_SET_KEY, courierId.toString());
    }

    @Override
    public void markUnavailable(UUID courierId) {
        redisTemplate.opsForSet().remove(AVAILABLE_SET_KEY, courierId.toString());
    }

    @Override
    public List<NearbyCourier> findNearestAvailableCouriers(GeoPoint origin, double radiusKm, int limit) {
        Circle searchArea = new Circle(new Point(origin.longitude(), origin.latitude()),
                new Distance(radiusKm, Metrics.KILOMETERS));

        // Over-fetch: some results may be filtered out below for being unavailable.
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                .includeDistance()
                .sortAscending()
                .limit(Math.max(limit * 5L, 50));

        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                redisTemplate.opsForGeo().radius(GEO_KEY, searchArea, args);
        if (results == null) {
            return List.of();
        }

        return results.getContent().stream()
                .filter(result -> Boolean.TRUE.equals(
                        redisTemplate.opsForSet().isMember(AVAILABLE_SET_KEY, result.getContent().getName())))
                .limit(limit)
                .map(result -> new NearbyCourier(
                        UUID.fromString(result.getContent().getName()), result.getDistance().getValue()))
                .toList();
    }
}
