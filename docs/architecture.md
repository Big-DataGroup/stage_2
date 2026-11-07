# Architecture and Stage 3 evolution

```text
Client
  |
  v
Search Service ----HTTP----> Indexing Service ----> persisted index

Control Module ----HTTP----> Ingestion Service ----> datalake
       |
       +-----------HTTP------> Indexing Service
```

The deployment is intentionally single-machine. Each service is an
independent JVM and communicates through HTTP, which keeps service boundaries
explicit while avoiding a broker dependency for Stage 2. The Control Module
uses orchestration: it records a workflow state, waits for successful
ingestion, reads the stored content and then submits it to Indexing.

Ingestion and Indexing use idempotent writes. Temporary files are replaced
atomically where supported, and the index is persisted after updates. The
clients use timeouts and bounded retries for transient connection failures.

For Stage 3, the services can move to separate nodes behind service discovery
and load balancing. The local JSON state stores should be replaced by durable
shared storage, workflow coordination should use a durable queue or workflow
engine, and metrics should be exported to a central monitoring system.
Indexing replicas would need a shared update log or partitioned index with
replica recovery rather than independent local files.
