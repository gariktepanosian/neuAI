package com.nutrihealth.diagnostic.adapter.out.messaging;

import com.nutrihealth.diagnostic.domain.model.DiagnosticReport;
import com.nutrihealth.diagnostic.domain.port.out.DietaryAdjustmentEventPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fallback adapter — not registered as a Spring bean.
 * Used only in unit tests that want a no-op publisher stub.
 * In production, {@link KafkaDietaryAdjustmentEventPublisher} is the active bean.
 */
public class LoggingDietaryAdjustmentEventPublisher implements DietaryAdjustmentEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingDietaryAdjustmentEventPublisher.class);

    @Override
    public void publishReportIngested(DiagnosticReport report) {
        log.info("diagnostic.report.ingested userId={} clinicId={} requiresVitaminDAdjustment={}",
                report.getUserId(), report.getClinicId(), report.requiresVitaminDAdjustment());
    }
}
