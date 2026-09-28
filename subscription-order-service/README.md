# subscription-order-service

Stripe billing, subscription lifecycle, and payment event propagation
microservice for NutriHealth AI.

## Structure (Hexagonal / Ports & Adapters)

```
domain/model         - Subscription (status-transition business rule), ScheduleType, SubscriptionStatus
domain/port/in        - ManageSubscriptionUseCase (inbound port)
domain/port/out       - SubscriptionRepositoryPort, PaymentEventPublisherPort
application/service   - SubscriptionService (creation, Stripe status mapping, payment result handling)
adapter/in/web         - SubscriptionController (POST /api/v1/subscriptions),
                          StripeWebhookController (POST /api/v1/webhooks/stripe)
adapter/out/persistence - PostgreSQL/JPA adapter for the `subscriptions` table
adapter/out/messaging   - Logging stub for the future Kafka event publisher
config                  - StripeConfig (wires the Stripe SDK's global API key)
```

## Stripe webhook handling

`POST /api/v1/webhooks/stripe` verifies the `Stripe-Signature` header against
the raw request body via `com.stripe.net.Webhook.constructEvent` — an invalid
or missing signature is rejected with `400` before any domain logic runs.
Handled event types:

- `customer.subscription.updated` → maps Stripe's subscription status onto
  the domain's `SubscriptionStatus` and persists the transition.
- `invoice.payment_succeeded` / `invoice.payment_failed` → reactivates or
  marks the subscription `PAST_DUE`, then publishes `SUBSCRIPTION_PAID` /
  `PAYMENT_FAILED` via `PaymentEventPublisherPort`.

Both invoice events read the related subscription id from
`invoice.parent.subscription_details.subscription` — Stripe's current
(2025+) invoice API shape, not the older top-level `invoice.subscription`
field.

## Running locally

Requires PostgreSQL (defaults to `jdbc:postgresql://localhost:5432/subscription_db`,
override via `DATABASE_URL`/`DATABASE_USERNAME`/`DATABASE_PASSWORD`), and a
Stripe account for real webhook delivery (`STRIPE_API_KEY`,
`STRIPE_WEBHOOK_SIGNING_SECRET`).

```bash
./mvnw spring-boot:run
```

## Notes / deviations from the spec

- Built against **Java 17**, matching the other services in this repo.
- `LoggingPaymentEventPublisher` stands in for the Kafka publisher described
  in the roadmap; no Kafka cluster has been provisioned yet (Phase 1, Step
  1.3). Swap the adapter behind `PaymentEventPublisherPort` once it exists.
- `nutrihealth.stripe.*` in `application.yml` ships dev/test placeholders.
  Production must source both values from GCP Secret Manager.
- Delivery scheduling (`delivery_schedules` table in the spec's Postgres
  schema) is out of scope here — this service owns subscription/billing
  state only; delivery orchestration belongs in `logistics-dispatch-service`.
