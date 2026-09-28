package com.nutrihealth.subscription.adapter.in.web;

import com.nutrihealth.subscription.domain.model.ScheduleType;
import com.nutrihealth.subscription.domain.model.Subscription;
import com.nutrihealth.subscription.domain.model.SubscriptionStatus;
import java.time.LocalDate;
import java.util.UUID;

public record SubscriptionResponse(UUID id, UUID userId, String stripeSubscriptionId, ScheduleType planType,
                                    SubscriptionStatus status, LocalDate startDate, LocalDate endDate) {

    public static SubscriptionResponse from(Subscription subscription) {
        return new SubscriptionResponse(
                subscription.getId(), subscription.getUserId(), subscription.getStripeSubscriptionId(),
                subscription.getPlanType(), subscription.getStatus(), subscription.getStartDate(),
                subscription.getEndDate());
    }
}
