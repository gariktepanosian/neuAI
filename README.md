# neuAI

NutriHealth AI — microservices platform combining AI personalized nutrition,
subscription meal delivery logistics, and HIPAA/GDPR-compliant telehealth
diagnostic integrations. Scaffolded from the project's technical architecture
& execution roadmap (Hexagonal Architecture, Spring Boot 3.x / Java, MongoDB +
PostgreSQL, Kafka, GCP/GKE).

## Services

| Service | Status | Description |
| --- | --- | --- |
| [clinic-diagnostic-service](clinic-diagnostic-service/README.md) | scaffolded | FHIR R4 + legacy HL7 v2 (ORU^R01) lab diagnostic ingestion, AES-256-GCM PHI field encryption |
| [user-auth-service](user-auth-service/README.md) | scaffolded | OAuth2.0/JWT auth, RBAC, GCP Secret Manager key rotation |
| [subscription-order-service](subscription-order-service/README.md) | scaffolded | Stripe billing/webhooks, subscription lifecycle, Kafka event publishing |
| ai-nutrition-engine-service | planned | AI-driven recipe generation and macro/menu personalization from FHIR biomarkers |
| logistics-dispatch-service | planned | Redis geospatial courier dispatch and delivery window optimization |

Each service is an independently deployable Spring Boot module following
Hexagonal Architecture (domain / port / adapter) and owns its own datastore,
per the platform's "Database Per Service" principle.
