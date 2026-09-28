package com.nutrihealth.subscription.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StripeConfig {

    private final String apiKey;

    public StripeConfig(@Value("${nutrihealth.stripe.api-key}") String apiKey) {
        this.apiKey = apiKey;
    }

    @PostConstruct
    void configureStripeClient() {
        Stripe.apiKey = apiKey;
    }
}
