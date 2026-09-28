package com.nutrihealth.diagnostic.domain.port.out;

import com.nutrihealth.diagnostic.domain.model.DiagnosticReport;

/**
 * Outbound port for propagating newly ingested diagnostic results to the rest
 * of the platform (Kafka topic in production) so the AI nutrition engine can
 * react to new biomarker data, per the event-driven architecture principle.
 */
public interface DietaryAdjustmentEventPublisherPort {

    void publishReportIngested(DiagnosticReport report);
}
