# KDS Club Manager — Product Requirements Document (PRD)

**Version:** 1.0 (Draft)
**Status:** Planning — Phase 1
**Date:** August 2026

---

## 1. Vision

KDS Club Manager is a **Club Operating System (Club OS)** — a single, configurable platform that gives any membership-based organisation the digital infrastructure it needs to run itself: members, money, meetings, votes, documents, and communication.

Rather than building a one-off app for a single type of club (e.g. "a stokvel app"), KDS Club Manager is built as a **core platform with pluggable configuration**. A "club type" is a template that turns core building blocks on or off and applies specific business rules. This means the same underlying product can serve a stokvel, a sports club, a burial society, or a business association — without forking the codebase.

**Why this matters:** most club/society software in South Africa (and similar markets) is either a single-purpose app (stokvel-only) or a generic spreadsheet/WhatsApp workflow. Neither scales into a real SaaS business. A configurable Club OS lets you capture many verticals with one product, one codebase, and one go-to-market motion.

---

## 2. Problem Statement

Membership organisations — especially informal or semi-formal ones common in South Africa — currently manage their operations through a patchwork of WhatsApp groups, spreadsheets, cash books, and manual meeting minutes. This causes:

- **Poor financial transparency** — contributions, payouts, and balances are hard to track and dispute.
- **No institutional memory** — when a treasurer or secretary leaves, records often leave with them.
- **Weak governance** — votes, meeting minutes, and rule changes aren't formally recorded.
- **No trust infrastructure** — new or prospective members can't easily verify a club's legitimacy or history.
- **Fragmented tooling** — a club typically needs 3–4 different tools (banking app, WhatsApp, Excel, physical minute book) instead of one system.

Existing point solutions solve this for a single vertical (e.g. stokvel-only apps) but don't generalise, so every new club type requires a new product.

---

## 3. Goals

1. Provide a single platform where any club can manage members, money, meetings, votes, documents, and communication.
2. Make a new "club type" (vertical) configurable via templates rather than custom code.
3. Establish a multi-tenant architecture that can scale from a handful of pilot clubs to thousands of clubs on shared infrastructure.
4. Provide strong auditability and financial transparency as a first-class feature, not an afterthought.
5. Ship a genuinely usable MVP for **one or two initial club types** (e.g. investment clubs/stokvels first) while proving the platform generalises to a second type early, to validate the "OS" thesis.

## 3.1 Non-Goals (for MVP)

