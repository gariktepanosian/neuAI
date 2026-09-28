# Phase 1, Step 1.2: HA PostgreSQL for the two services with relational
# data (users/RBAC, subscriptions/billing) per the spec's schema.
resource "google_sql_database_instance" "postgres" {
  name             = "nutrihealth-${var.environment}"
  project          = var.project_id
  region           = var.region
  database_version = "POSTGRES_16"

  settings {
    tier              = var.cloudsql_tier
    availability_type = "REGIONAL" # HA: synchronous standby in a second zone

    backup_configuration {
      enabled                        = true
      point_in_time_recovery_enabled = true
    }

    ip_configuration {
      ipv4_enabled = false
      # private_network intentionally left unset here — wire to a VPC via
      # google_compute_network/google_service_networking_connection once the
      # networking module for this environment is decided.
    }
  }

  deletion_protection = true

  depends_on = [google_project_service.required]
}

resource "google_sql_database" "subscription_db" {
  name     = "subscription_db"
  project  = var.project_id
  instance = google_sql_database_instance.postgres.name
}

resource "random_password" "postgres_app_user" {
  length  = 32
  special = false
}

resource "google_sql_user" "app_user" {
  name     = "nutrihealth_app"
  project  = var.project_id
  instance = google_sql_database_instance.postgres.name
  password = random_password.postgres_app_user.result
}
