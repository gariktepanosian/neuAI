package com.nutrihealth.subscription.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.nutrihealth.subscription.domain.model.ScheduleType;
import com.nutrihealth.subscription.domain.model.Subscription;
import com.nutrihealth.subscription.domain.port.in.ManageSubscriptionUseCase;
import com.stripe.net.Webhook;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class StripeWebhookControllerTest {

    private static final String WEBHOOK_SECRET = "whsec_test_secret";

    private String lastAppliedStatus;
    private String lastPaymentSubscriptionId;
    private Boolean lastPaymentSucceeded;

    private final ManageSubscriptionUseCase useCase = new ManageSubscriptionUseCase() {
        @Override
        public Subscription createSubscription(UUID userId, String stripeSubscriptionId, ScheduleType planType) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Subscription applyStripeStatusUpdate(String stripeSubscriptionId, String stripeStatus) {
            lastAppliedStatus = stripeStatus;
            return new Subscription(UUID.randomUUID(), UUID.randomUUID(), stripeSubscriptionId,
                    ScheduleType.CUSTOM, com.nutrihealth.subscription.domain.model.SubscriptionStatus.ACTIVE,
                    LocalDate.now(), null, true);
        }

        @Override
        public Subscription recordPaymentResult(String stripeSubscriptionId, boolean paymentSucceeded) {
            lastPaymentSubscriptionId = stripeSubscriptionId;
            lastPaymentSucceeded = paymentSucceeded;
            return new Subscription(UUID.randomUUID(), UUID.randomUUID(), stripeSubscriptionId,
                    ScheduleType.CUSTOM, com.nutrihealth.subscription.domain.model.SubscriptionStatus.ACTIVE,
                    LocalDate.now(), null, true);
        }
    };

    private final StripeWebhookController controller = new StripeWebhookController(useCase, WEBHOOK_SECRET);

    @Test
    void rejectsPayloadWithInvalidSignature() {
        String payload = "{\"id\":\"evt_1\",\"object\":\"event\",\"type\":\"invoice.payment_succeeded\",\"data\":{\"object\":{}}}";

        ResponseEntity<Void> response = controller.handleStripeWebhook(payload, "t=1,v1=not-a-real-signature");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void acceptsValidlySignedPaymentSucceededAndInvokesUseCase() {
        String payload = "{\"id\":\"evt_1\",\"object\":\"event\",\"api_version\":\"2025-05-28.basil\","
                + "\"type\":\"invoice.payment_succeeded\","
                + "\"data\":{\"object\":{\"id\":\"in_1\",\"object\":\"invoice\",\"parent\":"
                + "{\"type\":\"subscription_details\",\"subscription_details\":{\"subscription\":\"sub_123\"}}}}}";
        String signatureHeader = signPayload(payload);

        ResponseEntity<Void> response = controller.handleStripeWebhook(payload, signatureHeader);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(lastPaymentSubscriptionId).isEqualTo("sub_123");
        assertThat(lastPaymentSucceeded).isTrue();
    }

    @Test
    void acceptsValidlySignedSubscriptionUpdatedAndInvokesUseCase() {
        String payload = "{\"id\":\"evt_2\",\"object\":\"event\",\"api_version\":\"2025-05-28.basil\","
                + "\"type\":\"customer.subscription.updated\","
                + "\"data\":{\"object\":{\"id\":\"sub_123\",\"object\":\"subscription\",\"status\":\"past_due\"}}}";
        String signatureHeader = signPayload(payload);

        ResponseEntity<Void> response = controller.handleStripeWebhook(payload, signatureHeader);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(lastAppliedStatus).isEqualTo("past_due");
    }

    /** Builds a `t=<ts>,v1=<hmac>` header the same way Stripe signs real webhook deliveries. */
    private static String signPayload(String payload) {
        try {
            long timestamp = Instant.now().getEpochSecond();
            String signedPayload = timestamp + "." + payload;
            String signature = Webhook.Util.computeHmacSha256(WEBHOOK_SECRET, signedPayload);
            return "t=" + timestamp + ",v1=" + signature;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
