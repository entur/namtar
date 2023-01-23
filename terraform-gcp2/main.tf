# Contains main description of bulk of terraform?
terraform {
  required_version = ">= 0.13.2"
}

provider "google" {
  version = "~> 4.32.0"
}
provider "kubernetes" {
  version = "~> 2.13.1"
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