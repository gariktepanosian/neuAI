# Phase 1, Step 1.2: multi-zone GKE cluster for zero-downtime rolling updates
# and autoscaling worker nodes, per the spec's container orchestration choice.
resource "google_container_cluster" "primary" {
  name     = "nutrihealth-${var.environment}"
  project  = var.project_id
  location = var.region # regional (not zonal) cluster spans var.zones automatically

  # Manage node pools separately for independent lifecycle/scaling control.
  remove_default_node_pool = true
  initial_node_count       = 1

  networking_mode = "VPC_NATIVE"
  ip_allocation_policy {}

  workload_identity_config {
    workload_pool = "${var.project_id}.svc.id.goog"
  }

  release_channel {
    channel = "REGULAR"
  }

  depends_on = [google_project_service.required]
}

resource "google_container_node_pool" "primary_nodes" {
  name     = "primary-node-pool"
  project  = var.project_id
  location = var.region
  cluster  = google_container_cluster.primary.name

  node_locations = var.zones

  autoscaling {
    min_node_count = var.gke_min_node_count
    max_node_count = var.gke_max_node_count
  }

  node_config {
    machine_type = var.gke_node_machine_type
    disk_size_gb = 50

    workload_metadata_config {
      mode = "GKE_METADATA"
    }

    oauth_scopes = [
      "https://www.googleapis.com/auth/cloud-platform",
    ]
  }

  management {
    auto_repair  = true
    auto_upgrade = true
  }
}
