package com.nutrihealth.logistics.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class GeoPointTest {

    @Test
    void distanceToSelfIsZero() {
        GeoPoint point = new GeoPoint(40.7128, -74.0060);

        assertThat(point.distanceKm(point)).isCloseTo(0.0, within(0.0001));
    }

    @Test
    void distanceBetweenNewYorkAndLosAngelesIsApproximatelyKnownValue() {
        GeoPoint newYork = new GeoPoint(40.7128, -74.0060);
        GeoPoint losAngeles = new GeoPoint(34.0522, -118.2437);

        double distance = newYork.distanceKm(losAngeles);

        // Well-known great-circle distance is ~3936 km.
        assertThat(distance).isCloseTo(3936, within(20.0));
    }

    @Test
    void distanceIsSymmetric() {
        GeoPoint a = new GeoPoint(51.5074, -0.1278);
        GeoPoint b = new GeoPoint(48.8566, 2.3522);

        assertThat(a.distanceKm(b)).isCloseTo(b.distanceKm(a), within(0.0001));
    }

    @Test
    void rejectsLatitudeOutsideRedisGeoRange() {
        assertThatThrownBy(() -> new GeoPoint(86.0, 0.0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsLongitudeOutOfRange() {
        assertThatThrownBy(() -> new GeoPoint(0.0, 181.0)).isInstanceOf(IllegalArgumentException.class);
    }
}
