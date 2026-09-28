package com.nutrihealth.subscription.domain.port.out;

import com.nutrihealth.subscription.domain.model.Subscription;

/**
 * Outbound port for the Kafka event propagation described in the spec:
 * payment webhook verification produces SUBSCRIPTION_PAID or PAYMENT_FAILED
 * events so delivery scheduling can react immediately.
 */
public interface PaymentEventPublisherPort {

    void publishSubscriptionPaid(Subscription subscription);

    void publishPaymentFailed(Subscription subscription);
}
