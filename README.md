# CAMS - Career Activity Management System

CAMS is a full-stack, two-sided job board. Candidates build a profile and browse
and apply to openings; companies register, post job listings, and review the
people who apply. On top of the usual browse-and-filter search, CAMS ranks
candidates and jobs against each other by **meaning** rather than keywords, using
text embeddings and vector similarity search. It also keeps the original
single-user application tracker the project grew out of, so every candidate has a
private record of everything they have applied to.

The stack is a Spring Boot REST API, a React single-page frontend, PostgreSQL
with the `pgvector` extension for similarity search, and a small Python service
that turns text into embeddings. Everything runs together with one Docker Compose
command.

---

## What It Does

### For candidates
- Register and sign in; every candidate automatically gets a profile.
- Fill in a profile: headline, free-text summary, location, remote/hours
  preferences, skills (from a controlled ~5,000-entry vocabulary), work
  experience, education, and courses.
- Browse open job listings across the whole platform, with optional filters for
  location, remote/on-site, seniority level, company, and required skills.
- See a personal **"top matches"** list - open jobs ranked by how well they fit
  the candidate's profile, not just whether they contain a search term.
- Apply to a listing in one click. Applying also creates an entry in the
  candidate's personal application tracker.
- Track every application (job-board and manually-added) by status, source, and
  date, with a statistics dashboard.

### For employers
- Register a company. New employer accounts stay pending until an administrator
  approves them; rejected applicants can resubmit.
- Post, edit, and archive job listings, each with its own location, remote flag,
  seniority level, and required skills.
- Review applicants for a listing on one screen: name, headline, selected
  skills, and a **match score** for each applicant, plus soft warnings when a
  candidate's location or remote preference looks like a mismatch.
- Open any applicant's full profile and accept or reject their application.
  A decision syncs back to the candidate's own tracker automatically.

### For administrators
- Approve or reject employer registration requests.
- Promote users and trigger a re-embedding backfill after changes to how
  embeddings are built.

---

## How The Matching Works

Plain-language version: when a candidate saves their profile or a company saves a
listing, CAMS sends the relevant text to a small machine-learning service that
returns a list of 384 numbers - an **embedding** - that represents the meaning of
that text. Two pieces of text with similar meaning get embeddings that sit close
together. PostgreSQL (via the `pgvector` extension) stores these vectors and can
sort listings by distance to a candidate's vector directly in a database query,
so "find the closest jobs to this person" is a single ordered `SELECT`.

Technical version:
- The embedding model is `sentence-transformers/all-MiniLM-L6-v2`, served over
  HTTP by a FastAPI app. It produces 384-dimensional vectors.
- Embeddings are computed **asynchronously, after the triggering database
  transaction commits** - never inline with the user's request. A failed
  embedding call is logged and retried later, and never blocks a save.
- Candidate and listing vectors are stored in `vector(384)` columns on
  `candidate_profiles` and `job_listings`.
- The candidate-facing match query orders open listings by cosine distance to
  the candidate's vector, excludes listings they have already applied to, and
  applies a coarse location eligibility check (remote, or a location-text
  overlap). The employer-facing applicant view computes a cosine similarity per
  applicant for display.
- A separate offline evaluation harness (`scripts/matching-lab/`) compares
  several scoring strategies against a hand-graded benchmark set; it is research
  tooling and is not part of the running application.

---

## Tech Stack

All versions below are the ones currently pinned in `pom.xml`,
`frontend/package.json`, `embedding-service/`, and the Compose files.

### Backend API

| Technology | Version | Role |
|---|---|---|
| Java | 25 | Language |
| Spring Boot | 4.0.6 | Application framework (Web MVC, Data JPA, Security, Validation) |
| Hibernate ORM | 7.2.12 | JPA provider |
| `hibernate-vector` | 7.2.12 | Maps `pgvector` columns to `float[]` |
| Spring Security | Boot-managed | Authentication and authorization |
| JJWT (`jjwt-api`/`impl`/`jackson`) | 0.12.6 | JWT creation and verification |
| Flyway | Boot-managed (`flyway-database-postgresql`) | Versioned schema migrations |
| SpringDoc OpenAPI | 2.8.8 | Swagger UI / OpenAPI 3 docs |
| AWS SDK for Java - S3 | 2.46.7 | S3-compatible object storage client (pictures, logos) |
| Maven | 3.9.15 (via `./mvnw`) | Build |

