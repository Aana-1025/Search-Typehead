# Design Tradeoffs

## PostgreSQL Prefix Table
Using `query_prefixes` is simple and assignment-safe:
- prefix lookups are straightforward SQL reads
- the backend does not need a separate search engine
- the system stays easy to run locally

Tradeoff:
- storage is heavy because each query expands into many prefix rows

## Redis Cache
Redis improves repeated prefix read latency:
- hot prefixes avoid PostgreSQL on repeated reads
- the cache keeps the read path responsive for common suggestions

Tradeoff:
- cached suggestion data can be briefly stale until invalidation happens after a successful batch write

## Consistent Hashing with Logical Nodes
The project uses logical cache nodes to demonstrate consistent hashing concepts:
- routing behavior is visible and explainable
- cache ownership can be inspected through `/cache/debug`

Tradeoff:
- this is a demo-friendly abstraction, not a full Redis Cluster deployment

## In-Memory Queue
An in-memory queue is simpler than Kafka for this assignment:
- easy to implement
- easy to test locally
- enough to demonstrate asynchronous writes and eventual consistency

Tradeoff:
- queued events can be lost if the backend crashes before the next flush

## Batch Writes
Batching reduces database pressure:
- repeated search events are aggregated
- durable writes are fewer than raw incoming search requests
- prefix refresh and cache invalidation are coordinated after the batch write

Tradeoff:
- updates are eventually consistent rather than immediate
- the UI may briefly show old counts until the next flush completes

## Future Production Improvements
The following are intentionally not part of the current implementation:
- OpenSearch for scalable full-text and ranking features
- Kafka for durable event ingestion
- Redis Cluster for real distributed caching
- Prometheus for time-series monitoring
- Grafana for dashboards and alert visualization

These are future production improvements, not current assignment dependencies.
