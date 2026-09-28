package com.nutrihealth.logistics.application.service;

import com.nutrihealth.logistics.domain.model.CourierAssignment;
import com.nutrihealth.logistics.domain.model.GeoPoint;
import com.nutrihealth.logistics.domain.model.NoAvailableCourierException;
import com.nutrihealth.logistics.domain.port.in.DispatchCourierUseCase;
import com.nutrihealth.logistics.domain.port.out.CourierLocationRepositoryPort;
import com.nutrihealth.logistics.domain.port.out.CourierLocationRepositoryPort.NearbyCourier;
import com.nutrihealth.logistics.domain.port.out.DeliveryTrackingPublisherPort;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DispatchService implements DispatchCourierUseCase {

    private final CourierLocationRepositoryPort courierLocationRepository;
    private final DeliveryTrackingPublisherPort trackingPublisher;
    private final double searchRadiusKm;

    public DispatchService(CourierLocationRepositoryPort courierLocationRepository,
                            DeliveryTrackingPublisherPort trackingPublisher,
                            @Value("${nutrihealth.dispatch.search-radius-km:10}") double searchRadiusKm) {
        this.courierLocationRepository = courierLocationRepository;
        this.trackingPublisher = trackingPublisher;
        this.searchRadiusKm = searchRadiusKm;
    }

    @Override
    public void recordCourierLocation(UUID courierId, GeoPoint location) {
        courierLocationRepository.updateLocation(courierId, location);
        trackingPublisher.publishCourierLocationUpdate(courierId, location);
    }

    @Override
    public void markCourierAvailable(UUID courierId) {
        courierLocationRepository.markAvailable(courierId);
    }

    @Override
    public void markCourierUnavailable(UUID courierId) {
        courierLocationRepository.markUnavailable(courierId);
    }

    @Override
    public CourierAssignment dispatchNearestCourier(UUID deliveryId, GeoPoint deliveryLocation) {
        List<NearbyCourier> candidates =
                courierLocationRepository.findNearestAvailableCouriers(deliveryLocation, searchRadiusKm, 1);

        NearbyCourier nearest = candidates.stream().findFirst()
                .orElseThrow(() -> new NoAvailableCourierException(searchRadiusKm));

        CourierAssignment assignment = new CourierAssignment(deliveryId, nearest.courierId(), nearest.distanceKm());
        trackingPublisher.publishCourierAssigned(assignment);
        return assignment;
    }
}
