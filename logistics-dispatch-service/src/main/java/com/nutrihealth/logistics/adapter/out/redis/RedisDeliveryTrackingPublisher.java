package com.nutrihealth.logistics.adapter.out.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutrihealth.logistics.domain.model.CourierAssignment;
import com.nutrihealth.logistics.domain.model.GeoPoint;
import com.nutrihealth.logistics.domain.port.out.DeliveryTrackingPublisherPort;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes to the Redis pub/sub channel the mobile app's live courier map
 * subscribes to (spec: "Real-Time Courier Tracking Module ... connected to
 * WebSocket/Redis pub-sub stream").
 */
@Component
public class RedisDeliveryTrackingPublisher implements DeliveryTrackingPublisherPort {

    static final String COURIER_LOCATION_CHANNEL = "delivery-tracking:courier-location";
    static final String COURIER_ASSIGNED_CHANNEL = "delivery-tracking:courier-assigned";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisDeliveryTrackingPublisher(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishCourierAssigned(CourierAssignment assignment) {
        publish(COURIER_ASSIGNED_CHANNEL, Map.of(
                "deliveryId", assignment.deliveryId().toString(),
                "courierId", assignment.courierId().toString(),
                "distanceKm", assignment.distanceKm()));
    }

    @Override
    public void publishCourierLocationUpdate(UUID courierId, GeoPoint location) {
        publish(COURIER_LOCATION_CHANNEL, Map.of(
                "courierId", courierId.toString(),
                "latitude", location.latitude(),
                "longitude", location.longitude()));
    }

    private void publish(String channel, Map<String, ?> payload) {
        try {
            redisTemplate.convertAndSend(channel, objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to publish delivery tracking event", e);
        }
    }
}
