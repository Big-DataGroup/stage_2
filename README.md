# Stage 2 - Service-Oriented Search Engine

This local implementation contains the Stage 2 services required by the assignment:
Ingestion, Indexing, Search, and the HTTP Control Module.

## Build and tests

Requires Java 17 and Maven.

```bash
mvn clean test
mvn package
```

## Run locally

Start each service in a separate terminal:

```bash
java -jar ingestion-service/target/ingestion-service-1.0.0.jar
java -jar indexing-service/target/indexing-service-1.0.0.jar
java -jar search-service/target/search-service-1.0.0.jar
java -jar control-module/target/control-module-1.0.0.jar
```

Default ports are 7001, 7002, 7003, and 7000 respectively. Override them with
`INGESTION_PORT`, `INDEXING_PORT`, `SEARCH_PORT`, and `CONTROL_PORT`.
The indexing state is persisted at `data/indexing/index-state.json` and can be
changed with `INDEX_STATE_PATH`.

## API examples

```bash
curl "http://localhost:7003/search?q=adventure&offset=0&limit=20"
curl "http://localhost:7003/search?q=adventure&author=Defoe&language=en&year=1719"
curl -X POST http://localhost:7001/ingest/1342
curl -X POST http://localhost:7000/books/1342/ingest
curl http://localhost:7002/index/status
curl -X POST http://localhost:7002/index/rebuild
curl http://localhost:7000/workflow/status/1342
```

The frontend-facing contract is `GET /search`. Ingestion and indexing endpoints
are internal service APIs. A valid query with no matches returns HTTP 200 and an
empty `results` array; malformed query parameters return HTTP 400.

## Stage 2 coordination model

The Control Module performs ingestion first, reads the completed header/body
files, and only then requests indexing. Ingestion records readiness after both
files are atomically written. Indexing updates are idempotent by book ID and
persisted atomically. The Control Module persists workflow states in
`data/control/workflow-state.json` (override with `CONTROL_STATE_PATH`), and
Search reads only the persisted, completed index state.

All processes run on one host. Ports, paths, and service URLs are configurable
through environment variables. The service processes may be replicated, but
shared-file locking and work claiming should be added before using replicas in
production; this is explicitly part of the Stage 3 evolution.

## API specification

The ingestion contract is in `ingestion-service/docs/openapi.yaml`. The
implemented indexing and search routes are listed above and their request and
response semantics are covered by the service tests and this README.

With all services running, execute the PowerShell validation script:

```powershell
.\scripts\api-smoke.ps1
```

For a reproducible single-machine concurrency experiment, run:

```powershell
.\scripts\integration-benchmark.ps1 -Concurrency 1,2,4,8
```

The script reports throughput as client concurrency changes. CPU, memory,
replica count, thread settings, host hardware, and percentile latency should
be recorded alongside the generated CSV for the Stage 2 report.
