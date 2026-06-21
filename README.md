# Search Typeahead System

## Overview
This repository contains Milestone 11 of a high-level design assignment project for a Search Typeahead System. The current scope includes the initial project skeleton, local development workflow, PostgreSQL infrastructure, Flyway-managed schema setup, synthetic dataset generation, local dataset loading, a PostgreSQL-backed typeahead suggestion API, the React typeahead UI, Redis-backed suggestion caching with consistent hashing, an aggregated batch write pipeline for search submissions, recency-aware trending searches, and an assignment-friendly metrics summary API.

## Current Milestone
Milestone 11 focuses on:
- Java 21 + Spring Boot backend
- React + Vite + Tailwind frontend
- Docker Compose with PostgreSQL and Redis
- Flyway-based PostgreSQL schema setup
- Synthetic dataset generation for realistic search queries
- Local dataset loading into PostgreSQL
- `GET /suggest?q=<prefix>` backed by Redis cache with PostgreSQL fallback
- `POST /search` for fast enqueue plus eventual batched PostgreSQL updates
- `GET /trending` for recency-aware trending searches from PostgreSQL
- `GET /metrics/summary` for runtime plus durable summary metrics
- React UI for typing queries, viewing suggestions, and submitting searches
- `GET /cache/debug?prefix=<prefix>` for cache routing and hit or miss inspection
- scheduled aggregation into `search_queries`, `query_prefixes`, `query_activity_buckets`, and `batch_flush_audit`
- `GET /batch/debug` and `POST /batch/flush` for batch inspection and testing
- `scripts/perf-smoke.ps1` for lightweight performance evidence

Kafka, OpenSearch, Prometheus, and Grafana will be added in later milestones.

## Project Structure
```text
search-typeahead-system/
├── backend/
├── frontend/
├── data/
├── docs/
├── docker-compose.yml
├── README.md
└── .gitignore
```

## Local Setup
```bash
docker compose up -d
cd backend && ./mvnw spring-boot:run
cd frontend && npm install && npm run dev
```

The Docker Compose stack now starts:
- PostgreSQL on `localhost:55432`
- Redis on `localhost:6379`

## Database Schema
Flyway migration `V1__create_core_schema.sql` creates the initial PostgreSQL tables for:
- `search_queries`
- `query_prefixes`
- `query_activity_buckets`
- `batch_flush_audit`
- `processed_kafka_offsets`

Use these commands to verify the schema in PostgreSQL after the backend starts:

```bash
docker compose exec postgres psql -U typeahead -d typeahead -c "\dt"
docker compose exec postgres psql -U typeahead -d typeahead -c "\d search_queries"
```

## Dataset Generation
Generate the Milestone 3 synthetic dataset from the project root with:

```bash
python data/generate_dataset.py --rows 100000 --output data/queries.csv
```

## Dataset Loading
Load the generated dataset into `search_queries` and `query_prefixes` from the `backend` folder with:

```powershell
$env:JAVA_TOOL_OPTIONS="-Duser.timezone=UTC"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--app.dataset.load=true --app.dataset.path=../data/queries.csv"
```

## Verification Targets
- PostgreSQL runs on `localhost:55432`
- Redis runs on `localhost:6379`
- Backend usually runs on `http://localhost:8082`
- Frontend runs on `http://localhost:5173`
- `GET http://localhost:8082/health` returns:

```json
{"status":"UP","service":"search-typeahead-backend"}
```

## Suggest API
Milestone 10 still checks Redis before PostgreSQL for each normalized prefix. Cached entries use a TTL of 300 seconds and are routed onto logical cache nodes with consistent hashing.

Logical cache nodes:
- `cache-node-a`
- `cache-node-b`
- `cache-node-c`

Key formats:
- Cache key: `suggest:v1:count:<normalizedPrefix>`
- Redis key: `suggest:<ownerNode>:v1:count:<normalizedPrefix>`

