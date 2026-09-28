# user-auth-service

OAuth2.0/JWT authentication and RBAC microservice for NutriHealth AI.

## Structure (Hexagonal / Ports & Adapters)

```
domain/model          - UserAccount, Role (RBAC), domain exceptions
domain/port/in         - AuthenticationUseCase (inbound port)
domain/port/out        - UserAccountRepositoryPort, PasswordHasherPort, TokenIssuerPort
application/service    - AuthenticationService (register/login use case)
adapter/in/web          - AuthController (/api/v1/auth/register, /login), GlobalExceptionHandler
adapter/in/web/security - JwtAuthenticationFilter (validates Bearer tokens, populates RBAC context)
adapter/out/persistence - PostgreSQL/JPA adapter for the `users` table
adapter/out/security    - BCryptPasswordHasherAdapter, JwtTokenIssuerAdapter (HS256)
config                  - SecurityConfig (stateless JWT-based security filter chain)
```

## API

```
POST /api/v1/auth/register  { "email": "...", "password": "..." }  -> 201, { userId, email }
POST /api/v1/auth/login     { "email": "...", "password": "..." }  -> 200, { accessToken, expiresAtEpochSeconds }
```

All other endpoints require `Authorization: Bearer <token>`; the token's `role`
claim maps to a Spring Security `ROLE_*` authority for `@PreAuthorize` checks
in downstream code.

## Running locally

Requires PostgreSQL (defaults to `jdbc:postgresql://localhost:5432/subscription_db`,
override via `DATABASE_URL`/`DATABASE_USERNAME`/`DATABASE_PASSWORD`).

```bash
./mvnw spring-boot:run
```

## Notes / deviations from the spec

- Built against **Java 17**, matching [clinic-diagnostic-service](../clinic-diagnostic-service/README.md)'s
  note — bump once Java 21 is available.
- `nutrihealth.auth.jwt-signing-key` in `application.yml` ships a locally
  generated dev-only HS256 key. Production must source this from GCP Secret
  Manager with periodic rotation — this adapter only uses "the current key",
  it does not implement rotation itself.
- Shares the `users` table name with the `subscription_db` Postgres schema in
  the roadmap's spec, since both describe the same conceptual `users` entity.
  In a true "Database Per Service" deployment this service should own its own
  schema/database rather than colocating with subscription data.
