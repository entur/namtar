#Enviroment variables

variable "kube_namespace" {
  description = "The Kubernetes namespace"
  default = "namtar"
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

variable "ror-namtar-kafka-credentials" {
  description = "Namtar kafka ssl credentials"
}

variable  "ror-namtar-kafka-user-name" {
  description = "Namtar kafka user name"
}

variable "ror-namtar-kafka-user-password" {
  description = "Namtar kafka user password"
}

variable "ror-namtar-db-password" {
  description = "Namtar database password"
}



