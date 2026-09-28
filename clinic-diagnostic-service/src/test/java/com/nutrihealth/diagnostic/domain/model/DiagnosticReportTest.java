package com.nutrihealth.diagnostic.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DiagnosticReportTest {

    @Test
    void flagsVitaminDAdjustmentWhenLoincCodeLowAndInterpretationLow() {
        BiomarkerObservation lowVitaminD = new BiomarkerObservation(
                "2888-6", "25-hydroxyvitamin D3", 22.4, "ng/mL", InterpretationCode.LOW);
        DiagnosticReport report = new DiagnosticReport(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Instant.now(), List.of(lowVitaminD));

        assertThat(report.requiresVitaminDAdjustment()).isTrue();
    }

    @Test
    void doesNotFlagWhenVitaminDIsNormal() {
        BiomarkerObservation normalVitaminD = new BiomarkerObservation(
                "2888-6", "25-hydroxyvitamin D3", 35.0, "ng/mL", InterpretationCode.NORMAL);
        DiagnosticReport report = new DiagnosticReport(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Instant.now(), List.of(normalVitaminD));

        assertThat(report.requiresVitaminDAdjustment()).isFalse();
    }

    @Test
    void doesNotFlagWhenNoObservationsPresent() {
        DiagnosticReport report = new DiagnosticReport(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Instant.now(), List.of());

        assertThat(report.requiresVitaminDAdjustment()).isFalse();
    }
}
