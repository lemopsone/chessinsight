# Ручное нагрузочное тестирование `/move-evaluations` (mock Stockfish)

Тестируем endpoint оценки отдельного хода, чтобы не создавать большое количество партий.

## 0) Поднять инфраструктуру

```bash
docker compose --profile mock up -d
```

Проверка:

```bash
docker compose ps backend stockfish-mock prometheus cadvisor grafana
```

UI:
- Grafana: `http://localhost/monitoring/`
- Prometheus: `http://localhost:9090`
- Backend API: `http://localhost:8080/api`

## 1) Один раз получить токен (общий для всех прогонов)

```bash
python3 ci/perf/manual_prepare_analysis_state.py
```

Скрипт создаёт пользователя `perf_move_user` (или логинится им, если уже есть) и сохраняет токен в:

`reports/perf/manual-state/auth.json`

Этот токен используется во всех последующих запусках k6.

## 2) Фиксированный payload для move-analysis

По умолчанию используется файл:

`ci/perf/move-analysis-payloads.json`

Формат:

```json
{
  "moveNum": 1,
  "positionFEN": "...",
  "moveSAN": "e4",
  "moveUCI": "e2e4"
}
```

Файл должен содержать ровно один ход.

## 3) Подать нагрузку вручную через k6 (2 режима)

Фиксированный RPS:

```bash
python3 ci/perf/manual_run_k6.py --mode fixed --rps 20
```

По умолчанию `manual_run_k6.py` читает токен из `reports/perf/manual-state/auth.json`.
Скрипт не пишет отчеты в файлы, метрики смотрим только в Grafana/Prometheus.
Остановить прогон: `Ctrl+C` (скрипт остановит k6-контейнер).

Ramping до максимального RPS:

```bash
python3 ci/perf/manual_run_k6.py --mode ramping --max-rps 40
```

## 4) Поиск точки деградации (вручную)

```bash
python3 ci/perf/manual_run_k6.py --mode fixed --rps 5
python3 ci/perf/manual_run_k6.py --mode fixed --rps 10
python3 ci/perf/manual_run_k6.py --mode fixed --rps 20
python3 ci/perf/manual_run_k6.py --mode fixed --rps 40
```

Деградация: первый RPS, где:
- `error rate > 3%`, или
- `p95`/`p99` выше SLA.

`max_acceptable_rps` = предыдущий уровень.

## 5) Steady-тест

```bash
python3 ci/perf/manual_run_k6.py --mode fixed --rps <max_acceptable_rps>
```

## 6) Recovery-тест

Перегруз:

```bash
python3 ci/perf/manual_run_k6.py --mode fixed --rps <overload_rps>
```

Затем steady:

```bash
python3 ci/perf/manual_run_k6.py --mode fixed --rps <max_acceptable_rps>
```

В Grafana фиксируем момент возврата `p99` и error rate к steady-уровню.

## Где смотреть метрики

Дашборд: `ChessInsight Move Analysis Perf`.

Ключевые панели:
- RPS
- p95/p99 latency
- error rate
- backend CPU/RAM
- backend disk read/write (bytes/sec)
- Hikari connections

## Если после ~40 RPS latency резко растет

Проверьте два лимитера:

- пул сессий к Stockfish (`engine.stockfish.poolSize`, сейчас дефолт `64`);
- кэш загрузки пользователя в JWT-фильтре (`security.auth.user-cache-ttl-seconds`, сейчас `60`).

Обе настройки можно менять через env перед стартом compose:

```bash
export ENGINE_STOCKFISH_POOL_SIZE=24
export SECURITY_AUTH_USER_CACHE_TTL_SECONDS=300
export SERVER_TOMCAT_THREADS_MAX=64
export SERVER_TOMCAT_MAX_CONNECTIONS=200
export SERVER_TOMCAT_ACCEPT_COUNT=40
docker compose --profile mock up -d
```

## Остановка

```bash
docker compose down -v
```
