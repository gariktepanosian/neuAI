package com.nutrihealth.subscription.domain.model;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Core domain aggregate for a meal-plan subscription. Billing state
 * (ACTIVE/PAUSED/CANCELLED/PAST_DUE) is driven by Stripe webhook events but
 * the transition rule itself — CANCELLED is terminal — lives here, not in
 * the webhook adapter, per Hexagonal Architecture.
 */
public class Subscription {

    private final UUID id;
    private final UUID userId;
    private final String stripeSubscriptionId;
    private final ScheduleType planType;
    private SubscriptionStatus status;
    private final LocalDate startDate;
    private LocalDate endDate;
    private final boolean autoRenew;

    public Subscription(UUID id, UUID userId, String stripeSubscriptionId, ScheduleType planType,
                         SubscriptionStatus status, LocalDate startDate, LocalDate endDate, boolean autoRenew) {
        this.id = id;
        this.userId = userId;
        this.stripeSubscriptionId = stripeSubscriptionId;
        this.planType = planType;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
        this.autoRenew = autoRenew;
    }

    public void applyStatusTransition(SubscriptionStatus newStatus) {
        if (this.status == SubscriptionStatus.CANCELLED) {
            throw new IllegalStateException("Cannot transition a cancelled subscription to " + newStatus);
        }
        this.status = newStatus;
        if (newStatus == SubscriptionStatus.CANCELLED) {
            this.endDate = LocalDate.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getStripeSubscriptionId() {
        return stripeSubscriptionId;
    }

    public ScheduleType getPlanType() {
        return planType;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public boolean isAutoRenew() {
        return autoRenew;
    }
}
