# vim: set syntax=hcl:

variable "app_version" {
  default = "5.0.0"
}

target "default" {
  context = "."
  dockerfile = "./default.dockerfile"
  contexts = {
  }
  args = {
    git_commit = ""
    app_version = "${app_version}"
  }
  tags = [
    "docker-registry.eosc.athenarc.gr/resource-catalogue:latest"
  ]
}