Suggestion flow:
1. Normalize the incoming prefix.
2. Build `suggest:v1:count:<normalizedPrefix>`.
3. Use the consistent hash ring with virtual nodes to select the logical owner node.
4. Look up the owner-specific Redis key.
5. On a hit, return cached suggestions with `"source":"cache"`.
6. On a miss or Redis failure, fall back to PostgreSQL.
7. Cache the PostgreSQL result with TTL 300 seconds.

Use the backend suggestion endpoint after the dataset has already been loaded into PostgreSQL:

```bash
curl "http://localhost:8082/suggest?q=iph"
curl "http://localhost:8082/suggest?q=IPH"
curl "http://localhost:8082/suggest?q=spring%20boot"
curl "http://localhost:8082/suggest"
```

Example response:

```json
{
  "prefix": "iph",
  "count": 1,
  "suggestions": [
    {
      "query": "iphone",
      "count": 249943
    }
  ],
  "source": "postgres"
}
```

Edge-case behavior:
- Missing `q` returns HTTP 200 with empty suggestions.
- Empty or whitespace-only `q` returns HTTP 200 with empty suggestions.
- Mixed-case input is normalized before lookup.
- No-match prefixes return HTTP 200 with empty suggestions.
- Redis failures are logged and fall back to PostgreSQL instead of failing the request.

## Search API
Milestone 9 changes `POST /search` from an immediate database write into a fast enqueue operation. Each valid request is normalized exactly as before, stored in an in-memory queue, and returns `{"message":"Searched"}` immediately. A scheduled batch writer later drains queued events, aggregates repeated normalized queries, updates PostgreSQL once per unique query, refreshes prefixes, updates hourly activity buckets, records batch audits, and then invalidates affected Redis keys.

Why batching helps:
- repeated searches for the same normalized query collapse into one database update per flush
- API requests avoid waiting on PostgreSQL writes
- Redis invalidation happens once per unique updated query after the batch commit path

Eventual consistency:
- `/search` returns success after enqueue, not after PostgreSQL commit
- `/suggest` may show older counts until the next flush completes
- default flush interval is 5000 ms, so new counts usually appear within a few seconds

Batch settings:
- `app.batch.enabled=true`
- `app.batch.flush-interval-ms=5000`
- `app.batch.max-events=500`
- `app.batch.max-drain-events=5000`

Current tradeoff:
- the queue is in-memory and assignment-safe for local development
- queued searches would be lost if the backend process crashes before a flush
- a future Kafka milestone is the upgrade path for durable event buffering

Endpoint:

```text
POST /search
```

Request body example:

```json
{
  "query": "iphone"
}
```

Response example:

```json
{
  "message": "Searched"
}
```

PowerShell example:

```powershell
Invoke-RestMethod `
  -Method POST `
  -Uri "http://localhost:8082/search" `
  -ContentType "application/json" `
  -Body '{"query":"iphone"}'
```

Edge-case behavior:
- Missing request body returns HTTP 400.
- Missing `query` field returns HTTP 400.
- Empty query returns HTTP 400.
- Whitespace-only query returns HTTP 400.

## Batch Debug API
Use the batch debug endpoint to inspect the queue and flush settings.

Examples:

```bash
curl "http://localhost:8082/batch/debug"
curl -X POST "http://localhost:8082/batch/flush"
```

Example debug response:

```json
{
  "enabled": true,
  "queueSize": 0,
  "flushIntervalMs": 5000,
  "maxEvents": 500
}
```

Manual flush response example:

```json
{
  "status": "SUCCESS",
  "rawEventCount": 10,
  "uniqueQueryCount": 1,
  "dbWriteCount": 1
}
```

## Trending API
Milestone 11 keeps the PostgreSQL-backed trending endpoint that reads from `query_activity_buckets` and joins `search_queries` for display text and total counts. This is different from prefix suggestions because trending does not require a prefix and instead ranks the most recent search activity in a selected time window.

Endpoint:

```text
GET /trending
```

Query parameters:
- `window`: optional, one of `1h`, `6h`, `24h`, `7d`
- `limit`: optional, default `10`, max `20`