- Not building a payments/banking rail in-house (MVP will track contributions and reconcile manually or via simple integrations, not become a payment processor).
- Not supporting fully custom, code-level club types in the MVP — configuration is template/flag-based, not a plugin marketplace.
- Not building native mobile apps in the MVP (responsive web first).
- Not building public marketplace/discovery features for clubs in the MVP (that's a later roadmap item).
- Not handling multi-currency or multi-country regulatory compliance in the MVP.

---

## 4. Target Users

### Primary
- **Club administrators / committee members** (chairperson, treasurer, secretary) — the power users who configure and run the club day-to-day.
- **Club members** — consumers of the platform: view balances, vote, RSVP to meetings, read documents.

### Secondary
- **Club founders evaluating software** — people deciding whether to digitise their club at all.
- **Platform operator (you)** — needs tenant-level visibility, billing, and support tooling eventually.

## 4.1 Personas

**1. Thandiwe — Stokvel Treasurer**
Manages a 20-member investment club. Currently tracks contributions in a notebook and WhatsApp. Needs: clear ledger per member, automated reminders, payout calculations, exportable reports for AGM.

**2. Sipho — Sports Club Chairperson**
Runs a 60-member running club. Needs: event/meeting scheduling, membership renewals, basic voting for committee elections, communication broadcast.

**3. Nomvula — Burial Society Secretary**
Needs strict record-keeping (who paid, who's covered, next of kin details), document storage (constitution, policies), and formal meeting minutes for legal/dispute purposes.

**4. Regular Member — Lindiwe**
Just wants to see her contribution history, upcoming meetings, and vote when asked — low friction, mobile-first, minimal learning curve.

## 4.2 User Journeys (representative)

**Journey A — Club Onboarding**
Admin signs up → selects club type (e.g. "Investment Club") → platform provisions a tenant with the relevant template (features, roles, terminology) enabled → admin invites members → members accept invite and set up accounts.

**Journey B — Monthly Contribution Cycle**
Treasurer opens Contributions module → sees list of members and expected amounts → marks payments received (manually or via reconciliation) → system updates member ledgers → members get notified of updated balance.

**Journey C — Calling a Vote**
Admin creates a motion (e.g. "Approve new member application") → sets voting window and eligible voters → members receive notification → members cast vote → system tallies and locks results → outcome is recorded in the audit log and minutes.

**Journey D — Meeting Lifecycle**
Admin schedules a meeting → agenda items added → members RSVP → meeting happens → minutes are captured/uploaded → minutes and any resolutions are archived and linked to relevant records (e.g. a vote taken in that meeting).

---

## 5. Functional Requirements

Grouped by module. Each module is part of the shared core; club-type templates decide which parts are visible/required.

### 5.1 Authentication & Identity
- Email/password registration and login, JWT-based sessions.
- Password reset flow.
- Invite-based member onboarding (admin invites via email/phone).
- (Future) SSO/social login.

### 5.2 Multi-Tenancy & Club Management
- A "Club" is a tenant. One user can belong to multiple clubs (multi-club membership).
- Club creation wizard: choose club type/template, name, basic settings.
- Club-level settings: branding basics, terminology overrides, enabled modules.

### 5.3 Roles & Permissions
- Configurable roles per club type (e.g. Chairperson, Treasurer, Secretary, Member) mapped to a fixed set of underlying permissions.
- Role assignment and reassignment by admins.
- Permission checks enforced on both API and UI.

### 5.4 Member Management
- Member directory with profile info (configurable fields per club type).
- Member status lifecycle (invited → active → suspended → exited).
- Bulk import (CSV) for onboarding existing club member lists.

### 5.5 Contributions / Fees
- Define contribution schedules (e.g. monthly amount, once-off joining fee).
- Track expected vs. actual payments per member.
- Manual reconciliation workflow (mark-as-paid, upload proof).
- Running balance / ledger per member.
- Basic reporting (who's behind, total collected this period).

### 5.6 Meetings
- Schedule meetings with agenda items.
- RSVP tracking.
- Minutes capture (rich text or file upload) linked to the meeting.
- Meeting history archive.

### 5.7 Voting
- Create a motion/poll with options, eligible voters, and a voting window.
- Cast and tally votes (support simple majority initially; extensible for other rules later).
- Lock and publish results; results feed the audit log.

### 5.8 Documents
- Upload and organise club documents (constitution, policies, minutes, financial statements).
- Access control per document (role-based visibility).
- Versioning (at minimum, replace-with-history).

### 5.9 Notifications
- In-app notifications for key events (payment reminder, meeting scheduled, vote open/closing, minutes published).
- Email notifications for the same (SMS/WhatsApp as future roadmap).

### 5.10 Reports
- Standard exportable reports: contribution summary, member list, meeting/voting history.
- Export to PDF/CSV.

### 5.11 Audit Log
- Immutable log of key actions: payments recorded, votes cast/results, role changes, document uploads, member status changes.
- Viewable by admins for transparency and dispute resolution.

### 5.12 Workflows / Templates (Club Type Configuration)
- A "club type" defines: enabled modules, custom terminology (e.g. "Contribution" vs "Membership Fee" vs "Subs"), custom fields, default roles, and default rule sets (e.g. voting majority threshold).
- Platform-level admin can create/edit club type templates; club admins select and lightly customise from a template.

---

## 6. Non-Functional Requirements

- **Multi-tenancy & data isolation:** one club's data must never be visible to another club, enforced at the data-access layer, not just the UI.
- **Security:** JWT auth, role-based authorization, encrypted secrets, HTTPS everywhere, input validation, protection against OWASP Top 10 classes of issues.
- **Auditability:** financial and governance actions must be traceable to a user, timestamp, and (where relevant) an IP/context.
- **Performance:** typical club interactions (dashboard load, ledger view) should respond within ~300–500ms server-side under normal load for MVP scale.
- **Reliability:** automated backups of the database; documented restore process.
- **Maintainability:** modular monolith with clear module boundaries so modules can later be extracted into services without a rewrite.
- **Usability:** admin flows should be usable by non-technical committee members; member-facing flows must work well on mobile browsers.
- **Extensibility:** adding a new club type should require configuration, not new backend modules, for the majority of cases.
- **Observability:** structured logging and basic monitoring/alerting from day one, even if lightweight initially.

---

## 7. Risks & Assumptions

### Risks
- **Scope creep from "generalising too early"** — trying to support every club type before validating even one could delay MVP indefinitely. *Mitigation:* build for one vertical deeply first (stokvel), design the abstraction, then validate with a second vertical.
- **Financial trust** — clubs are dealing with real money; a bug or data loss around contributions is reputationally severe. *Mitigation:* strong audit logging, no direct money movement in MVP (tracking only, not custody).
- **Multi-tenancy bugs** — a data isolation bug is catastrophic (one club seeing another's data). *Mitigation:* enforce tenant scoping at the persistence layer with tests specifically targeting cross-tenant access.
- **Adoption friction** — non-technical committee members may resist moving off WhatsApp/Excel. *Mitigation:* prioritise low-friction onboarding and mobile-first member experience.
- **Coordination risk (two devs + AI coding agents)** — with two developers each driving agentic tools (Claude Code/Codex), there's a real risk of inconsistent architecture, duplicated work, or conflicting changes if agents aren't working from the same shared context. *Mitigation:* temporary full-stack feature ownership aligned to the modular monolith boundaries, a shared master prompt both developers' agents are seeded with, and a synced project-state document updated as work lands (see the companion Team Workflow & AI Agent Collaboration Guide).

### Assumptions
- Initial target market is South African membership clubs, starting with stokvels/investment clubs.
- MVP does not need to move real money — contribution tracking is sufficient for v1 trust-building.
- A small team of **two developers** is building this, using agentic engineering tools (Claude Code / Codex) to accelerate delivery, so build order must prioritise clear ownership of each active full-stack feature and staying in sync alongside learning and incremental delivery.

---

## 8. Success Metrics

**MVP validation metrics (qualitative + early quantitative):**
- Number of pilot clubs onboarded and actively using the platform for a full contribution cycle.
- % of members within a pilot club who log in and interact monthly.
- Reduction in "who paid / who didn't" disputes reported by pilot treasurers (qualitative feedback).
- Successful onboarding of a **second club type** using only configuration (proves the "OS" thesis) within a defined timeframe post-MVP.

**Later-stage SaaS metrics (for reference, not MVP-blocking):**
- Number of active tenants (clubs).
- Monthly active members per club.
- Retention/churn of clubs across contribution cycles.
- Revenue per club (once monetisation is introduced).

---

## 9. MVP Scope

**In scope for MVP:**
- Auth, multi-tenancy, roles/permissions (core).
- Member management (directory, invite, status).
- One fully-built club type template: **Investment Club / Stokvel**.
- Contributions module (schedules, tracking, ledger, basic reports).
- Meetings module (schedule, agenda, RSVP, minutes).
- Voting module (basic motions, simple majority).
- Documents module (upload, role-based access).
- Notifications (in-app + email) for core events.
- Audit log for financial and governance actions.
- Admin web app + member web app (responsive, same codebase, role-based views).

**Explicitly deferred (see roadmap):**
- Second and third club type templates (post-MVP validation step).
- Payment gateway integration.
- Mobile native apps.
- SMS/WhatsApp notifications.
- Advanced voting rules (weighted votes, quorum logic beyond simple majority).
- Public club directory/marketplace.
- Billing/subscription management for the platform itself.

---

## 10. Future Roadmap (indicative, not committed)

1. **Post-MVP:** Add a second club type template (e.g. Sports Club or Burial Society) using only configuration — validates the platform thesis.
2. Payment integration (e.g. payment gateway or open banking reconciliation) for contributions.
3. Platform-level billing (subscription tiers for clubs).
4. SMS/WhatsApp notification channels (high relevance for the target market).
5. Advanced governance rules (quorum, weighted voting, multi-stage approvals).
6. Native mobile apps.
7. Public-facing club directory / discovery (optional, depends on business model).
8. Microservice extraction for modules under heaviest load (likely Contributions and Notifications first).

---

## Next Step

This PRD is a first draft covering Phase 1. Before moving to Phase 2 (Domain Modelling), I'd suggest we review:
- Whether the MVP club type should indeed be **Investment Club/Stokvel** first, or a different vertical.
- Whether any Non-Goals need to move into MVP scope.
- Whether the success metrics feel right for how you'll actually validate this.

Let me know what you'd like to adjust, or say "approved" and we'll move to the Tech Spec / Phase 2.
