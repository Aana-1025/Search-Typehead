# Search Typeahead System

## Overview
This repository contains Milestone 8 of a high-level design assignment project for a Search Typeahead System. The current scope includes the initial project skeleton, local development workflow, PostgreSQL infrastructure, Flyway-managed schema setup, synthetic dataset generation, local dataset loading, a PostgreSQL-backed typeahead suggestion API, direct search submission count updates, the React typeahead UI, and Redis-backed suggestion caching with consistent hashing.

## Current Milestone
Milestone 8 focuses on:
- Java 21 + Spring Boot backend
- React + Vite + Tailwind frontend
- Docker Compose with PostgreSQL only
- Flyway-based PostgreSQL schema setup
- Synthetic dataset generation for realistic search queries
- Local dataset loading into PostgreSQL
- `GET /suggest?q=<prefix>` backed by Redis cache with PostgreSQL fallback
- `POST /search` for direct PostgreSQL count updates, per-query prefix refresh, and targeted cache invalidation
- React UI for typing queries, viewing suggestions, and submitting searches
- `GET /cache/debug?prefix=<prefix>` for cache routing and hit or miss inspection

Redis/cache, Kafka, OpenSearch, batch writes, trending features, and metrics APIs will be added in later milestones.

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
Milestone 8 checks Redis before PostgreSQL for each normalized prefix. Cached entries use a TTL of 300 seconds and are routed onto logical cache nodes with consistent hashing.

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
Milestone 8 keeps the direct PostgreSQL-backed search submission flow from Milestone 6. Each valid `POST /search` request updates durable counts in `search_queries`, refreshes `query_prefixes` for the affected query, and then invalidates cached suggestion keys for the query's generated prefixes. Redis invalidation failures are logged and do not fail the search request.

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
docker compose ps
cd backend
.\mvnw.cmd test
```

Then start the backend manually on port `8082` and verify:

1. `curl http://localhost:8082/health`
2. `curl "http://localhost:8082/cache/debug?prefix=iph"`
3. `curl "http://localhost:8082/suggest?q=iph"`
4. `curl "http://localhost:8082/suggest?q=iph"`
5. Confirm the first request is a PostgreSQL miss or load and the second request returns `"source":"cache"`.
6. Check Redis for the routed key:

```bash
docker exec -it typeahead-redis redis-cli KEYS "suggest:*:v1:count:iph"
docker exec -it typeahead-redis redis-cli TTL "suggest:<ownerNode>:v1:count:iph"
```

7. Submit a search:

```bash
curl -X POST "http://localhost:8082/search" \
  -H "Content-Type: application/json" \
  -d '{"query":"iphone"}'
```

8. Call `curl "http://localhost:8082/suggest?q=iph"` again and confirm cache invalidation plus refresh behavior works.
