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
  destination = "/etc/nginx/streams-enabled/stockfish.conf"
  perms       = 0644
  command     = "nginx -t && nginx -s reload"
}