package com.nutrihealth.logistics.domain.port.out;

import com.nutrihealth.logistics.domain.model.GeoPoint;
import java.util.List;
import java.util.UUID;

public interface CourierLocationRepositoryPort {

    void updateLocation(UUID courierId, GeoPoint location);

    void markAvailable(UUID courierId);

    void markUnavailable(UUID courierId);

    /** Available couriers within {@code radiusKm} of {@code origin}, nearest first. */
    List<NearbyCourier> findNearestAvailableCouriers(GeoPoint origin, double radiusKm, int limit);

    record NearbyCourier(UUID courierId, double distanceKm) {
    }
}
