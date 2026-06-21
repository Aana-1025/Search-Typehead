# Search Typeahead System

## Overview
This repository contains Milestone 1 of a high-level design assignment project for a Search Typeahead System. The current scope sets up the project skeleton, local development workflow, PostgreSQL infrastructure, and a minimal backend health endpoint for verification.

## Current Milestone
Milestone 1 focuses on:
- Java 21 + Spring Boot backend
- React + Vite + Tailwind frontend
- Docker Compose with PostgreSQL only
- Initial project documentation and directory structure

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

## Verification Targets
- PostgreSQL runs on `localhost:5432`
- Backend runs on `http://localhost:8080`
- Frontend runs on `http://localhost:5173`
- `GET http://localhost:8080/health` returns:

```json
{"status":"UP","service":"search-typeahead-backend"}
```
