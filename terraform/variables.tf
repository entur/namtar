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
