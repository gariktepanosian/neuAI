package com.nutrihealth.logistics.domain.model;

import java.util.UUID;

public record CourierAssignment(UUID deliveryId, UUID courierId, double distanceKm) {
}