Behavior:
- default window is `24h`
- default limit is `10`
- if `limit` is greater than `20`, it is clamped to `20`
- if `limit` is less than `1`, the API returns HTTP `400`
- if `window` is invalid, the API returns HTTP `400`
- source is always `postgres` in this milestone

Ranking:
- sum `query_activity_buckets.search_count` within the selected window
- rank by `recentCount` descending
- tie-break by `totalCount` descending
- tie-break by query text ascending
- `score` currently equals `recentCount` and represents recent search activity

Examples:

```bash
curl "http://localhost:8082/trending"
curl "http://localhost:8082/trending?window=24h&limit=10"
curl "http://localhost:8082/trending?window=1h&limit=5"
```

Example response:

```json
{
  "window": "24h",
  "count": 1,
  "items": [
    {
      "query": "iphone",
      "totalCount": 249948,
      "recentCount": 620,
      "score": 620.0
    }
  ],
  "source": "postgres"
}
```

Empty-window response:

```json
{
  "window": "24h",
  "count": 0,
  "items": [],
  "source": "postgres"
}
```

## Metrics Summary API
Milestone 11 adds a lightweight metrics summary endpoint that combines in-memory runtime counters with durable PostgreSQL aggregate metrics.

Endpoint:

```text
GET /metrics/summary
```

Runtime counters:
- suggestion request count
- suggestion cache hits
- suggestion cache misses
- suggestion PostgreSQL reads
- accepted searches
- queued searches
- trending requests

Durable PostgreSQL metrics:
- total `search_queries` row count
- total `query_prefixes` row count
- total `query_activity_buckets` row count
- total `batch_flush_audit` row count
- successful batch flush count
- total raw events processed
- total unique query writes
- estimated DB writes avoided

Important behavior:
- runtime counters reset when the application restarts
- durable batch and database metrics come from PostgreSQL and survive restarts
- cache hit rate is numeric
- if there are zero suggestion requests, cache hit rate is `0.0`
- estimated DB writes avoided is `rawEventsProcessed - uniqueQueryWrites`

Example response:

```json
{
  "suggestions": {
    "requests": 4,
    "cacheHits": 2,
    "cacheMisses": 2,
    "cacheHitRate": 0.5,
    "postgresReads": 2
  },
  "searches": {
    "accepted": 10,
    "queued": 10
  },
  "batchWrites": {
    "flushes": 3,
    "rawEventsProcessed": 20,
    "uniqueQueryWrites": 5,
    "estimatedDbWritesAvoided": 15
  },
  "trending": {
    "requests": 2
  },
  "database": {
    "searchQueries": 100000,
    "queryPrefixes": 4369342,
    "activityBuckets": 3,
    "batchAuditRows": 3
  },
  "source": "application"
}
```

## Performance Evidence
Use the lightweight PowerShell helper for a quick smoke run against a locally running backend:

```powershell
.\scripts\perf-smoke.ps1
```

The script:
- warms `/suggest` for `iph`
- warms `/suggest` for `spring boot`
- submits a few `/search` requests
- calls `/batch/flush`
- calls `/trending`
- calls `/metrics/summary`
- prints simple timing output

## Cache Debug API
Use the cache debug endpoint to inspect routing, keys, and hit or miss status without changing suggestion behavior.

Examples:

```bash
curl "http://localhost:8082/cache/debug?prefix=iph"
curl "http://localhost:8082/cache/debug?prefix=iph&mode=count"
```

Example response:

```json
{
  "enabled": true,
  "prefix": "iph",
  "mode": "count",
  "cacheKey": "suggest:v1:count:iph",
  "redisKey": "suggest:cache-node-b:v1:count:iph",
  "ownerNode": "cache-node-b",
  "logicalNodes": ["cache-node-a", "cache-node-b", "cache-node-c"],
  "status": "HIT",
  "ttlSeconds": 300
}
```

Status values:
- `HIT` means the routed Redis key exists.
- `MISS` means the routed Redis key does not exist.
- `ERROR` means Redis is unavailable but the endpoint still returned a debug payload.

