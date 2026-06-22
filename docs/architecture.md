# Architecture

## Overview
The Search Typeahead System is built as a layered local assignment implementation:
- React + Vite frontend
- Spring Boot backend
- Redis cache for repeated prefix lookups
- PostgreSQL for durable query, prefix, trending, and audit storage
- In-memory queue plus batch writer for eventual count updates

## Text Diagram
```text
React UI
  -> GET /suggest?q=prefix
     -> Spring Boot
        -> Redis routed lookup
           -> PostgreSQL query_prefixes fallback

React UI
  -> POST /search
     -> Spring Boot
        -> in-memory queue
           -> scheduled/manual batch writer
              -> search_queries
              -> query_prefixes
              -> query_activity_buckets
              -> batch_flush_audit
              -> Redis invalidation

React UI
  -> GET /trending
     -> Spring Boot
        -> PostgreSQL query_activity_buckets + search_queries

React UI
  -> GET /metrics/summary
     -> Spring Boot
        -> in-memory counters + PostgreSQL aggregates
```

## Read Path
1. The frontend sends `GET /suggest?q=<prefix>`.
2. The backend trims and normalizes the prefix.
3. A logical cache owner is chosen using consistent hashing.
4. Redis is checked for the routed suggestion key.
5. On a hit, the backend returns the cached suggestions with `source=cache`.
6. On a miss, PostgreSQL `query_prefixes` is queried.
7. The PostgreSQL result is returned and cached with TTL.

## Write Path
1. The frontend sends `POST /search` with a query string.
2. The backend validates and normalizes the query.
3. The request is accepted immediately and placed into the in-memory queue.
4. The API returns `{ "message": "Searched" }`.

This makes the write path fast for the user, but the durable update is eventually consistent rather than immediate.

## Cache Path
- Redis stores routed prefix results.
- Logical nodes are used to demonstrate consistent hashing:
  - `cache-node-a`
  - `cache-node-b`
  - `cache-node-c`
- The debug endpoint `/cache/debug?prefix=iph` shows the routed owner node, Redis key, TTL, and hit/miss status.

The saved proof file [proof/cache-debug-iph.json](proof/cache-debug-iph.json) currently shows:
- `ownerNode = cache-node-b`
- `status = MISS`
- `ttlSeconds = 300`

## Batch Path
The in-memory queue is drained by a scheduled/manual batch writer.

During a successful flush:
1. Events are drained from the queue.
2. Repeated normalized queries are aggregated.
3. `search_queries` counts are updated once per unique query.
4. `query_prefixes` rows are refreshed for affected queries.
5. `query_activity_buckets` records recent activity.
6. A `batch_flush_audit` row is inserted for successful non-empty flushes.
7. Redis keys for affected prefixes are invalidated.

The saved flush proof in [proof/batch-flush-success.json](proof/batch-flush-success.json) shows:
- `status = SUCCESS`
- `rawEventCount = 5`
- `uniqueQueryCount = 1`
- `dbWriteCount = 1`

## Trending Path
`GET /trending` uses `query_activity_buckets` to compute recent activity for a selected window. It joins `search_queries` to return:
- query text
- total durable count
- recent count
- score

The saved proof in [proof/trending-output.json](proof/trending-output.json) shows the top 5 queries for the `24h` window.

## Metrics Path
`GET /metrics/summary` combines:
- in-memory counters:
  - suggestion requests
  - cache hits
  - cache misses
  - PostgreSQL reads
  - accepted/queued searches
  - trending requests
- durable PostgreSQL aggregates:
  - `search_queries` row count
  - `query_prefixes` row count
  - `query_activity_buckets` row count
  - `batch_flush_audit` row count
  - successful flush count
  - raw events processed
  - unique query writes
  - estimated DB writes avoided

## Why This Design Fits the Assignment
- PostgreSQL prefix tables are simple and easy to explain.
- Redis demonstrates practical cache acceleration.
- Batch writes show throughput-oriented design and eventual consistency.
- Trending demonstrates windowed aggregation over recent activity.
- The system is small enough to run locally but still covers core HLD concepts.
