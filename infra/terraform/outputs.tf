output "gke_cluster_name" {
  value = google_container_cluster.primary.name
}

output "gke_cluster_endpoint" {
  value     = google_container_cluster.primary.endpoint
  sensitive = true
}

output "cloudsql_connection_name" {
  value = google_sql_database_instance.postgres.connection_name
}

output "redis_host" {
  value = google_redis_instance.cache.host
}

output "microservice_service_accounts" {
  value = { for name, sa in google_service_account.microservice : name => sa.email }
}

output "vpc_network_name" {
  value = google_compute_network.vpc.name
}

output "gke_subnetwork_name" {
  value = google_compute_subnetwork.gke.name
}
