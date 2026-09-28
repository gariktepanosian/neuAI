package com.nutrihealth.subscription.adapter.in.web;

import com.nutrihealth.subscription.domain.model.ScheduleType;
import com.nutrihealth.subscription.domain.model.Subscription;
import com.nutrihealth.subscription.domain.port.in.ManageSubscriptionUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

/** Created once a Stripe Checkout Session for a new plan has completed client-side. */
@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {

    private final ManageSubscriptionUseCase useCase;

    public SubscriptionController(ManageSubscriptionUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionResponse create(@Valid @RequestBody CreateSubscriptionRequest request) {
        Subscription subscription = useCase.createSubscription(
                request.userId(), request.stripeSubscriptionId(), request.planType());
        return SubscriptionResponse.from(subscription);
    }

    public record CreateSubscriptionRequest(
            @NotNull UUID userId,
            @NotBlank String stripeSubscriptionId,
            @NotNull ScheduleType planType) {
    }
}
