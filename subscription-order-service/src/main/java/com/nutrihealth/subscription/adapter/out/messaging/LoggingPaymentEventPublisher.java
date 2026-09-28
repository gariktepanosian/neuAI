package com.nutrihealth.subscription.adapter.out.messaging;

import com.nutrihealth.subscription.domain.model.Subscription;
import com.nutrihealth.subscription.domain.port.out.PaymentEventPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Placeholder outbound adapter. Logs instead of publishing to Kafka because
 * the Kafka cluster (spec Phase 1, Step 1.3) has not been provisioned yet.
 * Swap for a KafkaTemplate-backed adapter, keeping the same port, once the
 * cluster and topics (`SUBSCRIPTION_PAID`, `PAYMENT_FAILED`) exist.
 */
@Component
public class LoggingPaymentEventPublisher implements PaymentEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingPaymentEventPublisher.class);

    @Override
    public void publishSubscriptionPaid(Subscription subscription) {
        log.info("SUBSCRIPTION_PAID userId={} stripeSubscriptionId={}",
                subscription.getUserId(), subscription.getStripeSubscriptionId());
    }

    @Override
    public void publishPaymentFailed(Subscription subscription) {
        log.info("PAYMENT_FAILED userId={} stripeSubscriptionId={}",
                subscription.getUserId(), subscription.getStripeSubscriptionId());
    }
}
