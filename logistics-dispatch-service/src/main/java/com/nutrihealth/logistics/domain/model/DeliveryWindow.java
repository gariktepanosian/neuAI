package com.nutrihealth.logistics.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Represents an available delivery window for a given date.
 *
 * <p>A window is a half-open time interval [{@link #start}, {@link #end}) on a
 * specific {@link #date}.  Windows are comparable by start time for scheduling
 * purposes.
 */
public record DeliveryWindow(
        LocalDate date,
        LocalTime start,
        LocalTime end,
        int capacity,        // number of deliveries this window can accept
        int bookedCount      // how many have already been booked in this window
) implements Comparable<DeliveryWindow> {

    /** Remaining capacity in this window. */
    public int remainingCapacity() {
        return Math.max(0, capacity - bookedCount);
    }

    /** True if there is at least one free slot. */
    public boolean hasCapacity() {
        return remainingCapacity() > 0;
    }

    /** A load factor in [0, 1]: 0 = completely free, 1 = fully booked. */
    public double loadFactor() {
        return capacity == 0 ? 1.0 : (double) bookedCount / capacity;
    }

    @Override
    public int compareTo(DeliveryWindow other) {
        int dateCmp = this.date.compareTo(other.date);
        return dateCmp != 0 ? dateCmp : this.start.compareTo(other.start);
    }

    /**
     * Standard operating windows for a typical delivery day (08:00–20:00,
     * 2-hour slots). Used as the default slot schedule when no custom slots
     * are configured.
     */
    public static List<DeliveryWindow> defaultWindows(LocalDate date) {
        return List.of(
                new DeliveryWindow(date, LocalTime.of(8,  0), LocalTime.of(10, 0), 20, 0),
                new DeliveryWindow(date, LocalTime.of(10, 0), LocalTime.of(12, 0), 20, 0),
                new DeliveryWindow(date, LocalTime.of(12, 0), LocalTime.of(14, 0), 20, 0),
                new DeliveryWindow(date, LocalTime.of(14, 0), LocalTime.of(16, 0), 20, 0),
                new DeliveryWindow(date, LocalTime.of(16, 0), LocalTime.of(18, 0), 20, 0),
                new DeliveryWindow(date, LocalTime.of(18, 0), LocalTime.of(20, 0), 20, 0)
        );
    }
}
