package com.nutrihealth.subscription.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SubscriptionTest {

    private Subscription newActiveSubscription() {
        return new Subscription(UUID.randomUUID(), UUID.randomUUID(), "sub_123",
                ScheduleType.FULL_7_DAY, SubscriptionStatus.ACTIVE, LocalDate.now(), null, true);
    }

    @Test
    void statusTransitionUpdatesStatus() {
        Subscription subscription = newActiveSubscription();

        subscription.applyStatusTransition(SubscriptionStatus.PAST_DUE);

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);
    }

    @Test
    void cancellingSetsEndDate() {
        Subscription subscription = newActiveSubscription();

        subscription.applyStatusTransition(SubscriptionStatus.CANCELLED);

        assertThat(subscription.getEndDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void cancelledSubscriptionIsTerminal() {
        Subscription subscription = newActiveSubscription();
        subscription.applyStatusTransition(SubscriptionStatus.CANCELLED);

        assertThatThrownBy(() -> subscription.applyStatusTransition(SubscriptionStatus.ACTIVE))
                .isInstanceOf(IllegalStateException.class);
    }
}