### Frontend

| Technology | Version | Role |
|---|---|---|
| React | 19.2 | UI library |
| Vite | 8.0 | Build tool and dev server |
| Tailwind CSS | 4.3 | Styling |
| React Router | 7.17 | Client-side routing |
| Axios | 1.17 | HTTP client, with a JWT bearer interceptor |
| Recharts | 3.8 | Dashboard charts |
| react-datepicker | 9.1 | Date inputs |

### Embedding service

| Technology | Version | Role |
|---|---|---|
| Python | 3.13 | Runtime |
| FastAPI | 0.141.1 | HTTP API (`POST /embed`, `GET /health`) |
| sentence-transformers | 5.7.0 | Loads and runs the embedding model |
| Model | `all-MiniLM-L6-v2` | 384-dimensional sentence embeddings |
| PyTorch | 2.13.0 (CPU build) | Model backend |

### Data and infrastructure

| Technology | Version | Role |
|---|---|---|
| PostgreSQL | 18 | Relational database |
| pgvector | `pgvector/pgvector:pg18` image | Vector column type and similarity search |
| MinIO | `RELEASE.2025-09-07` | S3-compatible object storage (local development) |
| Nginx | `nginx:alpine` | Serves the built frontend, reverse-proxies `/api` |
| Docker Compose | v2 | Local orchestration |

---

## Architecture

```
                          ┌──────────────┐
        browser  ───────▶ │    Nginx     │  serves the React build,
                          │  (frontend)  │  proxies /api/* to the backend
                          └──────┬───────┘
                                 │ /api/*
                          ┌──────▼───────┐        ┌───────────────────┐
                          │ Spring Boot  │ ─────▶ │ embedding-service │
                          │   REST API   │  HTTP  │  (FastAPI + model)│
                          └──────┬───────┘        └───────────────────┘
                                 │ JDBC
                    ┌────────────▼────────────┐   ┌───────────────┐
                    │  PostgreSQL + pgvector  │   │     MinIO     │
                    │  (relational + vectors) │   │ (S3 storage)  │
                    └─────────────────────────┘   └───────────────┘
```

- **Frontend** is a static React bundle. In the Docker setups it is served by
  Nginx, which also reverse-proxies `/api/*` to the backend so the browser only
  ever talks to one origin; the backend is not exposed to the browser directly.
  (For frontend development, the Vite dev server runs it directly and calls the
  backend at `VITE_API_URL`.)
- **Backend** is a stateless REST API. Authentication is a signed JWT in the
  `Authorization: Bearer` header, validated on every request - there are no
  server-side sessions. The database schema is owned by Flyway migrations;
  Hibernate runs in `validate` mode and never alters the schema.
- **Embedding service** is stateless and single-purpose: text in, vector out.
  The model weights are baked into its image at build time.
- **PostgreSQL** stores both ordinary relational data and the embedding vectors.
  The `pgvector` extension is enabled by the first embedding migration.
- **MinIO** (locally) or any S3-compatible bucket (in production) holds uploaded
  profile pictures and company logos. The backend writes to it via the AWS S3
  SDK using an endpoint override, so the same code works against MinIO, AWS S3,
  or Cloudflare R2.

### Backend package layout (`src/main/java/cams/`)

| Package | Contents |
|---|---|
| `config/` | CORS, OpenAPI, admin-bootstrap runner |
| `controller/` | REST endpoints (auth, candidacies, job listings, candidate profiles, companies, employer requests, applications, skills, users, admin) |
| `service/` | Business logic and authorization; all database writes go through here |
| `repository/` | Spring Data JPA repositories, including the vector-ordered match query |
| `entity/` | JPA entities and enums |
| `dto/` | Separate request and response DTOs - entities are never used as request/response bodies |
| `mapper/` | The only place entity ↔ DTO translation happens |
| `security/` | JWT utilities, the auth filter, user-details service, security config |
| `client/` | `EmbeddingClient` - HTTP client for the embedding service |
| `event/` | Domain events that trigger asynchronous re-embedding |
| `exception/` | Custom exceptions and a single `@RestControllerAdvice` handler |

### Frontend layout (`frontend/src/`)

