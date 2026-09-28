# clinic-diagnostic-service

FHIR/HL7v2 diagnostic ingestion microservice for NutriHealth AI, scaffolded from
the platform's execution roadmap (Hexagonal Architecture, Spring Boot, MongoDB).

## Structure (Hexagonal / Ports & Adapters)

```
domain/model        - HealthProfile-adjacent core entities (DiagnosticReport, BiomarkerObservation)
domain/port/in       - ManageDiagnosticReportUseCase (inbound port)
domain/port/out      - DiagnosticReportRepositoryPort, DietaryAdjustmentEventPublisherPort
application/service  - DiagnosticReportService (use case implementation, FHIR parsing via HAPI)
adapter/in/web        - FhirDiagnosticController: POST/GET /fhir/R4/DiagnosticReport
adapter/in/hl7        - Hl7v2OruTransformer + Hl7v2IngestController: legacy ORU^R01 -> FHIR
adapter/out/persistence - Mongo adapter; PHI observation payload stored as opaque AES-256-GCM ciphertext
adapter/out/encryption  - PhiFieldEncryptor (AES-256-GCM field-level encryption)
adapter/out/messaging   - Logging stub for the future Kafka event publisher
```

## Running locally

Requires a MongoDB instance (defaults to `mongodb://localhost:27017/nutrition_db`,
override via `MONGODB_URI`).

```bash
./mvnw spring-boot:run
```

## Notes / deviations from the spec

- Built against **Java 17**, not Java 21 as listed in the roadmap's stack table —
  only JDK 17 was available in this environment. Bump `java.version` in `pom.xml`
  once Java 21 is available.
- `LoggingDietaryAdjustmentEventPublisher` is a stand-in for the Kafka publisher
  described in the roadmap; no Kafka cluster has been provisioned yet (Phase 1,
  Step 1.3). Swap the adapter behind `DietaryAdjustmentEventPublisherPort` once
  it exists.
- `nutrihealth.phi.encryption-key` in `application.yml` ships a locally generated
  dev-only AES-256 key. Production must source this from GCP Secret Manager/KMS.
