package com.nutrihealth.subscription.adapter.out.persistence;

import com.nutrihealth.subscription.domain.model.Subscription;
import com.nutrihealth.subscription.domain.port.out.SubscriptionRepositoryPort;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionPersistenceAdapter implements SubscriptionRepositoryPort {

    private final SpringDataSubscriptionRepository springDataRepository;

    public SubscriptionPersistenceAdapter(SpringDataSubscriptionRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Subscription save(Subscription subscription) {
        SubscriptionEntity entity = new SubscriptionEntity(
                subscription.getId(), subscription.getUserId(), subscription.getStripeSubscriptionId(),
                subscription.getPlanType(), subscription.getStatus(), subscription.getStartDate(),
                subscription.getEndDate(), subscription.isAutoRenew());
        springDataRepository.save(entity);
        return subscription;
    }

    @Override
    public Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId) {
        return springDataRepository.findByStripeSubscriptionId(stripeSubscriptionId).map(this::toDomain);
    }

    private Subscription toDomain(SubscriptionEntity entity) {
        return new Subscription(entity.getId(), entity.getUserId(), entity.getStripeSubscriptionId(),
                entity.getPlanType(), entity.getStatus(), entity.getStartDate(), entity.getEndDate(),
                entity.isAutoRenew());
    }
}
