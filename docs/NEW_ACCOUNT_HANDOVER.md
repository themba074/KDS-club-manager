# New Account Handover Guide

Welcome to KDS Club Manager. Read this file first, then read `AGENTS.md`,
`docs/PRD.md`, `docs/TECH_SPEC.md`, and `docs/TEAM_WORKFLOW.md` before making
changes.

## What this project is

KDS Club Manager is a configurable, multi-tenant operating system for clubs:
investment clubs and stokvels first, with sports clubs, burial societies, and
other membership organisations supported later through configuration.

The project is a modular monolith:

- `frontend/`: React 19, TypeScript, Vite, Zustand, TanStack Query, React
  Router, Tailwind, and shadcn/ui.
- `backend/`: Java 21, Spring Boot 4.1.0, Spring Security JWT, Spring Data
  JPA, MapStruct, and Flyway.
- PostgreSQL is the production-like database; Docker Compose provides local
  PostgreSQL, backend, and frontend services.

The tenant is a club. Every tenant-owned backend query must use both the
authenticated `TenantContext` and an explicit `club_id` predicate. Never use
a URL club ID as a substitute for tenant resolution, and never access another
module's repository directly.

## First-time setup

Install:

- Git
- Docker Desktop with Docker Compose
- Java 21
- Node.js 24

Clone the repository, open a PowerShell terminal at its root, and create a
local environment file only when one does not already exist:

```powershell
if (!(Test-Path .env)) { Copy-Item .env.example .env }
docker compose up --build
```

Local services:

- Frontend: <http://localhost:5175>
- Backend health: <http://localhost:8080/actuator/health>
- PostgreSQL: `localhost:5432`

Do not copy `.env.example` over an existing `.env`, because that can replace
local credentials and secrets. The example credentials are for local use only.

To stop services:

```powershell
docker compose down
```

Only use `docker compose down --volumes` when you intentionally want to erase
the local PostgreSQL data volume.

## Frontend-only development

Keep PostgreSQL and the backend in Docker, but run Vite locally when working
on frontend code. Do not run Docker frontend and Vite at the same time.

```powershell
docker compose stop frontend
docker compose up -d --build backend
Set-Location frontend
npm ci
npm run dev
```

Use <http://localhost:5175>. Vite proxies `/api` to the backend. If port 5175
is busy, stop the other frontend process instead of choosing a different port.

## Verification commands

Run these before handing a feature to review:

```powershell
Set-Location backend
.\mvnw.cmd test
```

```powershell
Set-Location frontend
npm run lint
npm run test:run -- --maxWorkers=1
npm run build
```

On this Windows machine Vitest's default parallel worker pool can time out
while starting workers. `--maxWorkers=1` is the reliable local command.

## What has landed

Features 0 through 13 are merged on `main`:

| Feature | Delivered capability |
| --- | --- |
| 0–4 | Docker/CI foundation, app shell, authentication, clubs/tenancy, and roles/permissions |
| 5–6 | Member invitations, directory, lifecycle status changes, and CSV import |
| 7–9 | Contribution schedules, payment/ledger tracking, and reports/exports |
| 10–11 | Meeting scheduling/agendas, RSVP, draft/published minutes, and secured minute attachments |
| 12 | Motion creation, voter snapshots, voting windows, and cancellation |
| 13 | One-time ballots, private tallies, and immutable published results |

For detailed usage notes, read the feature sections in `README.md`.

## Current work: Feature 14

Features 0 through 13 are merged on `main`. Feature 14, document upload,
storage, versioning, and role-based access, is implemented locally on:

```text
feature/document-library
```

The branch adds tenant-scoped upload/list/download endpoints, role allowlists
validated through ClubTypeConfig, optimistic metadata updates, and append-only
file versions. `DOCUMENTS_MANAGE` callers see and manage the full club library;
ordinary `DOCUMENTS_READ` callers receive only documents allowed for their
current role. Direct downloads repeat the tenant and visibility checks.

The shared `FileStorageService` remains compatible with contribution proofs and
meeting attachments. Local filesystem storage is the default. The Supabase
adapter uses a private bucket, uploads with overwrite disabled, and returns a
five-minute signed URL after authorization. Exact document keys use
`documents/{clubId}/{documentId}/{versionId}.{extension}`. No Supabase
credentials are present in this environment, so the adapter is covered by an
HTTP contract test rather than a live bucket test.
Docker Compose persists local files on its `storage_data` volume.

The Documents page uses TanStack Query for all server state. Managers can
upload, edit metadata/access, and add versions; every visible version can be
downloaded. Both browser and backend enforce the 5 MB limit, while the backend
also allowlists supported PDF, Office, CSV, text, PNG, and JPEG types.

Verification on 2026-09-12:

- `backend/`: `./mvnw.cmd test` passed, 136 tests. Coverage includes service
  validation and permission rechecks, tenant/role filtering, direct-download
  denial, immutable version downloads, optimistic conflicts, and the Supabase
  upload/signed-URL contract.
- `frontend/`: lint passed; all 57 tests passed; the production build passed.
- Docker Compose started the existing Feature 13 backend against its persisted
  PostgreSQL 16 volume at V11, then rebuilt Feature 14. Flyway upgraded the
  same volume to V12 successfully; all three document tables exist and the
  backend is healthy. An authenticated PostgreSQL smoke flow uploaded a local
  file and updated its metadata through the pessimistic row lock successfully;
  the generated account, club, database rows, and file were removed afterward.

Feature 14 is pending developer review. Do not commit, push, or merge until the
developers explicitly request it.

## How to continue safely

1. Check the current state first:

   ```powershell
   git status --short --branch
   git log --oneline --decorate -8
   ```

2. Preserve existing uncommitted work. Do not use `git reset --hard` or
   discard files unless the developers explicitly request it.
3. Read `AGENTS.md` before every feature. It is the working agreement for the
   project and may have been updated after this guide.
4. For a non-trivial task, explain the proposed implementation, alternatives,
   trade-offs, and tests; wait for explicit approval before writing code.
5. Create a focused `feature/...` branch from updated `main` for new work.
6. Add unit tests for service logic and integration tests proving cross-tenant
   requests fail for every new backend endpoint.
7. Keep frontend server calls inside TanStack Query hooks under the relevant
   `frontend/src/features/<module>/` directory. Do not call `fetch()` directly
   from components.
8. Do not commit, push, merge, or delete branches unless explicitly asked.

## Important design rules

- Controllers check permissions, not role names. The role-to-permission mapping
  belongs to ClubTypeConfig.
- DTOs are separate from JPA entities. Validate all inbound DTOs.
- Tenant context comes from the JWT; it is not supplied by a URL parameter.
- Modules communicate through public application services, never by importing
  another module's repository.
- Flyway migrations are append-only. Add a new migration rather than changing
  a migration that may already have been applied.
- Treat browser permission checks as usability improvements only. The backend
  must always enforce permission and tenant scope.

## Roadmap after Feature 14

The planned sequence is:

1. Feature 15: in-app/email notifications and existing trigger wiring.
2. Feature 16: immutable audit logging and viewer.
3. Features 17–21: club-type configuration, reports, hardening, deployment,
   and pilot-launch polish.

Use `docs/TEAM_WORKFLOW.md` as the authoritative feature plan. It includes
dependencies, acceptance criteria, and the intended developer workflow.
