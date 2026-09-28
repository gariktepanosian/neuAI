package com.nutrihealth.diagnostic.adapter.out.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutrihealth.diagnostic.domain.model.DiagnosticReport;
import com.nutrihealth.diagnostic.domain.port.out.DietaryAdjustmentEventPublisherPort;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

/**
 * Publishes {@code diagnostic.report.ingested} events to Kafka whenever a
 * FHIR or HL7 v2 diagnostic report is successfully persisted.
 *
 * <p>Topic: {@code diagnostic.report.ingested} (6 partitions, 3 replicas).
 * Matching DLQ: {@code diagnostic.report.ingested.dlq} — Spring Kafka's
 * {@code DefaultErrorHandler} with {@code DeadLetterPublishingRecoverer} should
 * be configured on any consumer of the main topic to route failed messages there.
 *
 * <p>Message key = {@code userId} (UUID) so all reports for the same patient
 * land on the same partition and can be consumed in order by the
 * ai-nutrition-engine-service.
 */
@Component
public class KafkaDietaryAdjustmentEventPublisher implements DietaryAdjustmentEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(KafkaDietaryAdjustmentEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public KafkaDietaryAdjustmentEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${nutrihealth.kafka.topic.report-ingested:diagnostic.report.ingested}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Override
    public void publishReportIngested(DiagnosticReport report) {
        String key = report.getUserId();
        String payload;
        try {
            payload = objectMapper.writeValueAsString(Map.of(
                    "eventType", "diagnostic.report.ingested",
                    "reportId", report.getId(),
                    "userId", report.getUserId(),
                    "clinicId", report.getClinicId() != null ? report.getClinicId() : "",
                    "requiresVitaminDAdjustment", report.requiresVitaminDAdjustment(),
                    "observationCount", report.getObservations().size(),
                    "occurredAt", Instant.now().toString()
            ));
        } catch (Exception e) {
            log.error("Failed to serialise Kafka payload for diagnostic report userId={}: {}",
                    key, e.getMessage(), e);
            return;
        }

        kafkaTemplate.send(topic, key, payload)
                .whenComplete((SendResult<String, String> result, Throwable ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish diagnostic.report.ingested to Kafka "
                                + "topic={} userId={}: {}", topic, key, ex.getMessage(), ex);
                    } else {
                        log.debug("Published diagnostic.report.ingested topic={} partition={} offset={}",
                                topic,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
