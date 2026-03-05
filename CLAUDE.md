# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Campus Club Management and Recruitment System — a Spring Boot 3.x multi-module Maven monorepo with a Vue 3 frontend and a Python AI agent service.

## Build & Run Commands

### Backend (Java)

```bash
# Build all modules
mvn clean package

# Build a specific module
mvn clean package -pl community-gateway

# Run the main application (entry point is community-gateway)
mvn spring-boot:run -pl community-gateway

# Run all tests
mvn test

# Run tests for a specific module
mvn test -pl community-gateway
```

Main application class: `com.cloud.community.gateway.CommunityApplication`
Server runs on port **8080**.

### Frontend (Vue 3 + Vite)

```bash
cd frontend
npm install
npm run dev      # Dev server
npm run build    # Production build
```

Node requirement: `^20.19.0` or `>=22.12.0`

### Python Agent Service

```bash
cd agent
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8000
```

Or via Docker:

```bash
docker build -t community-agent ./agent
docker run -p 8000:8000 -e DEEPSEEK_API_KEY=... -e DB_URL=... community-agent
```

### Infrastructure (Docker Compose)

```bash
# Start Redis, RabbitMQ, Prometheus, Grafana, and the agent
docker-compose up -d
```

Services: Redis (6379), RabbitMQ (5672 / management 15672), Prometheus (9090), Grafana (3000, admin/admin), Agent (8000).

## Architecture

### Module Responsibilities

| Module | Role |
|---|---|
| `community-core` | Shared library: JPA entities, repositories, JWT security, OSS integration, email/verification services |
| `community-gateway` | Application entry point, Spring Security config, JWT filter, all REST controllers |
| `community-club` | Club CRUD, approval/freeze workflow, member management |
| `community-user` | User registration, profile, authentication |
| `community-admin` | Admin-level system management and auditing |
| `community-activity` | Activity lifecycle: creation, signup, attendance, archiving |
| `community-recruit` | Recruitment batches, application forms, multi-stage review |
| `community-notice` | Notice publishing and read-tracking |

All business modules are dependencies of `community-gateway`, which is the single deployable JAR.

### Key Infrastructure

- **MySQL 8** (`community_db`) — primary data store; schema initialized via `community-gateway/src/main/resources/schema.sql`
- **Redis** — distributed caching and session/token storage
- **RabbitMQ** — async event messaging between modules
- **Aliyun OSS** — file/image storage
- **DeepSeek API** — LLM for the AI agent

### Python Agent (`/agent`)

Three endpoints exposed on port 8000:

- `POST /chat` — RAG Q&A using LangChain + Chroma vector DB + DeepSeek
- `POST /recommend` — Hybrid club recommendations (SVD collaborative filtering + semantic content-based)
- `POST /sync` — Sync knowledge base from the database

Required env vars: `DEEPSEEK_API_KEY`, `DB_URL`.

### Frontend (`/frontend`)

Vue 3 SPA using Element Plus, Pinia, Vue Router, Axios, ECharts, and STOMP/SockJS for WebSocket. API calls proxy to the backend at port 8080.

### Monitoring

Prometheus scrapes `/actuator/prometheus` every 15 s. Grafana connects to Prometheus for dashboards.

## Configuration

Primary config: `community-gateway/src/main/resources/application.properties`

Key properties to set for local development:

```
spring.datasource.url=jdbc:mysql://localhost:3306/community_db
spring.data.redis.host=localhost
spring.rabbitmq.host=localhost
aliyun.oss.*          # OSS bucket credentials
spring.mail.*         # SMTP credentials
deepseek.api.key=...  # DeepSeek API key
```

Database seed data: `scripts/data.sql` (full dataset) or generate mock data with `scripts/generate_mock_data.py`.
