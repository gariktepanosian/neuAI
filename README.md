# neuAI (NutriHealth AI)

## What this is

NutriHealth AI is a health-tech platform concept that combines three things
that are usually separate products:

1. **AI-personalized nutrition** — recipes and daily menus generated/adjusted
   for an individual, not a generic meal plan.
2. **Subscription meal delivery** — recurring orders (5-day workweek or
   7-day plans) with real-time courier tracking, billed automatically.
3. **Telehealth diagnostics integration** — a user's actual lab results
   (bloodwork, vitamin/mineral panels) feed directly into what food gets
   recommended to them, using the same clinical data standards (FHIR/HL7)
   hospitals and labs use.

The idea: instead of guessing at a diet, or following a generic subscription
box, a user's recommendations are grounded in their real biomarkers — e.g. a
lab result showing low vitamin D automatically steers their menu toward
vitamin-D-rich meals — and that food then gets delivered to them on a
schedule, billed and tracked automatically.

This repository contains the **backend implementation** of that platform,
built as independently deployable microservices, based on a technical
architecture & execution roadmap document (not included in this repo). See
each service's own README for what was actually built vs. planned there.

## What's implemented vs. not

**Implemented: 5 backend microservices** (Java 17, Spring Boot, Hexagonal
Architecture), each with passing tests:

| Service | What it does |
| --- | --- |
| [clinic-diagnostic-service](clinic-diagnostic-service/README.md) | Ingests lab results as FHIR resources (or legacy HL7 v2 messages, auto-converted), encrypts the health data field-by-field |
| [user-auth-service](user-auth-service/README.md) | Signup/login, issues JWTs, role-based access control |
| [subscription-order-service](subscription-order-service/README.md) | Manages meal-plan subscriptions, processes Stripe billing/webhooks |
| [ai-nutrition-engine-service](ai-nutrition-engine-service/README.md) | Picks or generates (via Gemini) a recipe matching a user's dietary needs, scales its macros to their calorie target |
| [logistics-dispatch-service](logistics-dispatch-service/README.md) | Finds the nearest available courier for a delivery using Redis geospatial search, publishes live location updates |

**Not implemented:**

- **No mobile app.** The roadmap calls for a Flutter app (iOS + Android) —
  none of that exists yet. There is no Flutter project, no screens, no code
  for it in this repo. Today the platform is API-only; you'd interact with it
  via HTTP requests (curl, Postman, etc.), not a phone app.
- **No infrastructure.** No Kubernetes manifests, no Terraform, no deployed
  Kafka/GKE/Cloud SQL — the services run locally against local
  Postgres/MongoDB/Redis instances you provide yourself.
- **No cross-service wiring.** The services don't call each other yet (e.g.
  the nutrition engine doesn't automatically fetch a user's latest lab
  results from the diagnostic service) — each is independently runnable and
  testable, but the end-to-end flow described above is not connected end to end.

## Why this design (the benefit of building it this way)

- **Each service is independently deployable and replaceable.** Hexagonal
  Architecture (ports/adapters) means the core business logic in each
  service doesn't know or care whether it's talking to Postgres or MongoDB,
  Stripe or another payment processor, Gemini or another model — those are
  swappable adapters. You can change infrastructure without rewriting
  business rules.
- **Database-per-service** means no service can accidentally corrupt another
  service's data or get blocked by someone else's schema migration — each
  team/service scales and evolves independently.
- **Using real clinical data standards (FHIR/HL7)** instead of a custom
  format means this can eventually integrate with actual hospitals, labs,
  and health systems without a rewrite — that interoperability is the whole
  point of FHIR existing.
- **PHI (health data) is encrypted field-by-field** and kept structurally
  separate from billing/account data, which is a real compliance requirement
  (HIPAA/GDPR) if this ever handles real patient data, not just a nice-to-have.

## How to use it

Each service is a standalone Maven project. To run one:

```bash
cd <service-name>
./mvnw spring-boot:run
```

You'll need the datastore that service depends on running locally first
(see each service's own README for exact connection details/env vars):

| Service | Needs running locally |
| --- | --- |
| clinic-diagnostic-service | MongoDB |
| user-auth-service | PostgreSQL |
| subscription-order-service | PostgreSQL (+ a Stripe test account for real webhook delivery) |
| ai-nutrition-engine-service | MongoDB (+ a Gemini API key for real AI generation) |
| logistics-dispatch-service | Redis |

Once a service is running, you interact with it over plain HTTP — e.g. for
`user-auth-service` on port 8081:

```bash
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"you@example.com","password":"correct-horse-battery-staple"}'
```

Each service's README documents its full API and exact endpoints. To verify
a service works without any external dependencies, run its test suite:

```bash
cd <service-name>
./mvnw test
```

## Next steps (if continuing this project)

1. Decide the two open questions from the original spec: launch region/data
   residency, and whether meals come from in-house "ghost kitchens" or
   outsourced partner restaurants — both affect infrastructure and data
   model decisions downstream.
2. Wire the services together (e.g. nutrition engine calling
   clinic-diagnostic-service for a user's latest biomarkers).
3. Stand up real infrastructure (Postgres/Mongo/Redis/Kafka, containerized)
   so the whole system can run together, not just service-by-service.
4. Build the Flutter mobile client — currently 0% started.
