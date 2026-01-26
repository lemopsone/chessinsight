service {
  name = "backend_api_read"
  id = "backend_api_read_ro2"
  address = "backend_ro2"
  port = 8082
  tags = []
  check {
    tcp = "backend_ro2:8082"
    interval = "10s"
    timeout = "2s"
  }
}
