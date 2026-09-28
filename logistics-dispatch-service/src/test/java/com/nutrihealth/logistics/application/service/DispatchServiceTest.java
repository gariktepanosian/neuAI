package com.nutrihealth.logistics.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nutrihealth.logistics.domain.model.CourierAssignment;
import com.nutrihealth.logistics.domain.model.GeoPoint;
import com.nutrihealth.logistics.domain.model.NoAvailableCourierException;
import com.nutrihealth.logistics.domain.port.out.CourierLocationRepositoryPort;
import com.nutrihealth.logistics.domain.port.out.CourierLocationRepositoryPort.NearbyCourier;
import com.nutrihealth.logistics.domain.port.out.DeliveryTrackingPublisherPort;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DispatchServiceTest {

    private final UUID nearestCourierId = UUID.randomUUID();

    private final CourierLocationRepositoryPort repositoryWithCandidates = new CourierLocationRepositoryPort() {
        @Override
        public void updateLocation(UUID courierId, GeoPoint location) {
        }

        @Override
        public void markAvailable(UUID courierId) {
        }

        @Override
        public void markUnavailable(UUID courierId) {
        }

        @Override
        public List<NearbyCourier> findNearestAvailableCouriers(GeoPoint origin, double radiusKm, int limit) {
            return List.of(new NearbyCourier(nearestCourierId, 2.5));
        }
    };

    private final CourierLocationRepositoryPort repositoryWithNoCandidates = new CourierLocationRepositoryPort() {
        @Override
        public void updateLocation(UUID courierId, GeoPoint location) {
        }

        @Override
        public void markAvailable(UUID courierId) {
        }

        @Override
        public void markUnavailable(UUID courierId) {
        }

        @Override
        public List<NearbyCourier> findNearestAvailableCouriers(GeoPoint origin, double radiusKm, int limit) {
            return List.of();
        }
    };

    private CourierAssignment lastPublishedAssignment;
    private final DeliveryTrackingPublisherPort trackingPublisher = new DeliveryTrackingPublisherPort() {
        @Override
        public void publishCourierAssigned(CourierAssignment assignment) {
            lastPublishedAssignment = assignment;
        }

        @Override
        public void publishCourierLocationUpdate(UUID courierId, GeoPoint location) {
        }
    };

    @Test
    void dispatchesNearestCourierAndPublishesAssignment() {
        DispatchService service = new DispatchService(repositoryWithCandidates, trackingPublisher, 10);
        UUID deliveryId = UUID.randomUUID();

        CourierAssignment assignment = service.dispatchNearestCourier(deliveryId, new GeoPoint(0, 0));

        assertThat(assignment.courierId()).isEqualTo(nearestCourierId);
        assertThat(assignment.deliveryId()).isEqualTo(deliveryId);
        assertThat(assignment.distanceKm()).isEqualTo(2.5);
        assertThat(lastPublishedAssignment).isEqualTo(assignment);
    }

    @Test
    void throwsWhenNoCourierIsAvailable() {
        DispatchService service = new DispatchService(repositoryWithNoCandidates, trackingPublisher, 10);

        assertThatThrownBy(() -> service.dispatchNearestCourier(UUID.randomUUID(), new GeoPoint(0, 0)))
                .isInstanceOf(NoAvailableCourierException.class);
    }
}
