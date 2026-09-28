package com.nutrihealth.subscription.domain.port.in;

import com.nutrihealth.subscription.domain.model.ScheduleType;
import com.nutrihealth.subscription.domain.model.Subscription;
import java.util.UUID;

public interface ManageSubscriptionUseCase {

    Subscription createSubscription(UUID userId, String stripeSubscriptionId, ScheduleType planType);

    /** Applied from `customer.subscription.updated` Stripe webhook events. */
    Subscription applyStripeStatusUpdate(String stripeSubscriptionId, String stripeStatus);

    /** Applied from `invoice.payment_succeeded` / `invoice.payment_failed` Stripe webhook events. */
    Subscription recordPaymentResult(String stripeSubscriptionId, boolean paymentSucceeded);
}
