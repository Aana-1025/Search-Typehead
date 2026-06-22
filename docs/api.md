# API Documentation

## GET /health
Purpose: basic service health check.

Example request:
```http
GET /health
```

Example response:
```json
{
  "status": "UP",
  "service": "search-typeahead-backend"
}
```

## GET /suggest?q=iph
Purpose: return up to 10 suggestions that begin with the normalized prefix.

Example request:
```http
GET /suggest?q=iph
```

Example response:
```json
{
  "prefix": "iph",
  "count": 1,
  "suggestions": [
    {
      "query": "iphone",
      "count": 249955
    }
  ],
  "source": "postgres"
}
```

Notes:
- Mixed-case prefixes are normalized.
- Empty or missing `q` returns an empty suggestion list.
- Results are limited to 10 suggestions.

## POST /search
Purpose: accept a search query and enqueue it for batch processing.

Example request:
```http
POST /search
Content-Type: application/json
```

```json
{
  "query": "iphone"
}
```

Example response:
```json
{
  "message": "Searched"
}
```

Notes:
- This endpoint acknowledges enqueue, not immediate PostgreSQL commit.
- Count and trending updates are eventually visible after the next flush.

## GET /cache/debug?prefix=iph
Purpose: inspect Redis routing, logical node ownership, and hit/miss state for a prefix.

Example request:
```http
GET /cache/debug?prefix=iph
```

Example response from [proof/cache-debug-iph.json](proof/cache-debug-iph.json):
```json
{
  "enabled": true,
  "prefix": "iph",
  "mode": "count",
  "cacheKey": "suggest:v1:count:iph",
  "redisKey": "suggest:cache-node-b:v1:count:iph",
  "ownerNode": "cache-node-b",
  "logicalNodes": [
    "cache-node-a",
    "cache-node-b",
    "cache-node-c"
  ],
  "status": "MISS",
  "ttlSeconds": 300
}
```

## GET /batch/debug
Purpose: inspect current queue size and batch configuration.

Example request:
```http
GET /batch/debug
```

Example response:
```json
{
  "enabled": true,
  "queueSize": 0,
  "flushIntervalMs": 5000,
  "maxEvents": 500
}
```

## POST /batch/flush
Purpose: manually flush queued search events.

Example request:
```http
POST /batch/flush
```

Example response from [proof/batch-flush-success.json](proof/batch-flush-success.json):
```json
{
  "status": "SUCCESS",
  "rawEventCount": 5,
  "uniqueQueryCount": 1,
  "dbWriteCount": 1
}
```

## GET /trending?window=24h&limit=5
Purpose: return top recent searches for a selected time window.

Example request:
```http
GET /trending?window=24h&limit=5
```

Example response from [proof/trending-output.json](proof/trending-output.json):
```json
{
  "window": "24h",
  "count": 5,
  "items": [
    {
      "query": "batch test iphone",
      "totalCount": 620,
      "recentCount": 620,
      "score": 620.0
    },
    {
      "query": "metrics test iphone",
      "totalCount": 10,
      "recentCount": 10,
      "score": 10.0
    },
    {
      "query": "iphone",
      "totalCount": 249955,
      "recentCount": 7,
      "score": 7.0
    },
    {
      "query": "trending iphone test",
      "totalCount": 7,
      "recentCount": 7,
      "score": 7.0
    },
    {
      "query": "batch audit test iphone",
      "totalCount": 5,
      "recentCount": 5,
      "score": 5.0
    }
  ],
  "source": "postgres"
}
```

## GET /metrics/summary
Purpose: summarize runtime counters and durable PostgreSQL aggregates.

Example request:
```http
GET /metrics/summary
```

Example response from [proof/metrics-summary.json](proof/metrics-summary.json):
```json
{
  "suggestions": {
    "requests": 312,
    "cacheHits": 84,
    "cacheMisses": 228,
    "cacheHitRate": 0.2692307692307692,
    "postgresReads": 228
  },
  "searches": {
    "accepted": 61,
    "queued": 61
  },
  "batchWrites": {
    "flushes": 44,
    "rawEventsProcessed": 73,
    "uniqueQueryWrites": 44,
    "estimatedDbWritesAvoided": 29
  },
  "trending": {
    "requests": 129
  },
  "database": {
    "searchQueries": 100020,
    "queryPrefixes": 4369591,
    "activityBuckets": 30,
    "batchAuditRows": 44
  },
  "source": "application"
}
```
