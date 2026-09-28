package com.nutrihealth.logistics.domain.port.in;

import com.nutrihealth.logistics.domain.model.CourierAssignment;
import com.nutrihealth.logistics.domain.model.GeoPoint;
import java.util.UUID;

public interface DispatchCourierUseCase {

    void recordCourierLocation(UUID courierId, GeoPoint location);

    void markCourierAvailable(UUID courierId);

    void markCourierUnavailable(UUID courierId);

    /** Finds and assigns the nearest available courier within the configured search radius. */
    CourierAssignment dispatchNearestCourier(UUID deliveryId, GeoPoint deliveryLocation);
}
