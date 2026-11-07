# Stage 2 service-oriented architecture

The project is a single-machine SOA implementation with four Java 17
microservices:

| Service | Port | Responsibility |
|---|---:|---|
| Control Module | 7000 | Orchestrates ingestion and indexing |
| Ingestion Service | 7001 | Downloads and stores Gutenberg books |
| Indexing Service | 7002 | Builds and persists the inverted index |
| Search Service | 7003 | Exposes the public search API |

## Build and run

From the repository root, build all services:

```powershell
mvn clean test
mvn package -DskipTests
```

Run each shaded JAR in a separate terminal. Keep all four terminals open:

```powershell
java -jar .\ingestion-service\target\ingestion-service-1.0.0-shaded.jar
java -jar .\indexing-service\target\indexing-service-1.0.0-shaded.jar
java -jar .\search-service\target\search-service-1.0.0-shaded.jar
java -jar .\control-module\target\control-module-1.0.0-shaded.jar
```

The services use `INGESTION_SERVICE_URL`, `INDEXING_SERVICE_URL` and
`SEARCH_PORT`/`INDEXING_PORT`/`INGESTION_PORT`/`CONTROL_PORT` for deployment
configuration. Inter-service clients support configurable
`SERVICE_CONNECT_TIMEOUT_SECONDS`, `SERVICE_REQUEST_TIMEOUT_SECONDS` and
`SERVICE_MAX_ATTEMPTS` (default: 3 attempts).

## Validation and benchmarks

PowerShell may require a temporary execution-policy bypass:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\scripts\api-smoke.ps1"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\scripts\integration-benchmark.ps1" -Concurrency "1,2,4,8" -RequestsPerClient 50
```

The smoke test checks health, combined filters, empty results, invalid
parameters, ingestion validation and workflow status. The integration
benchmark records throughput, p50/p95/p99 latency, Java CPU time delta and
Java working-set memory in `integration-benchmark.csv`.

The internal JMH benchmark is `IndexingBenchmark` and measures index updates
with five warmup and ten measurement iterations.

## Data and recovery

Ingestion stores `header` and `body` files below `data/datalake`; the index is
persisted in `data/indexing/index-state.json`; workflow states are persisted
in `data/control/workflow-state.json`. Writes use temporary files and atomic
replacement where supported.

The Control Module marks failures explicitly and clients retry transient
connection I/O up to `SERVICE_MAX_ATTEMPTS`. A failed book can be retried by
calling the same endpoint again; ingestion and indexing are idempotent.

## API documentation

Contracts and examples are in:

- `docs/api-contract.md`
- `docs/search-openapi.yaml`
- `ingestion-service/docs/openapi.yaml`

The architecture and Stage 3 evolution notes are in `docs/architecture.md`.
