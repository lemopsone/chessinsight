consul {
  address = "consul:8500"

  retry {
    enabled  = true
    attempts = 15
    backoff  = "250ms"
  }
}

template {
  source      = "/etc/nginx/stockfish-stream.conf.ctmpl"
  destination = "/etc/nginx/nginx.conf"
  perms       = 0644
  command     = ["nginx", "-s", "reload"]
}
