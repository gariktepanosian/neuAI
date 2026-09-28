package com.nutrihealth.subscription.application.service;

import com.nutrihealth.subscription.domain.model.ScheduleType;
import com.nutrihealth.subscription.domain.model.Subscription;
import com.nutrihealth.subscription.domain.model.SubscriptionNotFoundException;
import com.nutrihealth.subscription.domain.model.SubscriptionStatus;
import com.nutrihealth.subscription.domain.port.in.ManageSubscriptionUseCase;
import com.nutrihealth.subscription.domain.port.out.PaymentEventPublisherPort;
import com.nutrihealth.subscription.domain.port.out.SubscriptionRepositoryPort;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SubscriptionService implements ManageSubscriptionUseCase {

    private final SubscriptionRepositoryPort repository;
    private final PaymentEventPublisherPort eventPublisher;

    public SubscriptionService(SubscriptionRepositoryPort repository, PaymentEventPublisherPort eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Subscription createSubscription(UUID userId, String stripeSubscriptionId, ScheduleType planType) {
        Subscription subscription = new Subscription(UUID.randomUUID(), userId, stripeSubscriptionId, planType,
                SubscriptionStatus.ACTIVE, LocalDate.now(), null, true);
        return repository.save(subscription);
    }

    @Override
    public Subscription applyStripeStatusUpdate(String stripeSubscriptionId, String stripeStatus) {
        Subscription subscription = findOrThrow(stripeSubscriptionId);
        subscription.applyStatusTransition(mapStripeStatus(stripeStatus));
        return repository.save(subscription);
    }

    @Override
    public Subscription recordPaymentResult(String stripeSubscriptionId, boolean paymentSucceeded) {
        Subscription subscription = findOrThrow(stripeSubscriptionId);
        subscription.applyStatusTransition(paymentSucceeded ? SubscriptionStatus.ACTIVE : SubscriptionStatus.PAST_DUE);
        Subscription saved = repository.save(subscription);

        if (paymentSucceeded) {
            eventPublisher.publishSubscriptionPaid(saved);
        } else {
            eventPublisher.publishPaymentFailed(saved);
        }
        return saved;
    }

    private Subscription findOrThrow(String stripeSubscriptionId) {
        return repository.findByStripeSubscriptionId(stripeSubscriptionId)
                .orElseThrow(() -> new SubscriptionNotFoundException(stripeSubscriptionId));
    }

    /** Maps Stripe's subscription.status values to the domain's own status enum. */
    private SubscriptionStatus mapStripeStatus(String stripeStatus) {
        return switch (stripeStatus) {
            case "active", "trialing" -> SubscriptionStatus.ACTIVE;
            case "past_due", "unpaid" -> SubscriptionStatus.PAST_DUE;
            case "paused" -> SubscriptionStatus.PAUSED;
            case "canceled", "incomplete_expired" -> SubscriptionStatus.CANCELLED;
            default -> throw new IllegalArgumentException("Unrecognized Stripe subscription status: " + stripeStatus);
        };
    }
}
