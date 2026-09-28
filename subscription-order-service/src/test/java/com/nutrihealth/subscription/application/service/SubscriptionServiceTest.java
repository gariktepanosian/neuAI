package com.nutrihealth.subscription.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nutrihealth.subscription.domain.model.ScheduleType;
import com.nutrihealth.subscription.domain.model.Subscription;
import com.nutrihealth.subscription.domain.model.SubscriptionNotFoundException;
import com.nutrihealth.subscription.domain.model.SubscriptionStatus;
import com.nutrihealth.subscription.domain.port.out.PaymentEventPublisherPort;
import com.nutrihealth.subscription.domain.port.out.SubscriptionRepositoryPort;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubscriptionServiceTest {

    private final Map<String, Subscription> store = new HashMap<>();
    private final SubscriptionRepositoryPort repository = new SubscriptionRepositoryPort() {
        @Override
        public Subscription save(Subscription subscription) {
            store.put(subscription.getStripeSubscriptionId(), subscription);
            return subscription;
        }

        @Override
        public Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId) {
            return Optional.ofNullable(store.get(stripeSubscriptionId));
        }
    };

    private Subscription lastPaidEvent;
    private Subscription lastFailedEvent;
    private final PaymentEventPublisherPort eventPublisher = new PaymentEventPublisherPort() {
        @Override
        public void publishSubscriptionPaid(Subscription subscription) {
            lastPaidEvent = subscription;
        }

        @Override
        public void publishPaymentFailed(Subscription subscription) {
            lastFailedEvent = subscription;
        }
    };

    private SubscriptionService service;

    @BeforeEach
    void setUp() {
        service = new SubscriptionService(repository, eventPublisher);
    }

    @Test
    void createSubscriptionStartsActive() {
        Subscription subscription = service.createSubscription(UUID.randomUUID(), "sub_123", ScheduleType.CUSTOM);

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void applyStripeStatusUpdateMapsCanceledToDomainCancelled() {
        service.createSubscription(UUID.randomUUID(), "sub_123", ScheduleType.CUSTOM);

        Subscription updated = service.applyStripeStatusUpdate("sub_123", "canceled");

        assertThat(updated.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
    }

    @Test
    void applyStripeStatusUpdateThrowsForUnknownSubscription() {
        assertThatThrownBy(() -> service.applyStripeStatusUpdate("sub_missing", "active"))
                .isInstanceOf(SubscriptionNotFoundException.class);
    }

    @Test
    void recordPaymentResultSuccessPublishesSubscriptionPaidAndReactivates() {
        service.createSubscription(UUID.randomUUID(), "sub_123", ScheduleType.CUSTOM);
        service.applyStripeStatusUpdate("sub_123", "past_due");

        Subscription result = service.recordPaymentResult("sub_123", true);

        assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(lastPaidEvent).isNotNull();
        assertThat(lastFailedEvent).isNull();
    }

    @Test
    void recordPaymentResultFailurePublishesPaymentFailedAndMarksPastDue() {
        service.createSubscription(UUID.randomUUID(), "sub_123", ScheduleType.CUSTOM);

        Subscription result = service.recordPaymentResult("sub_123", false);

        assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);
        assertThat(lastFailedEvent).isNotNull();
        assertThat(lastPaidEvent).isNull();
    }
}
