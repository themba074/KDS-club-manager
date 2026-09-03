# KDS Club Manager — Technical Specification

**Version:** 1.0 (Draft)
**Status:** Planning — Phase 3 (System Design), informed by Phase 1 PRD
**Date:** August 2026

> Note: this spec uses **React** on the frontend (swapped from the Vue stack in the original brief), per your preference. Everything else follows the original stack unless noted.

---

## 1. Technology Stack

### Frontend
- **React 18+** with **TypeScript**
- **Vite** — build tool/dev server (fast HMR, same reasoning as with Vue: far better DX than CRA/Webpack for a new project)
- **Zustand** or **Redux Toolkit** for state management — recommend **Zustand** for a modular monolith frontend: less boilerplate than Redux, sufficient for club-scoped state (current club, current user, permissions), and easier to reason about per-module slices. Redux Toolkit is the fallback if the team later wants stricter conventions/devtools ecosystem.
- **React Router** for routing
- **TanStack Query (React Query)** for server-state (API data fetching, caching, invalidation) — this pairs well with Zustand: Zustand owns *client* state, React Query owns *server* state, which keeps the two concerns from tangling.
- **Tailwind CSS** for styling
- **A component library** — recommend **shadcn/ui** (Tailwind-native, unstyled-primitive based, so it won't fight your design system) as the React equivalent of PrimeVue's role. Alternative: **Mantine** or **PrimeReact** (PrimeVue's own React sibling) if you want a more "batteries-included" admin-UI feel out of the box — worth a quick trade-off discussion when we get to frontend architecture in detail.

### Backend
- **Java 21 (LTS)**
- **Spring Boot 4.1.0**
- **Spring Security** (JWT-based authentication/authorization)
- **Spring Data JPA** (Hibernate) for persistence
- **Bean Validation (Jakarta Validation)** for request validation
- **MapStruct** for entity↔DTO mapping (keeps mapping code out of services, reduces boilerplate)
- **Flyway** for database migrations

### Database
- **PostgreSQL 16+**
- Row-level tenant scoping strategy defined in the Multi-Tenancy section below

### Storage
- **Supabase Storage** (S3-compatible) via a storage abstraction interface, so the concrete provider can be swapped for AWS S3/Cloudflare R2 later without touching business logic

### Deployment
- **Docker** + **Docker Compose** for local/dev and simple production deployment
- CI/CD via **GitHub Actions** (build, test, lint, containerize)
- Cloud target: any Docker-friendly host initially (e.g. a single VPS or managed container service); designed to move to Kubernetes later only if/when scale demands it — avoiding premature infra complexity

### Architecture Style
- **Modular monolith**: one deployable Spring Boot application, internally organized into clearly bounded modules (Members, Contributions, Meetings, Voting, Documents, Notifications, Audit, Identity/Tenancy, ClubTypeConfig)
- Modules communicate through well-defined internal interfaces (application services), **not** direct repository access across module boundaries — this is what makes later microservice extraction feasible without a rewrite
- **Multi-tenant from day one**, tenant = Club

---

## 2. High-Level Architecture

```
┌─────────────────────────────┐
│   React SPA (Vite build)    │
│  Admin views + Member views │
└──────────────┬───────────────┘
               │ HTTPS / REST (JSON) + JWT
┌──────────────▼───────────────┐
│      Spring Boot API          │
│  ┌─────────────────────────┐ │
│  │ API Layer (Controllers)  │ │
│  ├─────────────────────────┤ │
│  │ Module Services          │ │
│  │ (Members, Contributions, │ │
│  │  Meetings, Voting, Docs, │ │
│  │  Notifications, Audit,   │ │
│  │  Identity/Tenancy,       │ │
│  │  ClubTypeConfig)         │ │
│  ├─────────────────────────┤ │
│  │ Persistence (Spring Data │ │
│  │ JPA repositories)        │ │
│  └─────────────────────────┘ │
└──────────────┬───────────────┘
       ┌────────┴─────────┐
┌──────▼──────┐    ┌───────▼────────┐
│ PostgreSQL   │    │ Supabase       │
│ (tenant-     │    │ Storage (S3-   │
│ scoped data) │    │ compatible)    │
└──────────────┘    └────────────────┘
```

**Why a modular monolith and not microservices from day one?**
At this stage (a two-developer team with an unvalidated product), microservices add operational overhead (service discovery, distributed transactions, network failure handling, multiple deployment pipelines) without a corresponding benefit — there's no scale problem to solve yet. A modular monolith with clean internal boundaries gives most of the *organisational* benefit of microservices (clear ownership, low coupling) while keeping *operational* complexity low. When/if a specific module (likely Notifications or Contributions under high tenant load) needs to scale independently, it can be extracted because the boundary already exists in code.

---

## 3. Multi-Tenancy Strategy

**Chosen approach: shared database, shared schema, with a `club_id` (tenant) column on every tenant-scoped table, enforced inside tenant-aware repositories.**

### Alternatives considered
| Approach | Isolation | Ops complexity | Cost at scale | Notes |
|---|---|---|---|---|
| Database-per-tenant | Highest | High (N databases to migrate/manage) | High | Great isolation, painful at hundreds/thousands of tenants |
| Schema-per-tenant | High | Medium-high | Medium | Postgres schema-per-tenant is workable but migrations across many schemas get unwieldy |
| **Shared schema + tenant column (chosen)** | Medium (enforced in app layer) | Low | Low | Standard SaaS pattern at this scale; requires discipline to enforce isolation correctly |

### Why shared schema wins here
For an MVP validating a product thesis with a handful to low hundreds of clubs, database- or schema-per-tenant is premature — it multiplies migration and operational work for isolation benefits you don't yet need. Shared schema with a `club_id` on every row, combined with:
- A `TenantContext` (thread-local, populated from the authenticated JWT's club claim) resolved once per request, and
- Tenant-aware repositories that obtain the required ID from `TenantContext` and include it in every query; unrestricted inherited CRUD methods must not be exposed for tenant entities.

...gives strong practical isolation without the operational cost. The key engineering discipline: **no query bypasses the tenant filter**, enforced by code review conventions and integration tests that specifically attempt cross-tenant reads.

This can migrate to schema-per-tenant later for large/enterprise clubs if needed — the abstraction (a `TenantContext`) is designed so the underlying isolation mechanism can change without touching module business logic.

### Feature 3 implementation

- Global identity/authentication and authenticated-user-scoped club bootstrap are explicit exceptions to requiring an active tenant. `POST /api/v1/clubs` creates a club and its creator membership atomically; `GET /api/v1/clubs` lists only that user's memberships. Only `INVESTMENT_CLUB` is accepted for now.
- Protected `POST /api/v1/auth/select-club` accepts `clubId`, requires a bearer token and a refresh cookie belonging to the same user, validates membership, and rotates the session. It does not trust a requested ID as proof of access.
- Login/registration start without a selected club. Selection adds the `clubId` JWT claim and stores the active club on the refresh session. Refresh revalidates membership. User-row locking serializes rotation and reuse revocation.
- The tenant filter runs after bearer verification, checks membership, sets context, and clears it in `finally`. Missing/invalid/inaccessible club context is rejected with 403. `GET /api/v1/club` exercises the tenant boundary.
- `CurrentClubRepository` applies the context predicate even for ID lookups. `ClubAccessRepository` is a narrowly scoped identity bootstrap repository whose reads always constrain `userId`. There is no global Hibernate filter or database row-level security: future module repositories must implement this boundary and cross-tenant tests explicitly.
- The frontend keeps the active club in Zustand, cancels/removes cached queries during selection, hides tenant pages while switching, and discards responses from an earlier session version. Future tenant query keys must include the active club ID. Refresh and selection are serialized within a tab; multi-tab session coordination is not implemented and simultaneous refreshes may require signing in again.
- Creator membership receives the Administrator role under Feature 4. The
  returned administrator boolean is display-only; permissions authorize actions.

---

## 4. Authentication & Authorization

### Authentication
- Email/password with **JWT** access tokens (short-lived, ~15 min) + refresh tokens (longer-lived, stored securely, rotated on use).
- Password hashing via **BCrypt**.
- JWT payload includes: `userId`, `clubId` (active tenant context), and a list of **permissions** (not raw roles) resolved at login/token-refresh time.

### Why permissions in the token, not just roles
Roles are configurable per club type (a "Treasurer" role in a stokvel maps to different underlying permissions than in a sports club). Encoding **resolved permissions** rather than a role name means the API layer's `@PreAuthorize` checks stay simple and generic (`hasAuthority('CONTRIBUTIONS_WRITE')`) regardless of what the role is *called* for that club type. The role→permission mapping lives in ClubTypeConfig. Tokens carry a snapshot; tenant requests resolve current permissions again for enforcement.

### Authorization model

- Feature 4: ClubTypeConfig owns the global read-only role catalog and
  role-permission mappings; Identity owns club-scoped assignments. Each
  membership currently has one role. Administrator is the management role
  alongside Chairperson, Treasurer, Secretary and Member. Custom roles and
  multiple simultaneous roles are not exposed in this foundation.
- Existing creator memberships migrate to Administrator; other memberships
  migrate to Member. New creators receive Administrator. A per-club lock and
  permission re-check protect assignment and prevent removing the last role
  manager. JWT permissions are a session snapshot; backend authorities are
  resolved from current membership/configuration on every tenant request.

#### Feature 4 defaults and API

The seeded catalog lives in migration V3. Administrator has every defined
permission, including ROLES_MANAGE; Chairperson can read role definitions
but cannot assign roles. Treasurer can write contributions; Secretary can
manage documents and membership details; Member has shared read access and
VOTES_CAST. All roles can read meetings/documents and vote. Chairperson has
VOTES_CREATE and AUDIT_READ; Treasurer and Secretary do not. Permissions
are deliberately not inferred from a role's displayed name.

- GET /api/v1/roles: ROLES_READ; current club type's read-only role definitions.
- GET /api/v1/role-members: ROLES_MANAGE; current club's membership IDs, emails and roles.
- PUT /api/v1/role-members/{membershipId}: ROLES_MANAGE; body contains roleCode.
  Unknown roles return 400, foreign memberships 403, last-manager removal 409.
- GET /api/v1/permissions: current authenticated club membership's effective permissions.

Role management uses both method-level checks and an application-service
re-check under the club lock. Membership reads/updates constrain club_id.
Global catalog queries constrain club_type, not tenant IDs, by design.
There is no public role-definition editor in Feature 4.

Session responses expose activeClub.permissions, and selected-club JWTs
include the same permission snapshot. The browser uses permissions for
navigation, route guards and assignment controls. Changes to another user's
UI become visible on refresh/login; backend revocation does not wait for
their token to expire. The assigning user's session is refreshed after saving.
Frontend cache keys contain the club ID. Member onboarding is described in the
Feature 5 section below.

Finance/document business endpoints remain future work. Feature 4's integration
tests use test-only secured operations to verify Treasurer/Member allow-deny
behavior; no placeholder financial write endpoint is shipped.

#### Feature 5 member invitations and directory

- Pending invitations are separate from `club_memberships`, so an invited
  email has no tenant access until its invitation is accepted.
- Invitation secrets are random, stored only as SHA-256 hashes, expire after
  seven days, and are locked and marked used during acceptance. The public
  token bootstrap endpoints expose preview and acceptance only.
- Acceptance creates an Identity user when necessary, or links an existing
  account after email-link possession proves access. It then creates the
  membership/profile atomically and returns a selected-club session.
- Members owns invitation/profile persistence. It obtains active membership
  identity data through a public Identity application service rather than
  reaching into Identity repositories.
- `GET /api/v1/members` requires `MEMBERS_READ` and supports search/status
  filtering. `POST /api/v1/member-invitations` requires `MEMBERS_WRITE` and
  creates Member-role invitations. Role elevation remains in role management.
- Development delivery logs the acceptance URL. Real email delivery remains
  the responsibility of Feature 15's notification service.

#### Feature 7 contribution schedules

- Contributions owns stable schedule identities, immutable effective-dated
  revisions, and revision-specific member assignment snapshots. It accesses
  tenant members only through the public Members application service.
- `CONTRIBUTIONS_WRITE` creates schedules and revisions; `CONTRIBUTIONS_READ`
  lists current revisions and calculated expectations. Application services
  re-check write permission under the same club lock used by member lifecycle
  changes, preventing role/status changes from racing an assignment snapshot.
- Amounts use `NUMERIC(19,2)`/`BigDecimal` and currently use explicit `ZAR`.
  Monthly schedules retain their original due-day, clamped to month-end;
  once-off schedules produce one expectation. Date-range calculation is capped
  at one year.
- An edit closes the previous revision on the day before the new effective
  date and inserts a new row. Payments reference the immutable revision ID and
  exact due date, so a later schedule edit cannot rewrite recorded terms.

Endpoints under `/api/v1/contribution-schedules` are tenant scoped: list/create,
`PUT /{scheduleId}` for revision creation, `/assignable-members`, and
`/upcoming?from=YYYY-MM-DD&to=YYYY-MM-DD`. Foreign IDs are deliberately reported
as unavailable rather than revealing another tenant's records.

#### Feature 8 payment tracking and member ledger

- `contribution_payments` stores immutable positive receipts against an exact
  schedule revision, assigned membership, and due date. Multiple receipts can
  represent partial payments; expected and paid totals are calculated rather
  than maintained as a mutable balance column.
- `POST /api/v1/contribution-payments` requires `CONTRIBUTIONS_WRITE` and
  accepts multipart payment data plus an optional PDF, JPEG, or PNG proof up
  to 1 MB. The service re-resolves the referenced expectation inside the
  active tenant before inserting anything.
- `GET /api/v1/contribution-payments/expectations` supplies treasurers with
  expected, paid, and outstanding amounts for the mark-as-received workflow.
- `GET /api/v1/contribution-payments/my-ledger` requires
  `CONTRIBUTIONS_READ`. It does not accept a membership identifier: Identity
  resolves the active membership from the authenticated user and tenant.
  Expectations and receipts become chronological debit/credit lines with a
  calculated running balance.
- Payment proof persistence calls the public Documents `FileStorageService`
  boundary. The development adapter writes tenant/category-namespaced files;
  Feature 14 will supply the Supabase adapter and signed retrieval URLs.

#### Feature 9 contribution reports

- `GET /api/v1/contribution-reports/summary` returns club totals and a
  per-member breakdown of expected, collected, and outstanding contributions
  for a date range of at most one year. Both controller and application
  service enforce `REPORTS_READ`.
- `GET /api/v1/contribution-reports/export` accepts the same range and a
  `CSV` or `PDF` format. It builds one authorized, tenant-scoped report
  snapshot before streaming the selected representation, keeping the UI and
  exports on the same aggregation rules.
- Expected contributions are calculated from immutable schedule revisions;
  receipts are loaded through a period query with an explicit `club_id`
  predicate. Aggregation uses `BigDecimal` at two-decimal currency scale, and
  outstanding values are clamped at zero so overpayment is not shown as debt.
- CSV output is UTF-8 and neutralizes values beginning with spreadsheet
  formula characters. PDF output uses Apache PDFBox and paginates member rows.
  Export generation does not create report or temporary-file persistence.

#### Meeting scheduling and agenda

- Meetings and agenda items are tenant-owned records. Repository reads use an
  explicit `club_id` predicate in addition to `TenantContext`, and agenda rows
  carry `club_id` so isolation remains visible at the database boundary.
- Meeting instants are stored as `TIMESTAMPTZ` together with the submitted UTC
  offset for faithful API display. Agenda positions are unique per meeting and
  are replaced as one ordered collection during an edit.
- `MEETINGS_READ` protects upcoming/past views; `MEETINGS_WRITE` protects
  scheduling and edits. Past meetings are immutable through the API, and JPA
  optimistic versions reject stale concurrent edits with HTTP 409.
- Created/updated meetings publish an after-commit, best-effort application
  event with the active-member audience obtained through the Members public
  application service. Feature 15 owns durable in-app/email delivery; failure
  of this interim notification seam never rolls back meeting scheduling.

#### General authorization rules

- Fixed, platform-defined **permission set** (e.g. `MEMBERS_READ`, `MEMBERS_WRITE`, `CONTRIBUTIONS_READ`, `CONTRIBUTIONS_WRITE`, `VOTES_CREATE`, `VOTES_CAST`, `DOCUMENTS_MANAGE`, `AUDIT_READ`, etc.)
- Each club type template defines its roles as **named bundles of permissions**.
- A member's effective permissions for a club = permissions attached to their assigned role(s) in that club.
- Enforcement at two layers:
  1. **API layer**: Spring Security method-level checks (`@PreAuthorize`) on every controller endpoint.
  2. **UI layer**: React renders/hides actions based on permissions returned with the user's session — this is a UX convenience only, **never** the source of truth (API always re-checks).

---

## 5. API Design

- **RESTful JSON API**, versioned from the start (`/api/v1/...`) to allow non-breaking evolution.
- Resource-oriented URLs scoped implicitly to the authenticated tenant (club) via the JWT — not via URL path params — to avoid the temptation to accidentally query another club's ID directly (e.g. `/api/v1/members` resolves "which club" from the token, not `/api/v1/clubs/{clubId}/members`, which would require re-validating the path clubId matches the token on every single endpoint).
- Standard DTOs separate from JPA entities (enforced via MapStruct) — prevents leaking persistence concerns (lazy-loading issues, internal-only fields) into the API contract.
- Consistent error format (problem-details style: `type`, `title`, `status`, `detail`, `errors[]` for validation failures).
- Pagination via `page`/`size` query params with a consistent envelope (`content`, `totalElements`, `totalPages`).

---

## 6. File Storage Strategy

- **Storage abstraction interface** (`FileStorageService`); local development
  currently uses a tenant-namespaced filesystem adapter, with the Supabase
  Storage adapter and signed retrieval flow landing in Feature 14.
- Files are namespaced by `club_id` and module (e.g. `documents/{clubId}/{documentId}/...`) to keep tenant separation visible even in storage, not just the DB.
- Access to files is via **signed, time-limited URLs** generated by the backend after a permission check — the frontend never talks to storage directly, so access control stays centralized in the API layer.

---

## 7. Logging, Monitoring, Security

### Logging
- Structured JSON logging (e.g. via Logback + a JSON encoder) from day one — much easier to query later even at small scale, and no cost to set up early.
- Correlation ID per request (propagated through to logs) for tracing a request end-to-end.

### Monitoring (lightweight for MVP)
- Spring Boot Actuator for health/metrics endpoints.
- A basic uptime/log aggregation tool appropriate to whatever host is chosen at deploy time (this is a "when we deploy" decision, not a Phase 3 blocker).

### Security
- HTTPS everywhere (enforced at the reverse proxy/load balancer).
- Input validation via Bean Validation on all DTOs.
- Rate limiting on auth endpoints (login, password reset) to reduce brute-force risk.
- Secrets (DB credentials, JWT signing key, storage keys) via environment variables / secret manager — never committed.
- CORS configured explicitly for the known frontend origin(s).
- Audit log (see PRD §5.11) as a security *and* trust feature — every financial/governance-relevant mutation is recorded with actor, timestamp, and action.

---

## 8. Scalability & Caching Considerations (forward-looking, not MVP-blocking)

- Modular monolith boundaries chosen specifically so **Contributions** and **Notifications** (the two modules most likely to see spiky, tenant-driven load — e.g. month-end reconciliation, mass reminder sends) can be extracted into separate services first if scale ever requires it.
- Caching candidates for later: club type template definitions (rarely change, read often), resolved role→permission mappings (invalidate on role change).
- No caching layer (e.g. Redis) in MVP — premature at expected scale; noted here so the architecture doesn't preclude adding one later.

---

## 9. Frontend Architecture (high level — detailed breakdown in Phase 3 continuation)

- Single React SPA serving both **admin** and **member** experiences, with route- and component-level rendering driven by the user's resolved permissions for the active club.
- Feature-folder structure mirroring backend modules (`/features/members`, `/features/contributions`, `/features/meetings`, `/features/voting`, `/features/documents`) — keeps frontend and backend mental models aligned, which matters a lot for a solo/small team maintaining both.
- A **"club context" provider** at the app root resolves the active club (for multi-club users) and exposes its permissions/enabled-modules to the rest of the tree — this is the frontend mirror of the backend's `TenantContext`.
- Club-type-driven UI: navigation items, terminology, and visible modules are derived from the active club's template config, not hardcoded — this is what makes the "OS" thesis real on the frontend, not just the backend.

---

## 10. Team Model: Two Developers + Agentic Engineering

Since this is being built by **two developers**, each using agentic coding tools (Claude Code / Codex), the module boundaries defined in §1 aren't just a future-microservices nicety — they're the mechanism that lets two people (and their AI agents) work in parallel without constantly colliding.

**Ownership rotates by full-stack feature:**
- One developer owns each active feature end-to-end across its backend module and matching frontend feature folder. Ownership rotates between Samukelo and Thembani according to the sequence in `TEAM_WORKFLOW.md` §7 so both developers gain experience across the full stack.
- While a feature is active, its owner has temporary ownership of the affected module paths. The other developer can read and consume the module through its public application-service interfaces, but coordinates before modifying those paths. Ownership returns to the team when the feature lands; it is not a permanent module assignment.
- Shared/cross-cutting concerns (auth, tenant context, DTO/API conventions, the design system) are **not** owned by either individual — changes to these require a quick sync and review between both developers before implementation, since both codebases depend on them.

This is also why the **API design decision in §5** (resolving tenant from the JWT, not the URL) matters for two-dev safety: it removes a whole class of "did you remember to scope this query" bugs that are easy for an agent working on an unfamiliar module to introduce.

See the companion **Team Workflow & AI Agent Collaboration Guide** for the actual day-to-day process, master prompt, and feature-prompt template you'll both use to keep your agents in sync.

---

## Next Step

This covers the system-design-level tech spec. Natural next steps from here, in the spirit of the original phased plan:
1. Confirm this stack/architecture (especially the React state-management choice and the multi-tenancy approach) before we go deeper.
2. Move into **Domain Modelling** (entities, aggregates, bounded contexts) — this is what the database design and UML in later phases will build on.

Let me know if you want to adjust anything here, or say "approved" and we'll move to domain modelling.
