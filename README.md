# Search Typeahead System

## Overview
This repository contains Milestone 5 of a high-level design assignment project for a Search Typeahead System. The current scope includes the initial project skeleton, local development workflow, PostgreSQL infrastructure, Flyway-managed schema setup, synthetic dataset generation, local dataset loading, and the first PostgreSQL-backed typeahead suggestion API.

## Current Milestone
Milestone 5 focuses on:
- Java 21 + Spring Boot backend
- React + Vite + Tailwind frontend
- Docker Compose with PostgreSQL only
- Flyway-based PostgreSQL schema setup
- Synthetic dataset generation for realistic search queries
- Local dataset loading into PostgreSQL
- `GET /suggest?q=<prefix>` backed by PostgreSQL `query_prefixes`

Redis, Kafka, OpenSearch, `/search`, batch writes, trending features, and metrics APIs will be added in later milestones.

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
- Backend runs on `http://localhost:8080`
- Frontend runs on `http://localhost:5173`
- `GET http://localhost:8080/health` returns:

```json
{"status":"UP","service":"search-typeahead-backend"}
```

## Suggest API
Use the backend suggestion endpoint after the dataset has already been loaded into PostgreSQL:

```bash
curl "http://localhost:8080/suggest?q=iph"
curl "http://localhost:8080/suggest?q=IPH"
curl "http://localhost:8080/suggest?q=spring%20boot"
curl "http://localhost:8080/suggest"
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
