package com.nutrihealth.logistics.domain.model;

public class NoAvailableCourierException extends RuntimeException {

    public NoAvailableCourierException(double searchRadiusKm) {
        super("No available courier found within " + searchRadiusKm + " km");
    }
}
