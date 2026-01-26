service {
  name = "stockfish"
  id = "stockfish_1"
  address = "stockfish-1"
  port = 5555
  tags = ["engine"]
  check {
    tcp = "stockfish-1:5555"
    interval = "10s"
    timeout = "2s"
  }
}
