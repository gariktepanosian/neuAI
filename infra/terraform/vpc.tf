# Phase 1.2: VPC + private subnet + Cloud NAT so GKE nodes and Cloud SQL /
# Memorystore use private IPs only (no public internet exposure).
#
# Architecture:
#   vpc "nutrihealth-<env>"
#     └── subnet "nutrihealth-<env>-gke"   (10.0.0.0/20 nodes, /16 pods, /22 services)
#         ├── GKE node pool
#         ├── Cloud SQL private IP (via Service Networking peering)
#         └── Memorystore Redis private IP
#   Cloud Router + Cloud NAT — outbound internet for nodes (apt, image pulls)

resource "google_compute_network" "vpc" {
  name                    = "nutrihealth-${var.environment}"
  project                 = var.project_id
  auto_create_subnetworks = false

  depends_on = [google_project_service.required]
}

resource "google_compute_subnetwork" "gke" {
  name          = "nutrihealth-${var.environment}-gke"
  project       = var.project_id
  region        = var.region
  network       = google_compute_network.vpc.id
  ip_cidr_range = "10.0.0.0/20" # 4094 node IPs

  secondary_ip_range {
    range_name    = "pods"
    ip_cidr_range = "10.4.0.0/16" # 65534 pod IPs (GKE alias IP)
  }

  secondary_ip_range {
    range_name    = "services"
    ip_cidr_range = "10.8.0.0/22" # 1022 ClusterIP service IPs
  }

  private_ip_google_access = true # allows nodes to reach Google APIs without NAT
}

# Cloud Router is needed for Cloud NAT (egress internet for nodes).
resource "google_compute_router" "router" {
  name    = "nutrihealth-${var.environment}"
  project = var.project_id
  region  = var.region
  network = google_compute_network.vpc.id
}

resource "google_compute_router_nat" "nat" {
  name                               = "nutrihealth-${var.environment}"
  project                            = var.project_id
  router                             = google_compute_router.router.name
  region                             = var.region
  nat_ip_allocate_option             = "AUTO_ONLY"
  source_subnetwork_ip_ranges_to_nat = "ALL_SUBNETWORKS_ALL_IP_RANGES"

  log_config {
    enable = true
    filter = "ERRORS_ONLY"
  }
}

# Private Services Access peering — required for Cloud SQL and Memorystore
# to receive private IP addresses within the VPC.
resource "google_compute_global_address" "private_services" {
  name          = "nutrihealth-${var.environment}-psconnect"
  project       = var.project_id
  purpose       = "VPC_PEERING"
  address_type  = "INTERNAL"
  prefix_length = 20
  network       = google_compute_network.vpc.id
}

resource "google_service_networking_connection" "private_vpc_connection" {
  network                 = google_compute_network.vpc.id
  service                 = "servicenetworking.googleapis.com"
  reserved_peering_ranges = [google_compute_global_address.private_services.name]

  depends_on = [google_project_service.required]
}

# ── Wire GKE cluster into this VPC ────────────────────────────────────────
# Re-export the network/subnetwork selectors as locals for use in gke.tf.
# (Terraform doesn't allow circular depends_on across files, so we pass
# the self_link values as locals.)
locals {
  gke_network    = google_compute_network.vpc.self_link
  gke_subnetwork = google_compute_subnetwork.gke.self_link
}
