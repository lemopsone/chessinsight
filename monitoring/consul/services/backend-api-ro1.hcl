service {
  name = "backend_api_read"
  id = "backend_api_read_ro1"
  address = "backend_ro1"
  port = 8081
  tags = []
  check {
    tcp = "backend_ro1:8081"
    interval = "10s"
    timeout = "2s"
  }
}
