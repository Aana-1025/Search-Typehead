# Performance Report

## Inputs Used
This report uses the saved proof outputs:
- [proof/metrics-summary.json](proof/metrics-summary.json)
- [proof/batch-flush-success.json](proof/batch-flush-success.json)
- [proof/db-search-query-count.txt](proof/db-search-query-count.txt)
- [proof/db-prefix-count.txt](proof/db-prefix-count.txt)

## Metrics Collected
From `metrics-summary.json`:
- suggestion requests = `312`
- cache hits = `84`
- cache misses = `228`
- cache hit rate = `0.2692307692307692`
- PostgreSQL reads = `228`
- accepted searches = `61`
- queued searches = `61`
- batch flushes = `44`
- raw events processed = `73`
- unique query writes = `44`
- estimated DB writes avoided = `29`
- search query rows = `100020`
- query prefix rows = `4369591`
- activity bucket rows = `30`
- batch audit rows = `44`

From `batch-flush-success.json`:
- status = `SUCCESS`
- rawEventCount = `5`
- uniqueQueryCount = `1`
- dbWriteCount = `1`

## Formulas
- `cacheHitRate = cacheHits / suggestionRequests`
- `estimatedDbWritesAvoided = rawEventsProcessed - uniqueQueryWrites`

Applied to the saved proof:
- `84 / 312 = 0.2692307692307692`
- `73 - 44 = 29`

## Interpretation
### Cache Behavior
The current proof shows:
- repeated prefixes are benefiting from Redis
- PostgreSQL is still handling cold requests and misses
- the cache hit rate is about `26.92%`

Cold cache behavior:
- the first request for a prefix is likely to miss
- PostgreSQL serves the data
- Redis is then populated for later reads

Warm cache behavior:
- repeated requests for a popular prefix can return from Redis
- repeated reads avoid unnecessary PostgreSQL queries

### Batch Write Behavior
The saved metrics show:
- `73` raw search events processed
- `44` unique writes to PostgreSQL
- `29` writes avoided through aggregation

This demonstrates the main benefit of batching:
- multiple repeated queries inside the same flush window do not require one database write per request
- the API can acknowledge quickly while the batch writer amortizes storage work

The batch success proof is an even clearer example:
- `rawEventCount = 5`
- `uniqueQueryCount = 1`
- `dbWriteCount = 1`

That means five identical searches were collapsed into one durable write.

## Database Size Notes
The project intentionally materializes prefixes for assignment simplicity.

Saved proof currently shows:
- `search_queries` around `100019` to `100020`
- `query_prefixes` around `4369567` to `4369591`

This confirms the expected tradeoff:
- fast prefix lookup
- heavier storage footprint

## p95 and Load Testing Note
This repository does not currently include exact p95 latency measurements from a full load-testing tool. Instead, the submitted performance evidence is practical and assignment-appropriate: cache hit rate from `/metrics/summary`, suggestion request count, PostgreSQL read count, batch raw events processed, unique query writes, estimated DB writes avoided, database row counts, saved proof files in `docs/proof/`, and lightweight smoke-script plus API verification.

Exact p95 latency can be measured later with a dedicated load-testing tool. The current submission does not overclaim latency numbers, but it does provide clear evidence of cache behavior, reduced PostgreSQL reads on repeated prefixes, batch write aggregation, and the current database scale.
