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
