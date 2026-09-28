# infra/terraform

Phase 1 (Steps 1.1 and 1.2) infrastructure-as-code: GCP project APIs, IAM
service accounts, a multi-zone GKE cluster, an HA Cloud SQL Postgres
instance, a Memorystore Redis instance, and Secret Manager secrets for every
credential the services currently read as `${VAR:dev-placeholder}`.

**This has not been applied.** It was written and `terraform validate`d in
this environment, but never run against a real GCP project — doing so
creates real, billable resources (GKE, Cloud SQL HA, and Memorystore Redis
are not free) and that decision belongs to whoever owns the GCP billing
account, not to an agent. Resolve the two open questions from the roadmap
first (launch region/data residency, ghost-kitchen vs. partner-restaurant
model) since both affect `region`/`zones` and possibly the schema.

## Layout

| File | Provisions |
| --- | --- |
| `apis.tf` | Enables the GCP APIs every service depends on (GKE, Cloud SQL, Redis, Secret Manager, Vertex AI, Artifact Registry, ...) |
| `iam.tf` | One least-privilege service account per microservice + Workload Identity bindings |
| `gke.tf` | Regional (multi-zone) GKE cluster with an autoscaling node pool |
| `cloudsql.tf` | HA (`REGIONAL`) PostgreSQL 16 instance + `subscription_db` database |
| `redis.tf` | `STANDARD_HA` Memorystore Redis instance |
| `secrets.tf` | Secret Manager secrets for the JWT signing key, PHI encryption key, Stripe keys, and Gemini API key, with per-service IAM access bindings |

## How to actually apply this

```bash
cd infra/terraform
gcloud auth application-default login
terraform init
terraform plan -var="project_id=<your-real-gcp-project-id>"
terraform apply -var="project_id=<your-real-gcp-project-id>"
```

After applying, replace the placeholder third-party secret values Terraform
couldn't generate for you:

```bash
echo -n "sk_live_..." | gcloud secrets versions add stripe-api-key-dev --data-file=-
echo -n "whsec_..."   | gcloud secrets versions add stripe-webhook-signing-secret-dev --data-file=-
echo -n "<gemini-key>" | gcloud secrets versions add gemini-api-key-dev --data-file=-
```

The JWT signing key and PHI encryption key are generated automatically by
Terraform (`random_id` resources) — no manual step needed for those.

## Known gaps

- No VPC/networking module yet — `cloudsql.tf`'s `ip_configuration` disables
  the public IP but doesn't wire a private VPC peering connection, so Cloud
  SQL is not actually reachable from GKE until that's added.
- No Cloud KMS CMEK key is provisioned yet, even though `cloudkms.googleapis.com`
  is enabled — the spec's "Encryption-at-Rest using GCP CMEK" isn't wired to
  any resource here yet.
- Single environment/region assumed; multi-region data residency (the
  roadmap's open question) would need a second `region`/workspace, not just
  a variable change.
