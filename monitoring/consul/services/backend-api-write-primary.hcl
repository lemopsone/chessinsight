service {
  name = "backend_api_write"
  id = "backend_api_write_primary"
  address = "backend"
  port = 8080
  tags = ["primary"]
  check {
    tcp = "backend:8080"
    interval = "10s"
    timeout = "2s"
  }
}
