variable "project_id" {
  description = "GCP project id to deploy NutriHealth AI infrastructure into. Must already exist (billing enabled)."
  type        = string
}

variable "region" {
  description = "Primary GCP region. Leave the data-residency question (EU vs US) resolved before setting this."
  type        = string
  default     = "us-central1"
}

variable "zones" {
  description = "Zones within the region for the GKE node pool, for multi-zone availability."
  type        = list(string)
  default     = ["us-central1-a", "us-central1-b", "us-central1-c"]
}

variable "environment" {
  description = "Deployment environment name, used as a resource-name suffix (dev/staging/prod)."
  type        = string
  default     = "dev"
}

variable "gke_node_machine_type" {
  type    = string
  default = "e2-standard-4"
}

variable "gke_min_node_count" {
  type    = number
  default = 1
}

variable "gke_max_node_count" {
  type    = number
  default = 5
}

variable "cloudsql_tier" {
  description = "Cloud SQL machine tier. db-custom-2-7680 = 2 vCPU / 7.5GB, a reasonable HA starting point."
  type        = string
  default     = "db-custom-2-7680"
}

variable "redis_memory_size_gb" {
  type    = number
  default = 5
}

variable "microservices" {
  description = "Microservices that get a dedicated GCP service account and Secret Manager access."
  type        = list(string)
  default = [
    "clinic-diagnostic-service",
    "user-auth-service",
    "subscription-order-service",
    "ai-nutrition-engine-service",
    "logistics-dispatch-service",
  ]
}
