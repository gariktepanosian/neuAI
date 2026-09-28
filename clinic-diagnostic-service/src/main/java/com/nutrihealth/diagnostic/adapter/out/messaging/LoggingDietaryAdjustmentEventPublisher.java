package com.nutrihealth.diagnostic.adapter.out.messaging;

import com.nutrihealth.diagnostic.domain.model.DiagnosticReport;
import com.nutrihealth.diagnostic.domain.port.out.DietaryAdjustmentEventPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Placeholder outbound adapter. Logs instead of publishing to Kafka because
 * the Kafka cluster (spec Phase 1, Step 1.3) has not been provisioned yet.
 * Swap for a KafkaTemplate-backed adapter, keeping the same port, once the
 * cluster and topic (`diagnostic.report.ingested`) exist.
 */
@Component
public class LoggingDietaryAdjustmentEventPublisher implements DietaryAdjustmentEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingDietaryAdjustmentEventPublisher.class);

    @Override
    public void publishReportIngested(DiagnosticReport report) {
        log.info("diagnostic.report.ingested userId={} clinicId={} requiresVitaminDAdjustment={}",
                report.getUserId(), report.getClinicId(), report.requiresVitaminDAdjustment());
    }
}
