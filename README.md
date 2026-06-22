# Search Typeahead System

Created by Antara Sanjay Utane  
Email: [antarautane11@gmail.com](mailto:antarautane11@gmail.com)

## Project Overview
This project implements a search typeahead system similar to the suggestion experience in search engines and e-commerce sites. Users can type a prefix, receive up to 10 ranked suggestions, submit a search, and then observe eventual count updates and trending results after batch processing. The implementation uses PostgreSQL for durable storage, Redis for repeated prefix caching, and an in-memory batch queue for write aggregation.

The assignment-core implementation in this repository is:
- PostgreSQL + Flyway for schema and durable data
- Redis for suggestion caching
- Spring Boot for API and batch coordination
- React + Vite for the final UI
- An in-memory batch queue for eventual count updates

Future production upgrades such as OpenSearch, Kafka, Redis Cluster, Prometheus, and Grafana are documented as tradeoffs and next steps, but they are not part of the current implementation.

## Tech Stack
- Frontend: React + Vite
- Backend: Java 21 + Spring Boot
- Database: PostgreSQL
- Migration: Flyway
- Cache: Redis
- Local orchestration: Docker Compose
- Dataset: Synthetic 100,000 query CSV

## Features
- 100,000 synthetic query dataset generated into `data/queries.csv`
- Dataset loader for `search_queries` and `query_prefixes`
- Prefix suggestions backed by normalized prefix lookup
- Maximum 10 suggestions per request
- Suggestions begin with the normalized prefix
- Count-based ranking for returned suggestions
- Mixed-case, empty-query, and no-match handling
- React UI with debounced suggestion fetches
- Search submission with `POST /search`
- Search submission through the Search button and Enter key
- Loading, error, empty-query, and no-results UI states
- Basic keyboard-friendly suggestion navigation and support
- Display of dummy search response after submission
- Batch-written count updates through an in-memory queue
- Redis cache checked before PostgreSQL
- Logical consistent hashing for cache-node routing
- Cache TTL plus invalidation after successful writes
- Cache routing inspection through `/cache/debug`
- Batch write aggregation to collapse repeated queries
- Trending searches from time-window activity buckets
- Metrics summary for cache and write behavior
- Final pastel UI with screenshots for the submission pack

## Architecture
```text
React UI
  -> Spring Boot API
     -> Redis cache for suggestions
        -> PostgreSQL query_prefixes fallback

POST /search
  -> in-memory queue
     -> batch writer
        -> PostgreSQL search_queries + query_prefixes
        -> query_activity_buckets
        -> cache invalidation
```

Read path:
- The React UI calls `GET /suggest?q=<prefix>`.
- Spring Boot normalizes the prefix and looks for a routed Redis entry first.
- On a cache miss, the backend reads PostgreSQL `query_prefixes`, returns the result, and populates Redis.

Write path:
- The UI calls `POST /search` with a query string.
- The backend validates and normalizes the query, enqueues it, and returns `{ "message": "Searched" }` immediately.

Cache path:
- Suggestion lookups are routed onto logical cache nodes using consistent hashing.
- Cached prefixes use TTL and are invalidated after successful updates for affected prefixes.

Batch path:
- A scheduled/manual batch writer drains the in-memory queue.
- Repeated normalized queries are aggregated before PostgreSQL writes.
- Successful flushes update search counts, prefix counts, activity buckets, and audit rows.

Trending path:
- `GET /trending` reads aggregated activity from `query_activity_buckets` within a selected time window and joins `search_queries` for total counts and display text.

Metrics path:
- `GET /metrics/summary` combines in-memory counters with durable PostgreSQL aggregates to summarize cache usage, write aggregation, and database table sizes.

More detail is available in [docs/architecture.md](docs/architecture.md).

## Database Schema
The core tables are:
- `search_queries`: one row per normalized search query with display text and durable total count
- `query_prefixes`: prefix expansion table used to serve fast prefix suggestions
- `query_activity_buckets`: time-window bucket table used for trending calculations
- `batch_flush_audit`: audit trail for successful batch flushes
- `processed_kafka_offsets`: reserved table for a future Kafka-based ingestion path

## APIs
The backend exposes the following endpoints:
- `GET /health`
- `GET /suggest?q=iph`
- `POST /search`
- `GET /cache/debug?prefix=iph`
- `GET /batch/debug`
- `POST /batch/flush`
- `GET /trending?window=24h&limit=5`
- `GET /metrics/summary`

Clean endpoint documentation lives in [docs/api.md](docs/api.md).

Example proof files used in the documentation:
- [docs/proof/cache-debug-iph.json](docs/proof/cache-debug-iph.json)
- [docs/proof/batch-flush-success.json](docs/proof/batch-flush-success.json)
- [docs/proof/trending-output.json](docs/proof/trending-output.json)
- [docs/proof/metrics-summary.json](docs/proof/metrics-summary.json)
- [docs/proof/db-search-query-count.txt](docs/proof/db-search-query-count.txt)
- [docs/proof/db-prefix-count.txt](docs/proof/db-prefix-count.txt)

## UI Screenshots
Saved screenshots:
- [01-home-trending](docs/screenshots/01-home-trending.png)
- [02-iph-suggestions](docs/screenshots/02-iph-suggestions.png)
- [03-spring-boot-suggestions](docs/screenshots/03-spring-boot-suggestions.png)
- [06-fresh-query-no-results](docs/screenshots/06-fresh-query-no-results.png)
- [07-fresh-query-search-response](docs/screenshots/07-fresh-query-search-response.png)
- [08-fresh-query-added-suggestion](docs/screenshots/08-fresh-query-added-suggestion.png)

The fresh-query sequence proves:
- the new query initially had no suggestions
- the user submitted it through Search
- after batch update and refresh it appeared as a suggestion

## Local Setup
Start Docker services:

```powershell
docker compose up -d
```

Run the backend:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Run the frontend:

```powershell
cd frontend
npm run dev
```

Generate the dataset:

```powershell
python .\data\generate_dataset.py --rows 100000 --output .\data\queries.csv
```

Load the dataset:

```powershell
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--app.dataset.load=true --app.dataset.path=../data/queries.csv"
```

Run backend tests:

```powershell
cd backend
.\mvnw.cmd test
```

Build the frontend:

```powershell
cd frontend
npm run build
```

## Performance Report Summary
The saved proof in [docs/proof/metrics-summary.json](docs/proof/metrics-summary.json) shows:
- Cache hit rate: `0.2692307692307692` (about `26.92%`)
- Suggestion request count: `312`
- PostgreSQL read count: `228`
- Batch raw events processed: `73`
- Unique query writes: `44`
- Estimated DB writes avoided: `29`
- Database rows:
  - `searchQueries`: `100020`
  - `queryPrefixes`: `4369591`
  - `activityBuckets`: `30`
  - `batchAuditRows`: `44`

These numbers demonstrate:
- repeated prefix reads benefit from Redis
- repeated searches can be collapsed into fewer PostgreSQL writes
- prefix-table storage is large but simple to query for the assignment

If exact p95 latency is needed, it can be measured separately with the smoke script or a dedicated load tool. This repository currently includes `scripts/perf-smoke.ps1` as a lightweight verification script rather than a full benchmarking harness.

The full write-up is available in [docs/performance-report.md](docs/performance-report.md).

## Additional Documentation
- [Architecture](docs/architecture.md)
- [API Reference](docs/api.md)
- [Performance Report](docs/performance-report.md)
- [Design Tradeoffs](docs/design-tradeoffs.md)
- [Submission Checklist](docs/submission-checklist.md)
