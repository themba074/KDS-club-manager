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
| 12 | Motion creation, voter snapshots, voting windows, and cancellation |

For detailed usage notes, read the feature sections in `README.md`.

## Current work: Feature 13

Feature 12 is merged on `main`. Feature 13, vote casting, tally, lock, and
publish, is implemented locally on:

```text
feature/vote-casting-results
```

The branch contains the completed implementation and has not been merged. It adds
append-only, tenant-scoped ballots; one-vote-per-motion enforcement; private
manager tallies after closing; simple-majority outcomes; and immutable
published result snapshots. There is intentionally no override or unpublish
path. Once results are published, the motion cannot be cancelled.

Voting writes lock the club and motion before rechecking current permissions,
membership status, the eligible-member snapshot, the voting window, and option
ownership. A database unique constraint is the final duplicate-vote guard,
including concurrent submissions. Composite tenant foreign keys prevent mixed
club motion, option, membership, ballot, and result relationships.

The Voting page now offers a one-time radio ballot to eligible members, keeps a
failed selection available for retry, replaces a successful ballot with a
receipt, gives managers a closed-result preview and publish action, and hides
unpublished results from ordinary members. The motions response returns only
the caller's own selected option; tally responses contain counts without voter
identities.

Verification on 2026-09-12:

- `backend/`: `./mvnw.cmd test` passed, 114 tests.
- `frontend/`: lint passed; all 54 tests passed; the production build passed.
- Voting coverage includes window and eligibility checks, invalid and foreign
  options, inactive members, duplicate and concurrent vote attempts, result
  privacy, simple-majority/no-majority tallies, stale publication, immutable
  post-publication behavior, all three endpoints' tenant isolation, and the
  corresponding browser flows.
- Docker Compose rebuilt the backend against the existing PostgreSQL 16
  database. Flyway upgraded it from V10 to V11 successfully, the backend became
  healthy, and `flyway_schema_history` recorded V11 as successful.

Developer review is complete. The branch is ready for remote review; open or
merge a pull request only when explicitly requested. Do not mark Feature 13 as
merged in `AGENTS.md` yet. PostgreSQL and the backend are currently running
under Docker Compose; the frontend container was not rebuilt in this pass.

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

## Roadmap after Feature 13

The planned sequence is:

1. Feature 14: documents, Supabase storage adapter, and access rules.
2. Feature 15: in-app/email notifications and existing trigger wiring.
3. Feature 16: immutable audit logging and viewer.
4. Features 17–21: club-type configuration, reports, hardening, deployment,
   and pilot-launch polish.

Use `docs/TEAM_WORKFLOW.md` as the authoritative feature plan. It includes
dependencies, acceptance criteria, and the intended developer workflow.
