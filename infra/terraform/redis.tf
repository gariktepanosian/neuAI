# Phase 1, Step 1.2: Memorystore Redis for logistics-dispatch-service's
# geospatial courier tracking, plus session/rate-limit caching platform-wide.
resource "google_redis_instance" "cache" {
  name           = "nutrihealth-${var.environment}"
  project        = var.project_id
  region         = var.region
  tier           = "STANDARD_HA" # HA: automatic failover to a replica
  memory_size_gb = var.redis_memory_size_gb
  redis_version  = "REDIS_7_2"

  depends_on = [google_project_service.required]
}
