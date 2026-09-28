package com.nutrihealth.subscription.adapter.in.web;

import com.nutrihealth.subscription.domain.port.in.ManageSubscriptionUseCase;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.net.Webhook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import java.util.Optional;

/**
 * Ingests Stripe Billing webhooks. Per the spec, subscription status
 * transitions and payment outcomes are processed idempotently only after
 * the `Stripe-Signature` header has been cryptographically verified against
 * the raw request body — never trust the parsed JSON body alone.
 */
@RestController
public class StripeWebhookController {

    private final ManageSubscriptionUseCase useCase;
    private final String webhookSigningSecret;

    public StripeWebhookController(ManageSubscriptionUseCase useCase,
                                    @Value("${nutrihealth.stripe.webhook-signing-secret}") String webhookSigningSecret) {
        this.useCase = useCase;
        this.webhookSigningSecret = webhookSigningSecret;
    }

    @PostMapping("/api/v1/webhooks/stripe")
    public ResponseEntity<Void> handleStripeWebhook(@RequestBody String rawPayload,
                                                      @RequestHeader("Stripe-Signature") String signatureHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(rawPayload, signatureHeader, webhookSigningSecret);
        } catch (SignatureVerificationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        StripeObject dataObject = event.getDataObjectDeserializer().getObject().orElse(null);

        switch (event.getType()) {
            case "customer.subscription.updated" -> {
                if (dataObject instanceof Subscription stripeSubscription) {
                    useCase.applyStripeStatusUpdate(stripeSubscription.getId(), stripeSubscription.getStatus());
                }
            }
            case "invoice.payment_succeeded" -> {
                if (dataObject instanceof Invoice invoice) {
                    extractSubscriptionId(invoice).ifPresent(id -> useCase.recordPaymentResult(id, true));
                }
            }
            case "invoice.payment_failed" -> {
                if (dataObject instanceof Invoice invoice) {
                    extractSubscriptionId(invoice).ifPresent(id -> useCase.recordPaymentResult(id, false));
                }
            }
            default -> {
                // Ignore event types this service does not act on.
            }
        }

        return ResponseEntity.ok().build();
    }

    /**
     * Stripe's 2024+ invoice API nests the related subscription id under
     * {@code parent.subscription_details.subscription} instead of the
     * previous top-level {@code invoice.subscription} field.
     */
    private Optional<String> extractSubscriptionId(Invoice invoice) {
        return Optional.ofNullable(invoice.getParent())
                .map(Invoice.Parent::getSubscriptionDetails)
                .map(Invoice.Parent.SubscriptionDetails::getSubscription);
    }
}
