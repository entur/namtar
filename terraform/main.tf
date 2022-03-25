# Contains main description of bulk of terraform?
terraform {
  required_version = ">= 0.13.2"
}

provider "google" {
  version = "~> 3.74.0"
}
provider "kubernetes" {
  version = "~> 1.13.4"
  load_config_file = var.load_config_file
}

# create service account
resource "google_service_account" "app_service_account" {
  account_id   = "${var.labels.team}-${var.labels.app}-sa"
  display_name = "${var.labels.team}-${var.labels.app} service account"
  project = var.gcp_project
}

# add service account as member to the bucket
resource "google_storage_bucket_iam_member" "storage_bucket_iam_member" {
  bucket = var.storage_bucket_name
  role   = var.service_account_bucket_role
  member = "serviceAccount:${google_service_account.app_service_account.email}"
}

  # add service account as member to the cloudsql client
resource "google_project_iam_member" "project" {
  project = var.gcp_project
  role    = var.service_account_cloudsql_role
  member = "serviceAccount:${google_service_account.app_service_account.email}"
}


# create key for service account
resource "google_service_account_key" "app_service_account_key" {
  service_account_id = google_service_account.app_service_account.name
}

  # Add SA key to to k8s
resource "kubernetes_secret" "app_service_account_credentials" {
  metadata {
    name      = "${var.labels.team}-${var.labels.app}-sa-key"
    namespace = var.kube_namespace
  }
  data = {
    "credentials.json" = "${base64decode(google_service_account_key.app_service_account_key.private_key)}"
  }
}

# Add app secrets
resource "kubernetes_secret" "ror-app-secrets" {
  metadata {
  name      = "${var.labels.team}-${var.labels.app}-secrets"
  namespace = var.kube_namespace
  }

  data = {
  "kafka-ssl-credentials" = var.ror-namtar-kafka-credentials
  "kafka-user-name"       = var.ror-namtar-kafka-user-name
  "kafka-user-password"   = var.ror-namtar-kafka-user-password
  "namtar-db-password"    = var.ror-namtar-db-password
  }
}

resource "google_sql_database_instance" "db_instance" {
  name = "namtar-db-pg13"
  database_version = "POSTGRES_13"
  project = var.gcp_resources_project
  region = var.ror-namtar-db-region

  settings {
    location_preference {
      zone = var.ror-namtar-db-zone
    }
    tier = var.ror-namtar-db-tier
    user_labels = var.labels
    availability_type = var.ror-namtar-db-availability
    backup_configuration {
      enabled = true
      // 01:00 UTC
      start_time = "01:00"
    }
    maintenance_window {
      // Sunday
      day = 7
      // 02:00 UTC
      hour = 2
    }
    ip_configuration {
      require_ssl = true
    }
  }

}

resource "google_sql_database" "db" {
  name = "namtar"
  project = var.gcp_resources_project
  instance = google_sql_database_instance.db_instance.name
}

resource "google_sql_user" "db-user" {
  name = var.ror-namtar-db-username
  project = var.gcp_resources_project
  instance = google_sql_database_instance.db_instance.name
  password = var.ror-namtar-db-password
}