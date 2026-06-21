# Search Typeahead System

## Overview
This repository contains Milestone 3 of a high-level design assignment project for a Search Typeahead System. The current scope includes the initial project skeleton, local development workflow, PostgreSQL infrastructure, a minimal backend health endpoint, the first Flyway-managed database schema, and a synthetic dataset generator for search-query testing.

## Current Milestone
Milestone 3 focuses on:
- Java 21 + Spring Boot backend
- React + Vite + Tailwind frontend
- Docker Compose with PostgreSQL only
- Flyway-based PostgreSQL schema setup
- Synthetic dataset generation for realistic search queries

Redis, Kafka, OpenSearch, APIs beyond the basic health check, batch writes, and trending features will be added in later milestones.

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

## Verification Targets
- PostgreSQL runs on `localhost:55432`
- Backend runs on `http://localhost:8080`
- Frontend runs on `http://localhost:5173`
- `GET http://localhost:8080/health` returns:

```json
{"status":"UP","service":"search-typeahead-backend"}
```
