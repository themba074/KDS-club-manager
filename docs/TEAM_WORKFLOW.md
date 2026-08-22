# KDS Club Manager — Team Workflow & AI Agent Collaboration Guide

**Version:** 1.0 (Draft)
**Audience:** Samukelo + Thembani
**Purpose:** Define how Samukelo and Thembani, each driving agentic coding tools (Claude Code / Codex), build KDS Club Manager together without diverging, duplicating work, or drifting from the agreed architecture.

---

## 1. Why This Doc Exists

Agentic coding tools are extremely good at producing plausible, working code fast — and just as good at producing code that quietly contradicts a decision the *other* agent (or the other developer) made an hour earlier, because neither agent knows the other exists. With one developer this is a non-issue; with two developers each running an agent, it's the single biggest risk to this project staying coherent.

The fix isn't "communicate more" in the abstract — it's **give both agents the same persistent, versioned source of truth**, and structure the work so agents rarely need to touch the same code at the same time. That's what this doc sets up.

---

## 2. Staying in the Loop: This Is Not "Set the Agent Loose"

It's worth being explicit about this, because it changes how you'll actually use the prompts in this doc: the goal isn't maximum agent autonomy — it's **maximum build velocity while both of you still understand and can explain every part of the system**. An agent that silently produces a working feature you can't explain in a code review is a liability, not a productivity win, especially for a codebase two people need to jointly own and maintain.

Concretely, this means:

- **Agents explain before they implement.** For any non-trivial feature, the agent should walk through what it's about to build, why, what the alternatives were, and what trade-offs it's making — and wait for your go-ahead — before generating code. This mirrors how you'd want a senior engineer mentoring you to work, not a code-vending machine. The feature prompt template in §6 has this built in as an explicit step.
- **You review for understanding, not just correctness.** When reviewing your co-developer's PR (built with agent help), the bar isn't just "does this work" — it's "can I explain what this does and why." If you can't, that's a signal to ask, not to rubber-stamp.
- **Architectural and cross-cutting decisions are made by you two, not delegated to an agent.** Agents propose and explain; you decide. This is already reflected in §4.2 — shared/cross-cutting concerns require a sync between the two of you before an agent touches them.
- **It's fine to slow down on purpose.** If a feature is new territory for one of you, ask the agent to go step-by-step and explain the reasoning as it goes, the same way the original project brief asked for phased approval before moving forward. Speed is a means, not the goal — the actual goal is a system you both understand well enough to run and extend after the AI-assisted build phase is over.

---

## 3. The Core Mechanism: A Shared Context File

Both of you point your agent (Claude Code, Codex, whatever else) at the **same context file, committed to the repo**, at the start of every session. This is the "master prompt" — not a one-time prompt you type, but a living file the agent reads automatically.

**File:** `/AGENTS.md`. This is a deliberate choice, not just a name: **both Claude Code and the Codex CLI read a file called `AGENTS.md` in the repo root by convention**, so a single file automatically briefs whichever tool either of you is using that session — you don't need to remember to paste anything in, and you don't need a separate `CLAUDE.md`. If a tool you use later expects a differently-named file, symlink it to `AGENTS.md` rather than maintaining two copies.

