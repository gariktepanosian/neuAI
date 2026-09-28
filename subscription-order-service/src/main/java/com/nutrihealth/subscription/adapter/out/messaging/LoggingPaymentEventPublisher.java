package com.nutrihealth.subscription.adapter.out.messaging;

import com.nutrihealth.subscription.domain.model.Subscription;
import com.nutrihealth.subscription.domain.port.out.PaymentEventPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fallback adapter used only in tests or when Kafka is unavailable.
 * In production, {@link KafkaPaymentEventPublisher} is the active bean.
 * This class is NOT annotated with {@code @Component} — it is wired up
 * only in test configurations that want a no-op publisher.
 */
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
