package com.nutrihealth.subscription.domain.model;

public class SubscriptionNotFoundException extends RuntimeException {

    public SubscriptionNotFoundException(String stripeSubscriptionId) {
        super("No subscription found for Stripe subscription id " + stripeSubscriptionId);
    }
}
