# Stage 2 API contract

## Frontend-facing

`GET /search` is provided by Search Service.

| Parameter | Required | Rules |
|---|---:|---|
| `q` | yes | Non-empty keyword or phrase |
| `author` | no | Case-insensitive substring |
| `language` | no | Case-insensitive exact match |
| `year` | no | Integer |
| `offset` | no | Integer, default `0` |
| `limit` | no | Integer, default `20`, maximum `100` |

The response contains `query`, `filters`, `total`, `offset`, `limit`, and
metadata results. No matches are represented by HTTP 200 with an empty list.

## Internal APIs

| Provider | Method | Route | Meaning |
|---|---|---|---|
| Ingestion | POST | `/ingest/{book_id}` | Download and atomically publish header/body |
| Ingestion | GET | `/ingest/status/{book_id}` | Readiness check |
| Ingestion | GET | `/ingest/content/{book_id}` | Read published content |
| Ingestion | GET | `/ingest/list` | List ready books |
| Indexing | POST | `/index/update/{book_id}` | Idempotently replace a book's index contribution |
| Indexing | POST | `/index/rebuild` | Clear the current published index |
| Indexing | GET | `/index/status` | Read index statistics |
| Control | POST | `/books/{book_id}/ingest` | Orchestrate ingestion, readiness/content read, and indexing |
| Control | GET | `/workflow/status/{book_id}` | Read the persisted orchestration state |

State-changing requests return only after their local operation has completed.
Repeated ingestion and indexing requests do not create duplicate postings.
