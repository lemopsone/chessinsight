service {
  name = "stockfish"
  id = "stockfish_3"
  address = "stockfish-3"
  port = 5555
  tags = ["engine"]
  check {
    tcp = "stockfish-3:5555"
    interval = "10s"
    timeout = "2s"
  }
}
