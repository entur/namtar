#Enviroment variables
variable "gcp_project" {
    description = "The GCP project id"
}

variable "gcp_resources_project" {
    description = "The GCP project id"
}

variable "kube_namespace" {
  description = "The Kubernetes namespace"
}

variable "labels" {
  description = "Labels used in all resources"
  type        = map(string)
     default = {
       manager = "terraform"
       team    = "ror"
       slack   = "talk-ror"
       app     = "namtar"
     }
}

variable "load_config_file" {
  description = "Do not load kube config file"
  default     = false
}

variable "storage_bucket_name" {
  description = "GCP bucket name"
}

variable "service_account_bucket_role" {
  description = "Role of the Service Account - more about roles https://cloud.google.com/storage/docs/access-control/iam-roles"
  default     = "roles/storage.objectViewer"
}

variable "service_account_cloudsql_role" {
  description = "Role of the Service Account - more about roles https://cloud.google.com/pubsub/docs/access-control"
  default     = "roles/cloudsql.client"
}

variable "ror-namtar-kafka-credentials" {
  description = "Namtar kafka ssl credentials"
}

variable  "ror-namtar-kafka-user-name" {
  description = "Namtar kafka user name"
}

variable "ror-namtar-kafka-user-password" {
  description = "Namtar kafka user password"
}

variable "ror-namtar-db-instance-name" {
  default = "namtar-db-pg13"
}

variable "ror-namtar-db-password" {
  description = "Namtar database password"
}

variable "ror-namtar-db-username" {
  description = "Namtar database user"
  default = "namtar"
}

variable "ror-namtar-db-region" {
  description = "GCP region"
  default = "europe-west1"
}

variable "ror-namtar-db-zone" {
  description = "GCP zone letter"
  default = "europe-west1-b"
}

variable "ror-namtar-db-tier" {
  description = "Database instance tier"
  default = "db-g1-small"
}

variable "ror-namtar-db-availability" {
  description = "Database availability"
  default = "ZONAL"
}



