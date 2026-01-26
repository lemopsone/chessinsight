service {
  name = "backend_mirror_api_read"
  id = "backend_mirror_api_read_ro1"
  address = "backend_mirror_ro1"
  port = 8084
  tags = []
  check {
    tcp = "backend_mirror_ro1:8084"
    interval = "10s"
    timeout = "2s"
  }
}