## Frontend Usage
The React frontend uses a Vite dev proxy, so the browser calls relative endpoints while Vite forwards `/suggest` and `/search` to `http://localhost:8082`.

1. Start the backend manually on port `8082`.
2. Start the frontend:

```bash
cd frontend
npm run dev
```

3. Open the Vite URL shown in the terminal, usually `http://localhost:5173`.
4. Type `iph` and verify suggestions appear.
5. Type `IPH` and verify suggestions still appear.
6. Type `spring boot` and verify suggestions appear.
7. Type `zzzzzz` and verify the no-results state appears.
8. Click a suggestion such as `iphone` and verify the UI shows `Searched`.
9. Press `Enter` on a query and verify the UI shows `Searched`.
10. Click the `Search` button and verify the UI shows `Searched`.
11. Search for `new ui test query`.
12. Type `new ui` and verify the new query appears in suggestions after refresh.

## Manual Verification
Run these commands after implementation:

```bash
docker compose up -d
cd backend
.\mvnw.cmd test
```

Then start the backend manually on port `8082` and verify:

1. `curl http://localhost:8082/health`
2. `curl "http://localhost:8082/batch/debug"`
3. Submit repeated searches for the same new query:

```powershell
1..10 | ForEach-Object {
  Invoke-RestMethod `
    -Method POST `
    -Uri "http://localhost:8082/search" `
    -ContentType "application/json" `
    -Body '{"query":"batch test iphone"}'
}
```

4. Wait 6 seconds for the scheduled flush, or call `curl -X POST "http://localhost:8082/batch/flush"`.
5. Call `curl "http://localhost:8082/suggest?q=batch%20test"` and confirm the suggestion list shows `batch test iphone` with a count around `10`.
6. Check batch audits:

```bash
docker compose exec postgres psql -U typeahead -d typeahead -c "SELECT batch_id, raw_event_count, unique_query_count, db_write_count, status FROM batch_flush_audit ORDER BY finished_at DESC LIMIT 5;"
```

7. Check hourly activity buckets:

```bash
docker compose exec postgres psql -U typeahead -d typeahead -c "SELECT query_id, bucket_start, bucket_granularity, search_count FROM query_activity_buckets ORDER BY id DESC LIMIT 5;"
```

8. Verify cache behavior still works for an existing prefix:

```bash
curl "http://localhost:8082/cache/debug?prefix=iph"
curl "http://localhost:8082/suggest?q=iph"
curl "http://localhost:8082/suggest?q=iph"
```

9. Confirm the first `iph` request is a PostgreSQL miss or load and the second request returns `"source":"cache"`.
10. Check Redis for the routed key:

```bash
docker exec -it typeahead-redis redis-cli KEYS "suggest:*:v1:count:iph"
docker exec -it typeahead-redis redis-cli TTL "suggest:<ownerNode>:v1:count:iph"
```

11. Verify trending with a fresh query:

```powershell
1..5 | ForEach-Object {
  Invoke-RestMethod `
    -Method POST `
    -Uri "http://localhost:8082/search" `
    -ContentType "application/json" `
    -Body '{"query":"fresh trending query"}'
}
Invoke-RestMethod -Method POST -Uri "http://localhost:8082/batch/flush"
Invoke-RestMethod -Uri "http://localhost:8082/trending"
Invoke-RestMethod -Uri "http://localhost:8082/trending?window=24h&limit=10"
```

12. Confirm `fresh trending query` appears with `recentCount` matching the flushed count and that `/suggest?q=iph` still returns postgres first and cache second.
13. Verify metrics summary:

```powershell
Invoke-RestMethod -Uri "http://localhost:8082/metrics/summary"
.\scripts\perf-smoke.ps1
```

14. Confirm:
- suggestion requests increased
- cache hits are greater than 0
- cache misses are greater than 0
- cache hit rate is numeric
- batch raw events processed increased
- unique query writes increased
- estimated DB writes avoided is visible
- database table counts are visible
