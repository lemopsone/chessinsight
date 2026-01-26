service {
  name = "stockfish"
  id = "stockfish_2"
  address = "stockfish-2"
  port = 5555
  tags = ["engine"]
  check {
    tcp = "stockfish-2:5555"
    interval = "10s"
    timeout = "2s"
  }
}