| Directory | Contents |
|---|---|
| `api/` | Axios instance with the JWT interceptor, plus per-resource API modules |
| `context/` | `AuthContext`, persisted to `localStorage` |
| `components/` | Navbar, route guards, and shared UI primitives |
| `pages/` | Landing, auth, candidate profile, job browse, matches, listing detail, employer listings and applicants, admin, dashboard |
| `utils/` | Formatting helpers and the client-side dashboard stats function |

---

## Running Locally With Docker Compose

**Prerequisites:** Docker Desktop (or Docker Engine + Compose v2).

### 1. Clone

```bash
git clone https://github.com/LyuboPetkov/cams-platform.git
cd cams-platform
```

### 2. Create a `.env` file in the repository root

```dotenv
DB_PASSWORD=choose_a_password
JWT_SECRET=base64_encoded_256_bit_key
STORAGE_ACCESS_KEY=minioadmin
STORAGE_SECRET_KEY=minioadmin
```

`JWT_SECRET` must be a base64-encoded 256-bit key. One way to generate one:

```bash
openssl rand -base64 32
```

### 3. Start the stack

```bash
docker compose -f docker-compose.local.yml up --build
```

This builds and starts five containers: PostgreSQL/pgvector, the embedding
service, MinIO, the backend, and the Nginx-served frontend. The first build
downloads the embedding model weights, so it needs network access and takes a
few minutes. Flyway runs all migrations automatically on backend startup.

| Service | URL |
|---|---|
| Frontend | http://localhost |
| Backend API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| MinIO console | http://localhost:9000 |

### 4. Stop

```bash
docker compose -f docker-compose.local.yml down
```

Add `-v` to also drop the database and object-storage volumes.

---

## Running Without Docker

Useful for backend or frontend development in isolation. You still need
PostgreSQL with `pgvector` and the embedding service running (the embedding
service is easiest to keep in Docker: `docker compose -f docker-compose.local.yml up embedding-service`).

**Prerequisites:** Java 25, Node.js 22+, PostgreSQL 18 with the `vector`
extension available.

### Database

```sql
CREATE DATABASE cams_db;
CREATE USER cams_user WITH PASSWORD 'your_password';
ALTER DATABASE cams_db OWNER TO cams_user;
```

The host-side PostgreSQL port CAMS expects is **5433** (matching the Compose
setup). Override with `DB_URL` if yours differs.

### Backend

```bash
export JWT_SECRET=your_base64_256_bit_key
export DB_PASSWORD=your_password
export STORAGE_ACCESS_KEY=minioadmin
export STORAGE_SECRET_KEY=minioadmin
./mvnw spring-boot:run
```

The API comes up on http://localhost:8080. Flyway applies migrations on startup.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

The dev server runs on http://localhost:5173 and reads the backend URL from
`VITE_API_URL` (defaults to `http://localhost:8080`).

### Useful backend commands

```bash
./mvnw test                       # run all tests
./mvnw test -Dtest=ClassName      # run one test class
./mvnw clean package              # build the jar
```

---

## Project Structure

```
cams-platform/
├── src/main/java/cams/          # Spring Boot backend
│   ├── config/ controller/ service/ repository/
│   ├── entity/ dto/ mapper/ security/ client/ event/ exception/
├── src/main/resources/
│   ├── application.yml           # configuration; secrets via env vars
│   └── db/migration/             # Flyway migrations (V1…V31)
├── frontend/                     # React + Vite single-page app
│   ├── src/
│   ├── Dockerfile                # multi-stage: Vite build → Nginx
│   ├── nginx.conf                # production Nginx config
│   └── nginx.local.conf          # local Nginx config (plain HTTP)
├── embedding-service/            # FastAPI embedding microservice
│   ├── app.py
│   ├── requirements.txt
│   └── Dockerfile
├── scripts/
│   ├── skill-scope/              # generates the seeded skill vocabulary
│   └── matching-lab/             # offline matching-strategy evaluation harness
├── Dockerfile                    # backend: multi-stage Maven build → JRE
├── docker-compose.local.yml      # local stack (plain HTTP)
└── docker-compose.yml            # production stack (HTTPS via Nginx)
```

---

## API Overview

Full, interactive documentation is at `/swagger-ui/index.html` when the backend
is running. All endpoints except registration, login, employer-request
resubmission, and the Swagger paths require a bearer token.

