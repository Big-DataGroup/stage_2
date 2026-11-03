# API contract

## Public endpoints

### Control Module (`7000`)

- `GET /health` returns a plain-text health message.
- `POST /books/{book_id}/ingest` runs `Ingestion -> content retrieval -> Indexing`.
- `GET /workflow/status/{book_id}` returns the latest workflow state.

### Search Service (`7003`)

`GET /search?q=<term>&author=<author>&language=<language>&year=<year>&offset=<n>&limit=<n>`
returns a JSON result containing `query`, `filters`, `total`, `offset`, `limit`
and `results`. `limit` is between 1 and 100 and `offset` cannot be negative.

- `200`: search result, including an empty `results` array.
- `400`: missing or invalid query/filter/pagination values.
- `502`: Indexing Service is unavailable.
- `503`: the request was interrupted.

`GET /health` returns `{ "service": "search-service", "status": "running" }`.

### Ingestion Service (`7001`)

- `POST /ingest/{book_id}` downloads and stores a book.
- `GET /ingest/status/{book_id}` checks availability.
- `GET /ingest/content/{book_id}` returns the stored header and body.
- `GET /ingest/list` lists available book identifiers.
- `GET /health` checks service availability.

Invalid identifiers return `400`; missing Gutenberg books return `404`;
upstream failures return `502`; storage failures return `500`.

### Indexing Service (`7002`)

- `POST /index/update/{book_id}` indexes plain-text content idempotently.
- `GET /index/search` provides the internal search operation.
- `GET /index/status` returns indexed book and term counts.
- `POST /index/rebuild` reloads the persisted index.
- `GET /health` checks service availability.

## Coordination

The Control Module is the orchestrator. It does not send content to Indexing
until Ingestion has returned success and the stored content can be read.
Transient connection errors are retried by the HTTP clients; failed workflows
are recorded with a `FAILED:` state and can be retried with the same request.
