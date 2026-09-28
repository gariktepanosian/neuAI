# Phase 1, Step 1.1: enable the GCP APIs every microservice or the platform
# as a whole depends on.
locals {
  required_apis = [
    "container.googleapis.com",        # GKE
    "sqladmin.googleapis.com",         # Cloud SQL
    "redis.googleapis.com",            # Memorystore
    "secretmanager.googleapis.com",    # Secret Manager (JWT/PHI/Stripe/Gemini keys)
    "cloudkms.googleapis.com",         # CMEK for encryption-at-rest
    "aiplatform.googleapis.com",       # Vertex AI / Gemini
    "artifactregistry.googleapis.com", # Container image storage for CI/CD
    "iam.googleapis.com",
    "iamcredentials.googleapis.com",
    "cloudresourcemanager.googleapis.com",
    "compute.googleapis.com",
    "servicenetworking.googleapis.com", # Private IP for Cloud SQL/Redis <-> GKE
  ]
}

resource "google_project_service" "required" {
  for_each = toset(local.required_apis)

  project            = var.project_id
  service            = each.value
  disable_on_destroy = false
}
