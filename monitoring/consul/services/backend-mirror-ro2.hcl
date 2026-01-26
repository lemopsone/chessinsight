service {
  name = "backend_mirror_api_read"
  id = "backend_mirror_api_read_ro2"
  address = "backend_mirror_ro2"
  port = 8085
  tags = []
  check {
    tcp = "backend_mirror_ro2:8085"
    interval = "10s"
    timeout = "2s"
  }
}
