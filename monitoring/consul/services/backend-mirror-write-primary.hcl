service {
  name = "backend_mirror_api_write"
  id = "backend_mirror_api_write_primary"
  address = "backend_mirror"
  port = 8083
  tags = ["primary"]
  check {
    tcp = "backend_mirror:8083"
    interval = "10s"
    timeout = "2s"
  }
}
