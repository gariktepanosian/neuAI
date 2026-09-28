package com.nutrihealth.diagnostic.adapter.in.web;

import com.nutrihealth.diagnostic.domain.model.DiagnosticReport;
import java.time.Instant;
import java.util.UUID;

public record DiagnosticReportResponse(UUID id, UUID userId, UUID clinicId,
                                        Instant testDate, int observationCount,
                                        boolean requiresVitaminDAdjustment) {

    public static DiagnosticReportResponse from(DiagnosticReport report) {
        return new DiagnosticReportResponse(
                report.getId(),
                report.getUserId(),
                report.getClinicId(),
                report.getTestDate(),
                report.getObservations().size(),
                report.requiresVitaminDAdjustment());
    }
}
