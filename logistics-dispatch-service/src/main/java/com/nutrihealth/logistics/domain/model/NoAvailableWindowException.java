package com.nutrihealth.logistics.domain.model;

/**
 * Thrown when no delivery window with remaining capacity can be found for
 * the requested date range.
 */
public class NoAvailableWindowException extends RuntimeException {

    public NoAvailableWindowException(String message) {
        super(message);
    }
}
