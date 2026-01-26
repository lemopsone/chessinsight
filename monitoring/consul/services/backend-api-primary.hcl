service {
  name = "backend_api_read"
  id = "backend_api_read_primary"
  address = "backend"
  port = 8080
  tags = ["primary"]
  check {
    tcp = "backend:8080"
    interval = "10s"
    timeout = "2s"
  }
}
