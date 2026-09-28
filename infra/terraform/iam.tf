# Phase 1, Step 1.1: one least-privilege service account per microservice,
# rather than a single shared account, so a compromised/misconfigured
# service can't reach another service's secrets or database.
resource "google_service_account" "microservice" {
  for_each = toset(var.microservices)

  project      = var.project_id
  account_id   = "${each.value}-${var.environment}"
  display_name = "NutriHealth AI - ${each.value} (${var.environment})"

  depends_on = [google_project_service.required]
}

# Cloud SQL client access: only the two services that actually use Postgres.
resource "google_project_iam_member" "cloudsql_client" {
  for_each = toset(["user-auth-service", "subscription-order-service"])

  project = var.project_id
  role    = "roles/cloudsql.client"
  member  = "serviceAccount:${google_service_account.microservice[each.value].email}"
}

# Vertex AI (Gemini) access: only the nutrition engine.
resource "google_project_iam_member" "vertex_ai_user" {
  project = var.project_id
  role    = "roles/aiplatform.user"
  member  = "serviceAccount:${google_service_account.microservice["ai-nutrition-engine-service"].email}"
}

# Workload Identity binding lets each service's Kubernetes ServiceAccount
# impersonate its matching GCP service account without a downloaded key file.
resource "google_service_account_iam_member" "workload_identity" {
  for_each = toset(var.microservices)

  service_account_id = google_service_account.microservice[each.value].name
  role               = "roles/iam.workloadIdentityUser"
  member             = "serviceAccount:${var.project_id}.svc.id.goog[nutrihealth/${each.value}]"
}
