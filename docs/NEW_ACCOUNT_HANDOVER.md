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

Features 0 through 11 are merged on `main`:

| Feature | Delivered capability |
| --- | --- |
| 0–4 | Docker/CI foundation, app shell, authentication, clubs/tenancy, and roles/permissions |
| 5–6 | Member invitations, directory, lifecycle status changes, and CSV import |
| 7–9 | Contribution schedules, payment/ledger tracking, and reports/exports |
| 10–11 | Meeting scheduling/agendas, RSVP, draft/published minutes, and secured minute attachments |

For detailed usage notes, read the feature sections in `README.md`.

## Current work: Feature 12

Feature 12, motion creation and voting windows, is implemented locally on:

```text
feature/motion-voting-window
```

The branch contains uncommitted work and has not been merged. It now includes
the motion schema/API, lazy `DRAFT`/`OPEN`/`CLOSED` state calculation, explicit
cancellation, active-member snapshots, and a Voting UI with creation, editing,
cancellation feedback, and automatic refresh every 15 seconds.

Drafts and explicitly cancelled motions can be edited. Cancelled edits never
reopen the motion. Managers receive voter IDs for editing; regular members see
only the count and their own eligibility. The editor preserves the voter
snapshot unless the manager explicitly changes it. Membership and permission
checks run under Identity's club lock before writes; stale versions fail.

The completion pass fixed two ORM collection issues: replacing every voter row
could violate its unique constraint, and fetching voters with a list of options
could duplicate options and corrupt a later edit. Unchanged voter rows are now
retained; options use a set with explicit ordered positions. Integration tests
exercise create/edit/reload with multiple voters and changing option counts.

Verification on 2026-09-11:

- `backend/`: `./mvnw.cmd -B -ntp verify` passed, 97 tests.
- `frontend/`: lint passed; the full single-worker suite passed, 49 tests;
  production build passed. The 12 voting tests and build passed again after
  the final form event-handler adjustment.
- Voting coverage includes exact window boundaries, cancellation corrections,
  invalid/foreign/inactive voters, snapshot stability, stale edits, cross-tenant
  reads/writes, permissions, frontend forms, polling, and club switching.
- Docker Compose built and started PostgreSQL, backend, and frontend. The
  PostgreSQL smoke test passed create/edit/reload, stale-edit rejection,
  cancellation/correction without reopening, and cross-club list/edit/cancel/
  voter-assignment isolation. Backend health and frontend HTTP checks passed.
  The test created a separate local test account and two smoke-test clubs;
  existing credentials and database data were preserved.
- The frontend Docker health probe now uses `127.0.0.1` to match Nginx's IPv4
  listener. The previous `localhost` probe failed inside the Alpine container
  even while the published frontend URL returned HTTP 200.

Remaining handoff: developer review of the final diff, then commit/push/open
the review only when explicitly requested. Do not mark Feature 12 as merged
in `AGENTS.md` yet. The local app is available at http://localhost:5175.

Do not start Feature 13 from `main` until Feature 12 has been reviewed and
merged. Feature 13 adds actual vote casting, duplicate-vote prevention,
tallies, locking, and published results.

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

## Roadmap after Feature 12

The planned sequence is:

1. Feature 13: vote casting, tally, lock, and publish.
2. Feature 14: documents, Supabase storage adapter, and access rules.
3. Feature 15: in-app/email notifications and existing trigger wiring.
4. Feature 16: immutable audit logging and viewer.
5. Features 17–21: club-type configuration, reports, hardening, deployment,
   and pilot-launch polish.

Use `docs/TEAM_WORKFLOW.md` as the authoritative feature plan. It includes
dependencies, acceptance criteria, and the intended developer workflow.
