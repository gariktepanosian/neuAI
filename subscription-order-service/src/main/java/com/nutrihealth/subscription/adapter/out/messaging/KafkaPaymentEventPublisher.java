package com.nutrihealth.subscription.adapter.out.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutrihealth.subscription.domain.model.Subscription;
import com.nutrihealth.subscription.domain.port.out.PaymentEventPublisherPort;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

/**
 * Outbound adapter that publishes payment domain events to Kafka.
 *
 * <p>Topics (defined in {@code infra/k8s/kafka/20-kafka-topics.yaml}):
 * <ul>
 *   <li>{@code subscription.payment.paid} — Stripe {@code invoice.payment_succeeded}</li>
 *   <li>{@code subscription.payment.failed} — Stripe {@code invoice.payment_failed}</li>
 * </ul>
 *
 * <p>Message key = {@code userId} (UUID string) so all events for the same user
 * land on the same partition and can be consumed in order by downstream services.
 *
 * <p>Failure handling: any exception from {@code KafkaTemplate.send()} is logged
 * at ERROR level.  The calling use-case does NOT roll back the database transaction
 * on a Kafka failure — Stripe is the source of truth; Kafka is fire-and-forget
 * best-effort for near-real-time reaction.  A periodic reconciliation job (future
 * work) should re-publish any events missing from Kafka by comparing Stripe
 * invoice history with the local {@code subscriptions} table.
 */
@Component
public class KafkaPaymentEventPublisher implements PaymentEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(KafkaPaymentEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String paidTopic;
    private final String failedTopic;

    public KafkaPaymentEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${nutrihealth.kafka.topic.subscription-paid:subscription.payment.paid}") String paidTopic,
            @Value("${nutrihealth.kafka.topic.subscription-failed:subscription.payment.failed}") String failedTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.paidTopic = paidTopic;
        this.failedTopic = failedTopic;
    }

    @Override
    public void publishSubscriptionPaid(Subscription subscription) {
        publish(paidTopic, subscription, "subscription.payment.paid");
    }

    @Override
    public void publishPaymentFailed(Subscription subscription) {
        publish(failedTopic, subscription, "subscription.payment.failed");
    }

    private void publish(String topic, Subscription subscription, String eventType) {
        String key = subscription.getUserId().toString();
        String payload;
        try {
            payload = objectMapper.writeValueAsString(Map.of(
                    "eventType", eventType,
                    "userId", subscription.getUserId().toString(),
                    "subscriptionId", subscription.getId().toString(),
                    "stripeSubscriptionId", subscription.getStripeSubscriptionId(),
                    "status", subscription.getStatus().name(),
                    "occurredAt", Instant.now().toString()
            ));
        } catch (Exception e) {
            log.error("Failed to serialise Kafka payload for topic={} userId={}: {}",
                    topic, key, e.getMessage(), e);
            return;
        }

        kafkaTemplate.send(topic, key, payload)
                .whenComplete((SendResult<String, String> result, Throwable ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish {} event to Kafka topic={} userId={}: {}",
                                eventType, topic, key, ex.getMessage(), ex);
                    } else {
                        log.debug("Published {} to topic={} partition={} offset={}",
                                eventType, topic,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
