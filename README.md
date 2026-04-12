# HumanaEthica — Volunteer Management Platform

**Full-stack web application** that connects non-profits with volunteers: activities, shift-based scheduling, capacity rules, and role-based access for admins, institution staff, and volunteers.

Built with a **layered Spring Boot** API, a **Vue.js / TypeScript** SPA, a **three-tier test strategy** (unit → integration → E2E), and **Docker-based** local and CI workflows—intended to read like a small production-style codebase, not a toy demo.

---

## Why it matters (for reviewers)

| Area | What you’ll find |
|------|-------------------|
| **Backend** | Java 21, Spring Boot 3.5, REST API, Spring Security with JWT (RSA), JPA, OpenAPI/Swagger |
| **Frontend** | Vue 2.7 + TypeScript, Vuetify, Vuex, client-side validation aligned with server rules |
| **Quality** | Spock/Groovy tests, PostgreSQL-backed integration tests, Cypress 13 E2E on real stack |
| **Ops** | Docker Compose for full stack, repeatable test targets, coverage (JaCoCo) |

---

## Tech stack

| Layer | Technology |
|-------|------------|
| **Backend** | Java 21, Spring Boot 3.5, Spring Security (JWT + RSA), Spring Data JPA |
| **Frontend** | Vue.js 2.7, TypeScript, Vuetify 2.7, Vuex, Vue Router, Axios |
| **Database** | PostgreSQL 14 |
| **Testing** | Groovy/Spock (unit + integration), Cypress 13 (E2E) |
| **CI/CD** | Pipeline automation, Docker Compose, Nginx |
| **Build** | Maven 3.9, Vue CLI 5, JaCoCo (coverage) |

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                     Vue.js Frontend                     │
│  TypeScript Models · Vuex Store · Axios Service Layer   │
└───────────────────────┬─────────────────────────────────┘
                        │ REST / JSON
┌───────────────────────▼─────────────────────────────────┐
│                  Spring Boot Backend                    │
│  Controllers → Services → Repositories → JPA Entities  │
│  JWT Auth · Custom PermissionEvaluator · OpenAPI Docs   │
└───────────────────────┬─────────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────────┐
│                    PostgreSQL 14                        │
└─────────────────────────────────────────────────────────┘
```

**Domain model:** `Institution` → `Activity` → `Shift` → `Participation`, with `Volunteer` ↔ `Enrollment` ↔ `Shift` (many-to-many via join table).

**User roles:** Admin, Member (institution staff), and Volunteer — permissions enforced with a custom `HEPermissionEvaluator` and Spring Security `@PreAuthorize`.

---

## Features

### Shift scheduling and capacity

- Members define time-bounded shifts per activity (location, participant limits).
- **Two-level capacity:** per-shift limit and aggregate activity-level limit.
- Backend ensures shift dates stay within the parent activity window; shift creation respects activity approval state.

### Volunteer enrollment

- Volunteers browse approved activities and enroll, selecting multiple shifts.
- Frontend blocks overlapping shifts before submit; backend remains authoritative.
- Members map enrollments to shifts via participations; server rejects when a shift is full.

### Security model

- JWTs signed with RSA keys generated at startup.
- Method-level authorization via `@PreAuthorize` and domain-aware permission checks.

### Activity lifecycle

- Activities move through states such as reported → approved → suspended, with registration, themes, and reporting flows.

---

## Testing strategy

```
Unit Tests (Spock/Groovy)        →  H2 in-memory DB, fast feedback
Integration Tests (Spock/Groovy) →  PostgreSQL, full service + DB stack
E2E Tests (Cypress 13)           →  Real browser, real DB, end-to-end flows
```

**E2E coverage includes:** shift creation and validation paths, multi-shift enrollment and overlap handling, participation blocked at capacity, and flows per role (admin, member, volunteer).

The automated pipeline runs these tiers with JaCoCo coverage and Cypress artifacts (e.g. video) suitable for debugging failures.

---

## Repository layout

The root folder name follows your clone (for example `humanaethica` on GitHub).

```
humanaethica/
├── backend/src/main/java/.../humanaethica/
│   ├── activity/          # Activity domain (entity, service, controller, DTO)
│   ├── shift/             # Shift domain
│   ├── enrollment/        # Enrollment domain
│   ├── participation/     # Participation domain
│   ├── auth/              # JWT + Spring Security configuration
│   └── user/              # User hierarchy (Admin, Member, Volunteer)
├── backend/src/test/groovy/   # Spock unit & integration tests
├── frontend/src/
│   ├── models/            # TypeScript domain models
│   ├── views/             # Vue pages (by role)
│   ├── components/        # Reusable UI (dialogs, forms, tables)
│   └── services/          # Axios API layer
├── tests/e2e/specs/       # Cypress suites
└── docker-compose.yml
```

---

## Running locally

### Docker Compose (recommended)

```bash
cp data/access.log.example data/access.log
cp data/error.log.example data/error.log
cp frontend/example.env frontend/.env

docker compose build
docker compose up -d frontend        # full stack at http://localhost:8081
```

API docs: `http://localhost:8080/swagger-ui.html`

### Without Docker (backend + frontend separately)

```bash
# Backend
cd backend && mvn clean spring-boot:run   # needs PostgreSQL + application-dev.properties

# Frontend
cd frontend && npm install && npm start   # http://localhost:8081
```

### Tests

```bash
# Unit tests
docker compose up be-unit-tests

# Integration tests
docker compose up integration-tests

# E2E (headless)
docker compose up e2e-run

# E2E (interactive; needs a display)
docker compose up e2e-open
```

Single test class or method (examples):

```bash
UNIT=CreateEnrollmentMethodTest docker compose up be-unit-tests
UNIT="CreateEnrollmentMethodTest#create enrollment" docker compose up be-unit-tests
```

---

## Continuous integration (GitLab)

The pipeline in `.gitlab-ci.yml` uses the CI/CD variable **HUMANAETHICA_CI_IMAGE**: a Docker image that includes JDK 21, Maven, Node.js, and PostgreSQL client tooling used by the test jobs. Set it under **Settings → CI/CD → Variables** in your GitLab project. You can derive a suitable image from the `backend` and `frontend` Dockerfiles or from your own base image.

Mirroring this repository to GitHub does not run GitLab CI; add GitHub Actions separately if you want CI there.

---

## Contributors

- Guilherme Monteiro  
- André Lopes