This file should always contain:
- Project vision (one paragraph — pull from the PRD)
- Current architecture summary (stack, modular monolith boundaries, multi-tenancy approach)
- Coding standards (naming, folder structure, testing expectations)
- **Feature assignment rotation** (who's building what right now, updated as it changes — see §7 for the full sequence)
- **Current build status** (what's done, what's in progress, what's next — updated as work lands)
- Links to the full PRD, Tech Spec, and this guide for anything the agent needs beyond the summary

The master prompt template in §5 is what goes in this file. Treat it as a living document — every time you make an architectural decision together, it gets updated, and both agents pick up the change the next time either of you starts a session.

---

## 4. Day-to-Day Workflow

### 4.1 Ownership rotates by feature, not by module
Rather than permanently splitting modules between the two of you, **ownership rotates feature by feature, and each feature is built full-stack (backend + frontend) by one owner.** This is deliberate: if you split every feature into "backend half" and "frontend half" and hand them to different people, you both spend the project only ever seeing one side of the stack — which is the opposite of what you said you want. Owning a feature end-to-end, then handing the next one to your co-developer, means you each get real reps on both sides, with your co-developer's code (and PR review) as the other half of your learning loop. §7 lays out the full sequence in build order, with an Owner column for you to fill in, so you're not renegotiating the plan itself every week — just who's picking up which feature.

### 4.2 Starting a feature
1. Check §7 (or the current `AGENTS.md` status) for the next feature assigned to you.
2. Confirm it doesn't require changing a shared/cross-cutting concern (auth, tenant context, API conventions, design system) beyond what's already scoped. If it does, **sync with your co-developer first** — a two-line message, not a meeting.
3. Use the feature's prompt from §7 (or the template in §6 for anything not on the original list) to brief your agent — never just say "build feature X," since the agent has no memory of decisions made in the other developer's sessions beyond what's in `AGENTS.md`.
4. Work in a feature branch: `feature/<module>-<short-description>` (e.g. `feature/contributions-payment-reconciliation`).

### 4.3 Staying in sync
- **Before starting agent work each session:** pull latest `main`, skim the "Current build status" section of `AGENTS.md` for anything your co-developer landed since you last looked.
- **After landing a feature:** update the "Current build status" section yourself (or have your agent do it as part of the task — see the Definition of Done in §6) before opening the PR, so the other developer's next session starts from accurate context.
- **Daily/async check-in:** a short message (not necessarily a call) covering: what feature you touched, anything you changed that affects a shared concern, anything blocking you.
- **Weekly sync:** review `AGENTS.md` and the §7 sequence together — is it still the right order? Has the architecture actually evolved beyond what's documented? This is the point where PRD/Tech Spec updates happen, if needed.

### 4.4 Code review
- Every PR gets reviewed by the other developer before merge — treat this as reviewing **the agent's output**, not just the diff. Agents can introduce subtly wrong patterns (e.g. bypassing the tenant filter, inconsistent DTO mapping) that look fine in isolation.
- Since your co-developer built the *other* half of the stack on their features, reviewing their PR is also how you learn that half — read it like you're being taught, not just gatekeeping.
- PR description should reference which feature prompt was used (or link to it) so the reviewer has the same context the agent had.
- Cross-cutting/shared-concern changes require both of you to explicitly agree before merge, not just a standard review pass.

### 4.5 Git conventions
- `main` is always deployable.
- Feature branches off `main`, PR back into `main`.
- Commit messages reference the module: `[contributions] add payment reconciliation endpoint`.
- No direct pushes to `main`.

---

## 5. Master Prompt Template (goes in `AGENTS.md`)

```markdown
# KDS Club Manager — Agent Context

## Project
KDS Club Manager is a configurable, multi-tenant Club Operating System for
membership organisations (stokvels/investment clubs, sports clubs, burial
societies, business associations, etc.). One core platform; club-type
templates configure which modules/rules are active per club. See PRD.md
and TECH_SPEC.md in /docs for full detail — this file is a summary for
agent context, not the full spec.

## How We Want You To Work
We are two developers learning as we build this, not just outsourcing
implementation. For any non-trivial task:
1. Explain what you're about to build and why, in plain terms.
2. Explain the alternatives you considered and why you're recommending
   this approach.
3. Flag trade-offs and anything we should sanity-check.
4. Wait for explicit approval before writing code.
Trivial/mechanical tasks (boilerplate matching an existing pattern, a
straightforward CRUD endpoint identical in shape to one already built)
can skip the full explanation, but still summarize what you did
afterward. When in doubt, explain first.

## Stack
- Frontend: React 18 + TypeScript + Vite, Zustand (client state), TanStack
  Query (server state), React Router, Tailwind CSS, shadcn/ui
- Backend: Java 21, Spring Boot 3.x, Spring Security (JWT), Spring Data
  JPA, MapStruct, Flyway
- Database: PostgreSQL — shared schema, tenant-scoped via `club_id` +
  enforced TenantContext (see TECH_SPEC.md §3). NEVER write a query or
  repository method that skips tenant scoping.
- Storage: Supabase Storage via FileStorageService abstraction
- Deployment: Docker / Docker Compose, GitHub Actions CI

## Architecture
- Modular monolith. Modules: Identity/Tenancy, Members, Contributions,
  Meetings, Voting, Documents, Notifications, Audit, ClubTypeConfig.
- Modules talk to each other only through their public application-service
  interfaces — never reach into another module's repositories directly.
- API is REST, versioned (/api/v1), tenant resolved from JWT (not URL).
- Permissions (not role names) are what controllers check
  (`@PreAuthorize`); role→permission mapping lives in ClubTypeConfig.

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

## Feature Assignment (full-stack ownership rotates per feature — see
## TEAM_WORKFLOW.md §7 for the complete sequence and prompts)
- Current feature: [name, from §7]
- Owner this feature: [owner name]
- Next up: [name, owner]

## Current Build Status (update after every landed feature)
- Phase: [e.g. "Domain modelling complete, building Identity/Tenancy module"]
- Landed: [list]
- In progress: [list, with owner]
- Next up: [from §7 sequence]

## Do Not
- Do not introduce a new state-management library beyond Zustand/TanStack
  Query without a team discussion.
- Do not add a new top-level module without updating this file and the
  Tech Spec.
- Do not bypass tenant scoping "just for now."
```

---

## 6. Feature Prompt Template (per feature, used by whichever developer/agent builds it)

Use one of these per feature — never batch multiple features into one prompt, per the original phased-approval workflow. Fill in every section; an agent given a thin prompt will make assumptions that may not match what the other developer's agent assumed for a related feature.

```markdown
# Feature Prompt: <Feature Name>

## Context
- Module: <e.g. Contributions>
- Reference: AGENTS.md (always load this first), plus this feature's
  entry in TEAM_WORKFLOW.md §7 and the corresponding Epic/User Story in
  the feature breakdown doc
- Depends on: <e.g. "Members module member entity must exist">
- I am building this feature end-to-end (backend + frontend). My
  co-developer is working on <their current feature, from §7> — do not
  touch files under their feature's paths.

## Goal
<One or two sentences — what this feature does and why, from the user's
perspective>

## Requirements
- <Functional requirement 1>
- <Functional requirement 2>
- ...

## Constraints
- Follow AGENTS.md coding standards exactly.
- Tenant scoping is mandatory on every query — no exceptions.
- Do not modify files outside <module folder paths>. If a shared/
  cross-cutting file needs to change, stop and flag it instead of
  editing it.
- Match existing patterns in <reference file/module already built>.

## Existing Architecture (relevant slice)
<Paste or summarize the specific entities/services/endpoints this
feature touches or extends — not the whole system, just what's
relevant>

## Expected Files
- Backend: <e.g. ContributionScheduleController, ContributionService,
  ContributionRepository, ContributionScheduleDto, migration V<n>__...>
- Frontend: <e.g. features/contributions/ContributionScheduleForm.tsx,
  useContributionSchedule.ts (TanStack Query hook)>

## Acceptance Criteria
- <Given/when/then style criteria from the user story>

## Testing Requirements
- Unit tests for service-layer logic
- Integration test proving tenant isolation (cross-tenant request
  returns 403/404, never data)
- Frontend: basic render + interaction test for new components

## Workflow (do not skip)
1. Explain the feature conceptually and where it fits the architecture.
2. Explain which files/modules will be touched and why.
3. Explain the database/API changes at a high level.
4. Flag any trade-offs or open questions.
5. **Wait for my explicit approval before writing code.**
6. Implement, following the approved plan.
7. Summarize what was built and any deviations from the plan.

## Definition of Done
- [ ] Plan explained and approved before implementation (step 5 above)
- [ ] Code follows AGENTS.md standards
- [ ] Tests pass (unit + integration)
- [ ] No cross-module boundary violations
- [ ] I can explain what this code does and why, not just that it works
- [ ] AGENTS.md "Current Build Status" section updated
- [ ] PR opened against main with description referencing this prompt
```

---

## 7. Full Project Feature Prompt Sequence (Start to End)

This is the build order for the whole MVP. Each feature is built end-to-end — backend and frontend — by one owner, then reviewed by the other developer, so across the sequence you both rack up real time on both sides of the stack rather than settling into a permanent "backend person" / "frontend person" split. The **Owner** column is left blank on purpose — fill it in yourselves (in this doc and in `AGENTS.md`) as you assign or rotate features; alternating whole features is a reasonable default if you want one.

Treat the order as a strong default, not a law — dependencies matter more than any alternation pattern, so if one of you is mid-feature and blocked, don't stall the other waiting for a perfect handoff; pull the next independent item forward instead and re-sync via `AGENTS.md`.

### 7.1 Sequence Overview

| # | Phase | Feature | Owner | Depends on |
|---|---|---|---|---|
| 0 | Foundations | Repo scaffolding, Docker Compose, CI skeleton | *(other dev reviews)* | — |
| 1 | Foundations | Frontend app shell: layout, routing, design system, placeholder pages |  | #0 |
| 2 | Foundations | Auth: registration, login, JWT issue/refresh |  | #0, #1 |
| 3 | Foundations | Club (tenant) creation wizard + TenantContext |  | #2 |
| 4 | Foundations | Roles & permissions foundation + role management UI |  | #3 |
| 5 | Members | Member invite flow + directory |  | #4 |
| 6 | Members | Member status lifecycle + CSV bulk import |  | #5 |
| 7 | Contributions | Contribution schedule definition (admin) |  | #5 |
| 8 | Contributions | Payment tracking + member ledger |  | #7 |
| 9 | Contributions | Contribution reports (summary, export) |  | #8 |
| 10 | Meetings | Meeting scheduling + agenda |  | #5 |
| 11 | Meetings | RSVP + minutes capture/upload |  | #10 |
| 12 | Voting | Motion creation + voting window |  | #5 |
| 13 | Voting | Vote casting, tally, lock & publish |  | #12 |
| 14 | Documents | Document upload, storage, role-based access |  | #4 |
| 15 | Notifications | Notification service (in-app + email) + triggers |  | #8, #10, #13 |
| 16 | Audit | Audit logging infrastructure + admin viewer |  | #7, #10, #13 |
| 17 | ClubTypeConfig | Investment Club/Stokvel template wiring | *(both together)* | #4, #7–#14 |
| 18 | Reports | Cross-module reports dashboard |  | #9, #11, #13 |
| 19 | Hardening | Security & tenant-isolation review pass | *(paired review)* | all above |
| 20 | Deployment | Dockerized deploy + CI/CD to staging |  | #19 |
| 21 | Launch prep | Pilot onboarding polish (empty states, errors, copy) |  | #20 |

### 7.2 Feature Prompts

Each entry below is a filled-in starting point for that feature's prompt — copy it, adjust anything that's drifted since planning, and paste it to your agent. All of them assume the standard **Workflow** (explain → flag trade-offs → wait for approval → implement → summarize) and **Definition of Done** from the §6 template; only the feature-specific fields are spelled out here to keep this scannable. Fill in `<Depends-on file/service names>` once the prior feature actually lands, since exact class/file names may shift slightly from the plan.

---

**#0 — Repo Scaffolding, Docker Compose, CI Skeleton**

- **Goal:** stand up the monorepo (`/backend` Spring Boot skeleton, `/frontend` Vite+React skeleton), Docker Compose (Postgres + backend + frontend), and a GitHub Actions pipeline that runs lint/build/test on PRs.
- **Requirements:** Spring Boot app boots with a health endpoint; React app boots with a placeholder route; `docker-compose up` runs all three services; CI fails a PR on build or lint failure.
- **Files:** `/backend` (Maven/Gradle project, `Dockerfile`), `/frontend` (Vite project, `Dockerfile`), `docker-compose.yml`, `.github/workflows/ci.yml`, `/AGENTS.md` (initial version).
- **Acceptance:** a fresh clone + `docker-compose up` gives a working (empty) full-stack app locally; a PR with a broken build is blocked by CI.
- **Note:** this one's foundational enough that the other developer should review closely even if just one of you drives it — everything else depends on these conventions being right.

---

**#1 — Frontend App Shell, Design System & Navigation**

- **Goal:** build the skeleton every later frontend feature plugs into — layout, routing, and a consistent design system — before any real data or backend calls exist. This is deliberately backend-free, so it's a good first feature for getting comfortable with the agent workflow.
- **Requirements:** app layout (nav/sidebar + content area), React Router routes for every planned module with a placeholder page each (Members, Contributions, Meetings, Voting, Documents, Notifications, Audit, Reports), Tailwind + shadcn/ui theme setup (colors, typography, spacing scale), a reusable page-layout component, and shared loading/empty/error state components other features will reuse rather than reinvent.
- **Depends on:** #0 scaffolding.
- **Files (frontend):** `frontend/src/app/Layout.tsx`, `AppRouter.tsx`, `tailwind.config`, shadcn/ui setup under `components/ui/`, a placeholder `index.tsx` under `features/<module>/` for each module listed above.
- **Acceptance:** every placeholder route is reachable through the nav; the layout is usable on a phone-width screen; colors/fonts/spacing are defined once as design tokens and reused, never hardcoded per page.
- **Testing:** basic render tests for the layout and router; no integration tests needed yet since there's no backend involved.

---

**#2 — Auth: Registration, Login, JWT Issue/Refresh**

- **Goal:** users can register, log in, and stay logged in via short-lived access + refresh tokens.
- **Requirements:** email/password registration with BCrypt hashing; login issues JWT access token (~15 min) + refresh token; refresh endpoint rotates tokens; password reset flow (request + confirm).
- **Depends on:** #0 scaffolding, #1 frontend shell (build the auth forms inside it).
- **Files (backend):** `AuthController`, `AuthService`, `UserEntity`, `UserRepository`, Spring Security config, JWT filter/provider, Flyway migration for `users`.
- **Files (frontend):** `features/auth/LoginForm.tsx`, `RegisterForm.tsx`, `ForgotPasswordForm.tsx`, an auth API client + TanStack Query hooks, a Zustand auth-session slice.
- **Acceptance:** invalid credentials rejected with a clear error; valid login returns tokens and the SPA stores/refreshes them transparently; expired access token triggers a silent refresh, not a forced logout.
- **Testing:** unit tests on password hashing/validation and token generation; integration test for the full register→login→refresh flow.

---

**#3 — Club (Tenant) Creation Wizard + TenantContext**

- **Goal:** an authenticated user can create a club (tenant), and every subsequent request carries which club it's acting on behalf of.
- **Requirements:** club creation form (name, club type — hardcode "Investment Club" as the only option for now, full template system comes in #17); creator is auto-assigned an admin role; JWT gains a `clubId` claim on club selection (support users belonging to zero or more clubs).
- **Depends on:** #2 auth.
- **Files (backend):** `ClubController`, `ClubService`, `ClubEntity`, `ClubMembershipEntity`, `TenantContext` (thread-local, populated from JWT claim in a filter/interceptor), Flyway migration for `clubs`/`club_memberships`.
- **Files (frontend):** `features/clubs/CreateClubWizard.tsx`, a club-context provider/hook that exposes the active club app-wide.
- **Acceptance:** a request without a resolvable `clubId` on a tenant-scoped endpoint is rejected; a user with multiple clubs can switch active club and subsequent requests scope correctly.
- **Testing:** integration test specifically attempting a cross-tenant read (user of Club X requests Club Y's data) and confirming it's rejected — this pattern gets reused for every module from here on.

---

**#4 — Roles & Permissions Foundation + Role Management UI**

- **Goal:** a fixed permission set exists, roles are named bundles of permissions, and admins can assign roles to members.
- **Requirements:** seed the platform-wide permission set (from Tech Spec §4); default roles for the Investment Club type (Chairperson, Treasurer, Secretary, Member) each mapped to permissions; `@PreAuthorize` checks wired to permissions, not role names; admin UI to view/assign roles per member.
- **Depends on:** #3 tenancy.
- **Files (backend):** `PermissionEnum`/table, `RoleEntity`, `RolePermissionEntity`, `RoleService`, updated Spring Security method-security config, Flyway migration.
- **Files (frontend):** `features/roles/RoleManagement.tsx`, permission-aware UI helper (hide/show actions based on the session's resolved permissions).
- **Acceptance:** a Member-role user gets a 403 hitting a Treasurer-only endpoint; the UI never relies on role *names* to decide what to show, only resolved permissions.
- **Testing:** unit tests for role→permission resolution; integration tests covering at least one allow and one deny case per role.

---

**#5 — Member Invite Flow + Directory**

- **Goal:** admins can invite people to the club; invited people can accept and appear in a member directory.
- **Requirements:** invite by email, invite acceptance flow (creates/links a `UserEntity` + `ClubMembershipEntity`), member directory list view with search/filter, configurable profile fields per PRD §4.2 personas (start with a sane default field set for Investment Club).
- **Depends on:** #4 roles.
- **Files (backend):** `MemberController`, `MemberService`, invite token entity/table, email-sending hook (stub or real, see #15 for the full notification service — this can start with a simple direct email call).
- **Files (frontend):** `features/members/MemberDirectory.tsx`, `InviteMemberForm.tsx`, invite-acceptance page.
- **Acceptance:** an invited-but-not-yet-accepted member shows a distinct status in the directory; accepting an invite logs the user in and lands them in the club.
- **Testing:** integration test for the invite→accept lifecycle; tenant-isolation test (directory never leaks another club's members).

---

**#6 — Member Status Lifecycle + CSV Bulk Import**

- **Goal:** members can move through invited → active → suspended → exited, and existing club rosters can be imported in bulk.
- **Requirements:** status transition rules (who can trigger which transition, from Roles/Permissions); CSV upload with column mapping and validation preview before committing; import errors reported per-row, not as an all-or-nothing failure.
- **Depends on:** #5 member directory.
- **Files (backend):** status transition logic in `MemberService`, `MemberImportController`/`MemberImportService`, CSV parsing (e.g. via a lightweight library), audit trail hook (full Audit module lands in #16, but log the event locally for now).
- **Files (frontend):** `features/members/BulkImport.tsx` (upload, column mapping, preview, confirm), status-change controls in the directory.
- **Acceptance:** an invalid CSV row is reported with a clear reason and doesn't block the valid rows from importing; an exited member loses access on next request.
- **Testing:** unit tests for status transition rules; integration test for a mixed valid/invalid CSV import.

---

**#7 — Contribution Schedule Definition (Admin)**

- **Goal:** a treasurer can define what members are expected to pay and how often.
- **Requirements:** create/edit a contribution schedule (amount, frequency — monthly/once-off), assign to all members or a subset, view upcoming expected contributions.
- **Depends on:** #5 members.
- **Files (backend):** `ContributionScheduleController`, `ContributionScheduleService`, `ContributionScheduleEntity`, Flyway migration.
- **Files (frontend):** `features/contributions/ScheduleForm.tsx`, `features/contributions/ScheduleList.tsx`.
- **Acceptance:** a schedule change doesn't retroactively alter already-recorded payments (schedules are versioned or effective-dated, not mutated in place).
- **Testing:** unit tests for schedule calculation logic; tenant-isolation test.

---

**#8 — Payment Tracking + Member Ledger**

- **Goal:** treasurers can record payments received; every member can see their own running balance.
- **Requirements:** mark-as-paid workflow with optional proof-of-payment upload (uses the storage abstraction — coordinate with #14 if it hasn't landed yet, or stub direct storage access and refactor later), running ledger per member (expected vs. actual, balance).
- **Depends on:** #7 schedules.
- **Files (backend):** `PaymentController`, `PaymentService`, `PaymentEntity`, ledger calculation logic, migration.
- **Files (frontend):** `features/contributions/RecordPayment.tsx` (treasurer view), `features/contributions/MyLedger.tsx` (member view).
- **Acceptance:** a member can only ever see their own ledger, never another member's, regardless of role (this is a permissions + query-scoping check, not just a UI hide).
- **Testing:** integration test for the "member sees only their own ledger" rule specifically, plus the standard tenant-isolation test.

---

**#9 — Contribution Reports (Summary, Export)**

- **Goal:** treasurers can export a contribution summary for AGMs or audits.
- **Requirements:** summary report (total collected, by-member breakdown, outstanding balances) exportable as PDF and CSV.
- **Depends on:** #8 ledger.
- **Files (backend):** `ContributionReportService`, PDF/CSV generation (pick one library and use it consistently across all reports going forward — this sets the pattern #18 reuses).
- **Files (frontend):** `features/contributions/ReportExport.tsx`.
- **Acceptance:** exported figures match what's shown in-app to the cent; large clubs (hundreds of members) export without timing out (pagination/streaming if needed).
- **Testing:** unit test comparing report totals against seeded ledger data.

---

**#10 — Meeting Scheduling + Agenda**

- **Goal:** admins can schedule a meeting with an agenda; members can see upcoming meetings.
- **Requirements:** create/edit meeting (date, time, location/link, agenda items), list of upcoming and past meetings.
- **Depends on:** #5 members.
- **Files (backend):** `MeetingController`, `MeetingService`, `MeetingEntity`, `AgendaItemEntity`, migration.
- **Files (frontend):** `features/meetings/MeetingScheduler.tsx`, `features/meetings/MeetingList.tsx`.
- **Acceptance:** editing a meeting after it's scheduled notifies affected members (stub the notification call here if #15 hasn't landed yet — wire it for real once it has).
- **Testing:** unit tests for agenda ordering/edit logic; tenant-isolation test.

---

**#11 — RSVP + Minutes Capture/Upload**

- **Goal:** members can RSVP to meetings; admins can capture and publish minutes afterward.
- **Requirements:** RSVP (yes/no/maybe) per member per meeting with a live count for admins; minutes as rich text or file upload, linked to the meeting, published/visible per document access rules.
- **Depends on:** #10 meetings.
- **Files (backend):** `RsvpController`/`RsvpService`, minutes storage hook (coordinate with #14's `FileStorageService`), migration.
- **Files (frontend):** `features/meetings/RsvpControl.tsx`, `features/meetings/MinutesEditor.tsx`.
- **Acceptance:** minutes aren't visible to members until explicitly published; RSVP counts update without a full page reload.
- **Testing:** integration test for the draft→published minutes visibility rule.

---

**#12 — Motion Creation + Voting Window**

- **Goal:** admins can create a motion (poll) with options and a defined voting window, scoped to eligible voters.
- **Requirements:** motion creation (title, description, options, eligible-voter set, open/close time), automatic open/close based on window.
- **Depends on:** #5 members.
- **Files (backend):** `MotionController`, `MotionService`, `MotionEntity`, `MotionOptionEntity`, a scheduled job (or lazy check) for auto-closing expired windows, migration.
- **Files (frontend):** `features/voting/CreateMotion.tsx`.
- **Acceptance:** a motion can't be edited once it's open for voting (only before open or after explicit admin cancellation); ineligible members never see it as votable.
- **Testing:** unit tests for window state transitions.

---

**#13 — Vote Casting, Tally, Lock & Publish**

- **Goal:** eligible members cast votes; results are tallied, locked, and recorded once the window closes.
- **Requirements:** one vote per eligible member per motion, simple-majority tally (per PRD — extensible later), results locked and immutable once published, result feeds the audit log (stub locally if #16 hasn't landed yet).
- **Depends on:** #12 motions.
- **Files (backend):** `VoteController`, `VoteService`, `VoteEntity`, tally logic, migration.
- **Files (frontend):** `features/voting/CastVote.tsx`, `features/voting/MotionResults.tsx`.
- **Acceptance:** a duplicate vote attempt is rejected, not silently overwritten; results are unchangeable after lock, including by admins, short of a documented override path if you decide you need one — flag this as a design question rather than assuming.
- **Testing:** integration test for double-vote prevention and for post-lock immutability.

---

**#14 — Document Upload, Storage, Role-Based Access**

- **Goal:** clubs can store documents (constitution, policies, financial statements) with per-document visibility rules.
- **Requirements:** `FileStorageService` abstraction over Supabase Storage (per Tech Spec §6), upload/list/download, signed time-limited URLs, role-based visibility per document.
- **Depends on:** #4 roles.
- **Files (backend):** `DocumentController`, `DocumentService`, `FileStorageService` + Supabase implementation, `DocumentEntity`, migration.
- **Files (frontend):** `features/documents/DocumentLibrary.tsx`, `UploadDocument.tsx`.
- **Acceptance:** a member without visibility permission gets a 403 on direct URL guessing, not just a hidden UI element; files are namespaced by `clubId` in storage.
- **Testing:** integration test for a permission-denied download attempt.

---

**#15 — Notification Service (In-App + Email) + Triggers**

- **Goal:** key events across modules (payment reminder, meeting scheduled, vote open/closing, minutes published) generate notifications.
- **Requirements:** in-app notification feed, email delivery, an internal event/trigger mechanism other modules call into (so Contributions/Meetings/Voting don't each reinvent notification sending) — this is a good moment to introduce a simple internal event publisher if one doesn't exist yet.
- **Depends on:** #8, #10, #13 (needs real events to hook into; can start once at least one exists and extend as others land).
- **Files (backend):** `NotificationController`, `NotificationService`, `NotificationEntity`, email sending integration, an internal `DomainEventPublisher` (or similar) other modules call.
- **Files (frontend):** `features/notifications/NotificationCenter.tsx`, unread-count indicator.
- **Acceptance:** a triggering event still succeeds even if notification delivery fails (notifications are best-effort, not a hard dependency of the triggering action).
- **Testing:** unit tests per trigger type; a test proving triggering-action success is independent of notification delivery success.

---

**#16 — Audit Logging Infrastructure + Admin Viewer**

- **Goal:** financial and governance-relevant actions are recorded immutably and viewable by admins.
- **Requirements:** an audit log entry captures actor, action, timestamp, and relevant entity reference; write hooks added to the actions named in PRD §5.11 (payments recorded, votes cast/results, role changes, document uploads, member status changes — retrofit hooks into #7/#8/#13/#14/#6 as needed); admin-only viewer with filtering.
- **Depends on:** #7, #10, #13 (retrofits earlier modules — expect to touch files outside this feature's own folder, which is why it's flagged for a sync with your co-developer before starting).
- **Files (backend):** `AuditLogEntity`, `AuditLogService`, hooks in existing services, migration.
- **Files (frontend):** `features/audit/AuditLogViewer.tsx`.
- **Acceptance:** audit entries are append-only at the application layer (no update/delete endpoint exists); an admin can filter by actor, action type, and date range.
- **Testing:** integration test confirming a sampled set of trigger actions each produce exactly one audit entry.

---

**#17 — Investment Club/Stokvel Template Wiring**

- **Goal:** prove the "Club OS" thesis by formalizing everything built so far into a proper, swappable `ClubTypeConfig` template rather than hardcoded defaults.
- **Requirements:** extract the hardcoded Investment Club defaults (roles, terminology, enabled modules) from #3–#14 into a `ClubTypeConfig` template structure; club creation (#3) reads from this template instead of hardcoding.
- **Depends on:** most prior features — this is intentionally a joint, architecture-level feature. Do this one as a paired session (both of you + one agent session, or two agent sessions reviewed live together), not a solo hand-off — it touches shared/cross-cutting concerns by definition.
- **Files:** `ClubTypeConfigEntity`/definition, refactors across `ClubService`, `RoleService`, and any module with hardcoded Investment-Club-specific behavior.
- **Acceptance:** a second, minimal club type (even a throwaway test template) can be created via configuration alone, with zero new backend module code — this is the actual acceptance test for the whole platform thesis, not just this feature.
- **Testing:** the "second club type via config only" acceptance criterion above, run as an explicit integration test.

---

**#18 — Cross-Module Reports Dashboard**

- **Goal:** a single reports view surfaces the exports already built (contributions, member list, meeting/voting history) in one place.
- **Requirements:** dashboard listing available reports per the modules already shipped, consistent export UX reusing the PDF/CSV pattern from #9.
- **Depends on:** #9, #11, #13.
- **Files:** `features/reports/ReportsDashboard.tsx`, any backend aggregation endpoint needed beyond what #9 already exposes.
- **Acceptance:** no duplicated report-generation logic — this wraps existing per-module report endpoints, it doesn't reimplement them.

---

**#19 — Security & Tenant-Isolation Review Pass**

- **Goal:** a dedicated pass across the whole codebase specifically hunting for tenant-scoping gaps, missing permission checks, and other issues easy to miss feature-by-feature.
- **Requirements:** audit every repository method for tenant scoping; audit every controller endpoint for a permission check; review rate limiting on auth endpoints; review secrets handling.
- **Depends on:** everything above.
- **Approach:** this is a checklist-driven pass, not a "build something new" prompt — brief the agent to enumerate every repository/controller and flag anything missing the expected pattern, then the two of you review the flagged list together rather than letting the agent silently "fix" security-sensitive code unsupervised.
- **Acceptance:** a written list of findings, each either fixed with a linked PR or explicitly accepted as a known/deferred risk.

---

**#20 — Dockerized Deployment + CI/CD to Staging**

- **Goal:** a repeatable path from a merged PR to a running staging environment.
- **Requirements:** production-shaped Docker images, environment-based config (no secrets in images), a CI/CD pipeline stage that deploys `main` to staging automatically.
- **Depends on:** #19.
- **Files:** deployment-specific Dockerfiles/compose overrides, `.github/workflows/deploy.yml`, environment config docs.
- **Acceptance:** a fresh deploy from a clean `main` succeeds without manual steps beyond documented one-time setup (DB provisioning, secret configuration).

---

**#21 — Pilot Onboarding Polish**

- **Goal:** the product is ready to hand to a real pilot club without you sitting next to them.
- **Requirements:** empty states for every list view, clear error messages (no raw stack traces or generic "something went wrong"), a lightweight onboarding checklist/tour for a first-time club admin.
- **Depends on:** #20.
- **Files:** touches across `/frontend/features/*` for empty/error states; a new onboarding checklist component.
- **Acceptance:** a non-technical tester can go from "just invited" to "understands the four or five core actions available to them" without you explaining anything live.

---

## 8. Suggested Repo Layout (supports the above)

```
/docs
  PRD.md
  TECH_SPEC.md
  TEAM_WORKFLOW.md          <- this doc
  feature-prompts/           <- optional: save filled-in prompts here for history
/AGENTS.md                   <- master prompt / living context file
/CLAUDE.md                   <- identical to AGENTS.md (or symlink), for Claude Code
/backend
  /members
  /contributions
  /meetings
  /voting
  /documents
  /notifications
  /audit
  /identity-tenancy
  /club-type-config
/frontend
  /features
    /members
    /contributions
    /meetings
    /voting
    /documents
```

Keeping `docs/` and the module folders mirrored between backend and frontend means an agent (or a human) can find "everything about Contributions" in predictable places, regardless of which side of the stack it's working on.

---

## Next Step

This gives you a working process and a full build sequence from day one through pilot-ready. As you actually start building, expect the §7 order to shift a little around real dependencies — that's fine, just keep `AGENTS.md` honest about what actually happened. Treat this doc itself as living — update it the same way you'll update `AGENTS.md`.

Fill in the **Owner** column in §7.1 (and keep `AGENTS.md` matching it) before you start #1 — who does what doesn't really matter much here, so alternating whole features between you is a reasonable default if you don't already have a preference. The one thing worth deciding early is whether #17's "second club type" acceptance test should target a real second vertical (e.g. Sports Club) or a throwaway test template.

Let me know if you want to adjust the sequence, the prompt templates, or move on to **Phase 2 — Domain Modelling** next.
