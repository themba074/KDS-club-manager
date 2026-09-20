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

The planned sequence #0–#21 is complete as of the team update on 2026-09-20.
The current phase is quality review and pilot feedback. Staging configuration
and open security findings must still be checked independently.

Delivered capabilities:

| Feature | Delivered capability |
| --- | --- |
| 0–4 | Docker/CI foundation, app shell, authentication, clubs/tenancy, and roles/permissions |
| 5–6 | Member invitations, directory, lifecycle status changes, and CSV import |
| 7–9 | Contribution schedules, payment/ledger tracking, and reports/exports |
| 10–11 | Meeting scheduling/agendas, RSVP, draft/published minutes, and secured minute attachments |
| 12 | Motion creation, voter snapshots, voting windows, and cancellation |
| 13 | One-time ballots, private tallies, and immutable published results |
| 14–16 | Document library, notifications, and audit trail |
| 17–18 | Club-type configuration and cross-module reporting |
| 19 | Security and tenant-isolation review; open findings tracked separately |
| 20–21 | Staging deployment infrastructure and pilot onboarding polish |

For detailed usage notes, read the feature sections in `README.md`.

## Historical implementation notes: Feature 15

At the time of these notes, features 0 through 14 had landed and Feature 15
was implemented on:

```text
codex/notification-service
```

The branch adds tenant-scoped in-app notification persistence, feed and unread
APIs, read controls, email delivery state, and log/SMTP delivery adapters.
`DomainEventPublisher` keeps source modules independent of delivery. Handlers
run after the source transaction commits and failures are contained, so meeting
scheduling, minutes publication, motion changes, and payment reminder requests
remain successful if notification work fails.

Meeting scheduling and edits notify active members immediately. Minutes
publication does the same. Motion creation stores durable notifications for the
opening instant and one hour before closing; short windows use their midpoint
for the closing reminder. Draft edits replace undelivered work and cancellation
suppresses it. Managers trigger payment reminders explicitly from an
outstanding contribution.

The Notifications page and header badge use TanStack Query, support individual
and bulk read actions, and link recipients to the relevant feature. The email
worker establishes `TenantContext` for each club and retries failed delivery up
to three times. Local development logs email; SMTP is configured through the
variables documented in `.env.example`.

Migration V13 creates the notification table and tenant membership foreign key.

Verification on 2026-09-13:

- `backend/`: `./mvnw.cmd test` passed all 147 tests. Coverage includes
  trigger mapping, publisher and email failure isolation, read scoping,
  cross-tenant endpoint denial, scheduled delivery, and an end-to-end payment
  reminder reaching the intended member feed.
- `frontend/`: all 59 tests passed; lint and the production build passed.
- Docker rebuilt the backend and upgraded the existing PostgreSQL 16 volume
  from V12 to V13. The backend is healthy and the `notifications` table exists.
- SMTP credentials are not present in this environment, so the SMTP adapter is
  covered through its boundary and configuration while local delivery uses the
  logging adapter.

Feature 15 has since been completed. The verification counts above are a
historical snapshot, not the current regression-test totals.

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
5. Create a focused `codex/...` branch from updated `main` for new work.
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

## Roadmap after Feature 15

The planned sequence is:

1. Feature 16: immutable audit logging and viewer.
2. Features 17–21: club-type configuration, reports, hardening, deployment,
   and pilot-launch polish.

Use `docs/TEAM_WORKFLOW.md` as the authoritative feature plan. It includes
dependencies, acceptance criteria, and the intended developer workflow.
