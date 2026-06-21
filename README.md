# Search Typeahead System

## Overview
This repository contains Milestone 7 of a high-level design assignment project for a Search Typeahead System. The current scope includes the initial project skeleton, local development workflow, PostgreSQL infrastructure, Flyway-managed schema setup, synthetic dataset generation, local dataset loading, a PostgreSQL-backed typeahead suggestion API, direct search submission count updates, and the first working React typeahead UI.

## Current Milestone
Milestone 7 focuses on:
- Java 21 + Spring Boot backend
- React + Vite + Tailwind frontend
- Docker Compose with PostgreSQL only
- Flyway-based PostgreSQL schema setup
- Synthetic dataset generation for realistic search queries
- Local dataset loading into PostgreSQL
- `GET /suggest?q=<prefix>` backed by PostgreSQL `query_prefixes`
- `POST /search` for direct PostgreSQL count updates and per-query prefix refresh
- React UI for typing queries, viewing suggestions, and submitting searches

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
- Backend usually runs on `http://localhost:8082`
- Frontend runs on `http://localhost:5173`
- `GET http://localhost:8082/health` returns:

```json
{"status":"UP","service":"search-typeahead-backend"}
```

## Suggest API
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

## Search API
Milestone 6 adds direct PostgreSQL-backed search submission updates. Each valid `POST /search` request updates durable counts in `search_queries` and refreshes `query_prefixes` only for the affected query. Redis/cache, batch writes, and trending logic are not added yet.

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
