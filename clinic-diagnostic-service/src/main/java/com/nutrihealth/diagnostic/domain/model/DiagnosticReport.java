package com.nutrihealth.diagnostic.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Core domain aggregate for a single lab diagnostic report, independent of
 * FHIR, HL7 v2, or persistence framework concerns (Hexagonal Architecture).
 */
public class DiagnosticReport {

    private final UUID id;
    private final UUID userId;
    private final UUID clinicId;
    private final Instant testDate;
    private final List<BiomarkerObservation> observations;

    public DiagnosticReport(UUID id, UUID userId, UUID clinicId, Instant testDate,
                             List<BiomarkerObservation> observations) {
        this.id = id;
        this.userId = userId;
        this.clinicId = clinicId;
        this.testDate = testDate;
        this.observations = observations;
    }

    public boolean requiresVitaminDAdjustment() {
        return observations.stream()
                .anyMatch(obs -> "2888-6".equals(obs.getLoincCode())
                        && obs.getInterpretation() == InterpretationCode.LOW);
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getClinicId() {
        return clinicId;
    }

    public Instant getTestDate() {
        return testDate;
    }

    public List<BiomarkerObservation> getObservations() {
        return observations;
    }
}
