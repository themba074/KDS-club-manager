# KDS Club Manager — Agent Context

## Project
KDS Club Manager is a configurable, multi-tenant Club Operating System for
membership organisations (stokvels/investment clubs, sports clubs, burial
societies, business associations, etc.). One core platform; club-type
templates configure which modules/rules are active per club. See
docs/PRD.md and docs/TECH_SPEC.md for full detail — this file is a
summary for agent context, not the full spec.

## Stack
- Frontend: React 19 + TypeScript + Vite, Zustand (client state), TanStack
  Query (server state), React Router, Tailwind CSS, shadcn/ui
- Backend: Java 21, Spring Boot 4.1.0, Spring Security (JWT), Spring Data
  JPA, MapStruct, Flyway
- Database: PostgreSQL — shared schema, tenant-scoped via `club_id` +
  enforced TenantContext (see docs/TECH_SPEC.md §3). NEVER write a query
  or repository method that skips tenant scoping.
- Storage: Supabase Storage via FileStorageService abstraction
- Deployment: Docker / Docker Compose, GitHub Actions CI

## Architecture
- ClubTypeConfig now provides the global Investment Club role/permission
  catalog. Identity owns tenant-scoped membership role assignments.
- Modular monolith. Modules: Identity/Tenancy, Members, Contributions,
  Meetings, Voting, Documents, Notifications, Audit, ClubTypeConfig.
- Modules talk to each other only through their public application-service
  interfaces — never reach into another module's repositories directly.
- API is REST, versioned (/api/v1), tenant resolved from JWT (not URL).
- Permissions (not role names) are what controllers check
  (`@PreAuthorize`); role→permission mapping lives in ClubTypeConfig.

## How We Want You To Work
We are two developers (Samukelo and Thembani) learning as we build this,
not just outsourcing implementation. For any non-trivial task:
1. Explain what you're about to build and why, in plain terms.
2. Explain the alternatives you considered and why you're recommending
   this approach.
3. Flag trade-offs and anything we should sanity-check.
4. Wait for explicit approval before writing code.
Trivial/mechanical tasks (boilerplate matching an existing pattern, a
straightforward CRUD endpoint identical in shape to one already built)
can skip the full explanation, but still summarize what you did
afterward. When in doubt, explain first.

## Coding Standards
- SOLID, composition over inheritance, DTOs separate from entities
  (MapStruct), Bean Validation on all inbound DTOs.
- Meaningful names, no abbreviations. Consistent folder structure per
  module on both frontend and backend.
- Every new backend endpoint: unit tests for service logic + integration
  test that verifies tenant isolation (a cross-tenant request must fail).
- Every new frontend feature: components under /features/<module>,
  server state via TanStack Query hooks, no direct fetch() calls in
  components.

## Feature Assignment
Full-stack ownership rotates per feature between Samukelo and Thembani —
see docs/TEAM_WORKFLOW.md §7 for the complete sequence (#0–#21) and the
filled-in prompt for each one. Assignment is a default, not a rule —
either of you can pick up the other's feature if it makes sense.

- Feature 3 and the 8-character password minimum were originally committed
  at b66e90e.
- Features #0–#8 are merged on `main`; #9 is in progress on
  `feature/contribution-reports`.

## Current Build Status
- Phase: Contributions
- Completed foundation work: repository flattened and initialized on
  `main`; `/backend` Spring Boot and `/frontend` Vite+React skeletons
  created.
- Landed on `main`: #0 — public backend
  health endpoint, Docker Compose for PostgreSQL/backend/frontend, and a
  GitHub Actions backend/frontend verification workflow.
- Landed on `main`: #1 — Frontend app shell (Samukelo): responsive
  layout, centralized routing/navigation, design tokens, placeholder
  feature pages, shared loading/empty/error states, and frontend render
  tests.
- Landed on `main`: #2 — global user identity, registration/login, rotating
  refresh-token sessions, password reset, protected SPA routes, and auth
  forms.
- Committed foundation: #3 — club wizard,
  administrator membership, club selection/refresh, tenant request context,
  and scoped persistence with isolation tests.
- Identity bootstrap (create/list/select clubs) is authenticated-user-scoped;
  it must work before a club is selected. All tenant-data reads require
  TenantContext and explicit repository predicates (see TECH_SPEC section 3).
- Landed on `main`: #4 — seeded global Investment Club role catalog, scoped
  assignments, current-permission backend checks, last-manager protection,
  and permission-aware UI.
- Landed on `main`: #5 — tenant-scoped, expiring member invitations;
  new/existing-account acceptance; member profiles; searchable/filterable
  directory; and isolation tests.
- Landed on `main`: #6 — active,
  suspended, and exited membership transitions; immediate access revocation;
  last-role-manager protection; stateless CSV inspection, column mapping,
  validation preview, and partial invitation import; status/import UI; and
  lifecycle, mixed-import, and tenant-isolation tests.
- Landed on `main`: #7 —
  effective-dated monthly/once-off schedules, active-member assignment
  snapshots, immutable revisions, upcoming expectations, permissions, and
  tenant isolation.
- Landed on `main`: #8 — immutable,
  tenant-scoped partial payment tracking; optional proof storage; expected,
  paid, and running balances; and authenticated-member-only ledgers with
  privacy and tenant-isolation tests.
- In progress on `feature/contribution-reports`: #9 — contribution summaries
  with per-member expected, collected, and outstanding totals plus CSV/PDF
  exports.
- Next required after #9 is reviewed and merged: #10 — meeting scheduling and
  agenda management.

## Do Not
- Do not introduce a new state-management library beyond Zustand/TanStack
  Query without a team discussion.
- Do not add a new top-level module without updating this file and
  docs/TECH_SPEC.md.
- Do not bypass tenant scoping "just for now."
- Do not skip the explain-first step in "How We Want You To Work" above.