| Area | Representative endpoints |
|---|---|
| Auth | `POST /api/auth/register`, `POST /api/auth/login` |
| Candidate profile | `GET`/`PUT /api/candidate-profiles/me`, plus `/skills`, `/experience`, `/education`, `/courses`, `/picture` |
| Job listings (employer) | `POST /api/job-listings`, `GET /api/job-listings/mine`, `PUT /api/job-listings/{id}`, `PUT /api/job-listings/{id}/skills`, `POST /api/job-listings/{id}/archive` |
| Job listings (candidate) | `GET /api/job-listings` (browse + filters), `GET /api/job-listings/matches` (ranked), `GET /api/job-listings/{id}/candidate-view` |
| Candidacies | `POST /api/candidacies` (apply), `GET /api/candidacies/mine`, `GET /api/job-listings/{id}/candidacies` (applicants), `POST /api/candidacies/{id}/accept` \| `/reject` |
| Companies | `GET /api/companies`, `GET /api/companies/{id}`, `GET`/`PUT /api/companies/me`, `POST /api/companies/me/logo` |
| Employer requests | `GET /api/employer-requests/me`, `POST /api/employer-requests/resubmit` |
| Applications (tracker) | `GET`/`POST /api/applications`, `GET`/`PUT`/`DELETE /api/applications/{id}` |
| Skills | `GET /api/skills` |
| Admin | `GET /api/admin/employer-requests`, `POST /api/admin/employer-requests/{id}/approve` \| `/reject`, `POST /api/admin/users/promote`, `POST /api/admin/reembed-listings` \| `reembed-candidates` |

---

## Key Design Decisions

**Stateless JWT authentication.** No server-side sessions. Every request carries
a signed token; the backend validates the signature with a secret that never
leaves the server. `/api/auth/**` and the Swagger paths are public, everything
else requires a valid token.

**Authorization in the service layer.** Every service method resolves the
authenticated user and checks ownership before acting. Access to a job listing is
scoped to the owning *company*, not the individual who posted it. The 403-vs-404
distinction is deliberate: "exists but not yours" returns 403, "does not exist"
returns 404.

**Strict request/response DTO separation.** Request DTOs carry validation
annotations and no server-generated fields; response DTOs carry `id` and
timestamps and are never deserialized from client input. A dedicated mapper class
is the only place translation happens.

**Centralised error handling.** Custom exceptions are thrown from the service
layer and caught by one `@RestControllerAdvice` that returns a consistent JSON
error shape. Controllers contain no try/catch.

**Flyway owns the schema.** Hibernate runs in `validate` mode. Schema changes are
new versioned migration files; applied migrations are never edited.

**Embeddings are computed off the request path.** A profile or listing save
publishes a domain event; an `@Async` listener recomputes the embedding *after*
the transaction commits, in its own transaction. A slow or failing embedding call
never delays or breaks a save.

**Similarity search runs in the database.** The candidate's match list is a
single `pgvector`-ordered query rather than loading rows and ranking them in Java.

**Client-side dashboard statistics.** The tracker dashboard fetches the plain
applications list and computes chart data in the browser with a pure function -
there are no backend aggregation endpoints for display-only stats.

**One codebase, two environments.** `docker-compose.local.yml` uses plain HTTP
and `localhost`; `docker-compose.yml` adds HTTPS termination and the production
domain. The frontend image takes a build argument to pick the right Nginx config.

**Storage is S3-compatible, not AWS-specific.** The same S3 SDK client talks to a
local MinIO container in development and a real S3 or R2 bucket in production,
selected entirely through `STORAGE_*` configuration.

---

## Environment Variables

| Variable | Required | Description |
|---|---|---|
| `DB_PASSWORD` | Yes | Password for the `cams_user` database role |
| `JWT_SECRET` | Yes | Base64-encoded 256-bit key for signing JWTs |
| `STORAGE_ACCESS_KEY` | Yes | Object-storage access key (MinIO root user locally) |
| `STORAGE_SECRET_KEY` | Yes | Object-storage secret key (MinIO root password locally) |
| `DB_URL` | No | Full JDBC URL; overrides the default `localhost:5433` |
| `EMBEDDING_SERVICE_URL` | No | Base URL of the embedding service (default `http://localhost:8000`) |
| `STORAGE_ENDPOINT` / `STORAGE_PUBLIC_URL_BASE` / `STORAGE_REGION` / `STORAGE_BUCKET` | No | Object-storage target; defaults point at the local MinIO container |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | No | If both are set, an admin account is created once on startup |

Never commit real secret values.
