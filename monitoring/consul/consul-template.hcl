consul {
  address = "consul:8500"

  retry {
    enabled  = true
    attempts = 15
    backoff  = "250ms"
  }
}

template {
  source      = "/etc/nginx/conf.d/01-upstreams-consul.conf.ctmpl"
  destination = "/etc/nginx/conf.d/01-upstreams-consul.conf"
  perms       = 0644
  command     = ["nginx", "-s", "reload"]
}
