package com.nutrihealth.subscription.domain.port.out;

import com.nutrihealth.subscription.domain.model.Subscription;
import java.util.Optional;

public interface SubscriptionRepositoryPort {

    Subscription save(Subscription subscription);

    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);
}
