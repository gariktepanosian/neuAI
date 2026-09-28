package com.nutrihealth.logistics.domain.port.out;

import com.nutrihealth.logistics.domain.model.CourierAssignment;
import com.nutrihealth.logistics.domain.model.GeoPoint;
import java.util.UUID;

/**
 * Outbound port for the Redis pub/sub live-tracking stream the mobile app's
 * courier map subscribes to, per the spec's real-time tracking module.
 */
public interface DeliveryTrackingPublisherPort {

    void publishCourierAssigned(CourierAssignment assignment);

    void publishCourierLocationUpdate(UUID courierId, GeoPoint location);
}
