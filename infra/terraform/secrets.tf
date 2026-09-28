# Phase 1, Step 1.4: Secret Manager holds every credential the services
# currently take as a `${VAR:dev-placeholder}` in application.yml. Symmetric
# keys (JWT, PHI encryption) are generated here; third-party keys (Stripe,
# Gemini) get an empty placeholder version — replace it with the real value
# via `gcloud secrets versions add <name> --data-file=-` before deploying.

resource "random_id" "jwt_signing_key" {
  byte_length = 32
}

resource "random_id" "phi_encryption_key" {
  byte_length = 32
}

locals {
  secrets = {
    "jwt-signing-key" = {
      value    = random_id.jwt_signing_key.b64_std
      services = ["user-auth-service"]
    }
    "phi-encryption-key" = {
      value    = random_id.phi_encryption_key.b64_std
      services = ["clinic-diagnostic-service"]
    }
    "stripe-api-key" = {
      value    = "REPLACE_ME"
      services = ["subscription-order-service"]
    }
    "stripe-webhook-signing-secret" = {
      value    = "REPLACE_ME"
      services = ["subscription-order-service"]
    }
    "gemini-api-key" = {
      value    = "REPLACE_ME"
      services = ["ai-nutrition-engine-service"]
    }
  }
}

resource "google_secret_manager_secret" "this" {
  for_each = local.secrets

  project   = var.project_id
  secret_id = "${each.key}-${var.environment}"

  replication {
    auto {}
  }

  depends_on = [google_project_service.required]
}

resource "google_secret_manager_secret_version" "initial" {
  for_each = local.secrets

  secret = google_secret_manager_secret.this[each.key].id

  # Third-party keys start as a placeholder — Terraform won't fight you for
  # updating them manually afterwards (see the README for how).
  secret_data = each.value.value

  lifecycle {
    ignore_changes = [secret_data]
  }
}

# Flatten {secret -> [services]} into one IAM binding per (secret, service) pair.
locals {
  secret_access_bindings = flatten([
    for secret_name, cfg in local.secrets : [
      for service in cfg.services : {
        secret_name = secret_name
        service     = service
      }
    ]
  ])
}

resource "google_secret_manager_secret_iam_member" "accessor" {
  for_each = {
    for binding in local.secret_access_bindings :
    "${binding.secret_name}-${binding.service}" => binding
  }

  project   = var.project_id
  secret_id = google_secret_manager_secret.this[each.value.secret_name].secret_id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.microservice[each.value.service].email}"
}
