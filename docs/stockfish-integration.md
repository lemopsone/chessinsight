# Мок-сервер Stockfish

Взаимодействие с сервером Stockfish осуществляется по TCP, формат команд - UCI

## Контракт

[Документация формата обмена данными](https://official-stockfish.github.io/docs/stockfish-wiki/UCI-&-Commands.html)

Используется следующий набор команд

- Request: `uci` (установка режима работы с нотацией UCI)
  Response: строки `id name ...`, опционально `id author ...`, завершающая строка `uciok`
- Request: `isready` (проверка готовности сервера)
  Response: `readyok`
- Request: `ucinewgame` (сброс контекста предыдущих позиций, начало оценки новой игры)
  Response: no output
- Request: `position fen <fen>` (указание текщей позиции)
  Response: no output
- Request: `go depth <n>` (или `go movetime <ms>`) (анализ текущей позиции, установленной через `position`) 
  Response: одна или несколько строк, начинающихся с `info ...` и завершающая строка `bestmove <uci>`

Контракт выполняется обоими видами серверов:

- реальный: Stockfish-сервер с доступом по TCP (`Dockerfile.stockfish`)
- моковый: `test-fixtures/stockfish-mock/mock_stockfish_server.py`

## Выбор реализации

Используемая версия движка указывается в конфиге:

- в рантайме: переменные окружения/spring-конфиг `ENGINE_STOCKFISH_HOST` и `ENGINE_STOCKFISH_PORT`
- E2E: переменная окружения `TEST_STOCKFISH_PROVIDER=mock|real`

## E2E

- `web/src/test/java/ru/chessinsight/e2e/GameCreationScenarioE2E.java`

Осуществляемые действия:

1. Регистрация пользователя.
2. Импорт партии в нотации PGN.
3. Анализ партии ручкой `POST /api/v1/games/{gameId}/analysis`.
4. Проверка корректности ответа сервера.
5. Удаление созданной партии.
