# Feature 19 Security and Tenant-Isolation Review

Status: findings awaiting team review  
Branch: `feature/security-review`  
Baseline reviewed: `main` at `3903b74`

## Review method

This review traced each HTTP endpoint through its application service and repository boundary. It also checked authentication and session handling, tenant-context establishment, file storage, runtime configuration, tracked secret-like files, and the container proxy. A finding stays open until the team approves a fix or records an explicit acceptance or deferral.

## Findings requiring a decision

| ID | Severity | Status | Finding | Evidence and risk | Recommended disposition |
|---|---|---|---|---|---|
| SEC-19-01 | High | Review required | Public authentication and invitation endpoints have no abuse throttling. | `FoundationSecurityConfiguration` permits register, login, refresh, logout, password-reset request/confirm, and invitation acceptance. There is no rate-limiter dependency, filter, service, or test. Attackers can make unrestricted credential-stuffing, account-creation, reset-token guessing, and invitation-token guessing attempts. | Add application-level limits keyed by source IP plus normalized account identifier where available, return `429` with a generic response, and add integration tests. Preserve a deployment-layer limit as an additional control, not the only control. |
| SEC-19-02 | High | Review required | The default Compose path is development-friendly but does not fail closed for non-local deployment. | `docker-compose.yml` supplies a repository-known JWT signing secret and database password, enables raw reset/invitation token delivery, uses an insecure refresh cookie, and serves plain HTTP unless every relevant variable is overridden. Reusing this Compose file for staging would allow token forgery and expose credential links in logs. | Keep convenient local defaults in an explicit development profile. Add a production/staging profile that requires injected secrets, secure cookies and real delivery providers, and fails startup when development delivery or known defaults are active. Coordinate the deployment-specific part with Feature 20. |
| SEC-19-03 | Medium | Review required | Several tenant-owned repository writes do not enforce tenant ownership at the repository boundary. | `VoteRepository.add`, `MotionResultRepository.addAll`, and `MemberRepository.saveInvitation` persist a caller-supplied `clubId` without comparing it with the current tenant. `MemberRepository.saveProfile` is used by the secret-authorized public invitation flow and has no explicit expected-club guard. Current services construct the entities safely, so no present cross-tenant exploit was found, but a future caller mistake could persist data into another tenant. | Add the standard `TenantContext` guard to request-scoped writes. For public invitation acceptance, require the validated invitation's club ID explicitly and document that secret-validation boundary. Add negative repository/service tests. |
| SEC-19-04 | Low | Review required | Registration reveals whether an email address already has an account. | `AuthService.register` raises `EmailAlreadyRegisteredException`; `ApiExceptionHandler` returns a distinct `409` and message. Login and password-reset request correctly use generic responses. Enumeration can support targeted phishing or credential attacks. | Decide whether the current user experience is worth the disclosure. Prefer a generic registration response or an email-based existing-account flow; at minimum, cover it with SEC-19-01 throttling. |
| SEC-19-05 | Low | Review required | The Nginx frontend has no browser hardening headers. | `frontend/nginx.conf` does not set a Content Security Policy, clickjacking protection, MIME-sniffing protection, referrer policy, or permissions policy. This does not create an observed tenant bypass, but weakens defense in depth against browser-side attacks. | Add a tested header policy. Enable HSTS only at the TLS-terminating production layer so local HTTP remains usable. This can be completed with Feature 20 if explicitly deferred here. |

## Repository tenant-scope inventory

| Repository | Classification | Result |
|---|---|---|
| `AuditLogRepository` | Tenant data | Reads always add `clubId`; append verifies the current tenant. Pass. |
| `ContributionPaymentRepository` | Tenant data | Reads include `clubId`; writes verify the current tenant. Pass. |
| `ContributionScheduleRepository` | Tenant data | Schedule, version and assignment reads include `clubId`; writes verify/stamp the current tenant. Pass. |
| `DocumentRepository` | Tenant data | List/find/lock reads include `clubId`; writes verify the current tenant. Pass. |
| `ClubAccessRepository` | Identity bootstrap/system | User-facing membership reads bind both user and club. `clubName` is limited to a previously validated invitation; `allClubIds` is limited to the background-job boundary. Accepted scoped exceptions. |
| `CurrentClubRepository` | Tenant data | Lookup binds requested ID and current tenant ID. Pass. |
| `MemberIdentityDirectoryRepository` | Tenant data | All directory and membership reads include the current tenant. Pass. |
| `MembershipLifecycleRepository` | Tenant data | Locks, lists, lookups and updates include the current tenant. Pass. |
| `PasswordResetTokenRepository` | Global identity | Token hash and user-owned cleanup are intentionally global and do not contain club data. Pass. |
| `RefreshTokenRepository` | Global identity | Token hash, owner, family and user operations are intentionally global and do not contain club data. Pass. |
| `RoleMembershipRepository` | Tenant data | Reads and target updates include the current tenant. Pass. |
| `UserRepository` | Global identity | User ID/email operations are intentionally global and do not contain club data. Pass. |
| `MeetingParticipationRepository` | Tenant data | RSVP/minutes reads include `clubId`; writes verify the current tenant. Pass. |
| `MeetingRepository` | Tenant data | Reads include `clubId`; writes verify the current tenant. Pass. |
| `MemberRepository` | Tenant data plus invitation-secret bootstrap | Tenant reads are scoped. Token-hash lookups are an intentional pre-tenant boundary. Write guards need a decision under SEC-19-03. |
| `NotificationRepository` | Tenant data | Reads/updates include `clubId` and membership; writes verify the current tenant. Pass. |
| `MotionRepository` | Tenant data | Reads/locks include `clubId`; writes verify the current tenant. Pass. |
| `MotionResultRepository` | Tenant data | Reads include `clubId`; result writes need a decision under SEC-19-03. |
| `VoteRepository` | Tenant data | Reads include `clubId`; vote writes need a decision under SEC-19-03. |

No unscoped tenant-data read was found in the 19 repositories. The four global-identity/bootstrap exceptions above are deliberate boundaries rather than general tenant repositories.

## Controller authorization inventory

| Controller | Endpoints reviewed | Protection result |
|---|---|---|
| `AuditLogController` | query audit log | `AUDIT_READ`; pass. |
| `ContributionPaymentController` | record payment, expectations, own ledger, reminders | `CONTRIBUTIONS_WRITE`/`CONTRIBUTIONS_READ`; pass. |
| `ContributionReportController` | summary and export | `REPORTS_READ`; pass. |
| `ContributionScheduleController` | list, assignable members, create, revise, upcoming | appropriate contribution read/write permissions; pass. |
| `DocumentController` | list, upload, edit, new version, download | `DOCUMENTS_READ`/`DOCUMENTS_MANAGE`; service also applies role visibility; pass. |
| `AuthController` | register, login, refresh, logout, select club, reset request/confirm | Public endpoints are intentional; select-club requires authentication globally. Abuse control is open under SEC-19-01. |
| `ClubController` | club types, create/list clubs, current club | Create/list/type endpoints require authentication globally and intentionally work before tenant selection; current club also passes the tenant filter. Pass. |
| `RoleController` | roles, role members, assignment, current permissions | `ROLES_READ`, `ROLES_MANAGE`, or authenticated current-user access; pass. |
| `MeetingController` | list, create, edit | `MEETINGS_READ`/`MEETINGS_WRITE`; pass. |
| `MinutesController` | read, edit, attach, publish, download | `MEETINGS_READ`/`MEETINGS_WRITE`; service protects unpublished content; pass. |
| `RsvpController` | own response and manager counts | `MEETINGS_READ`; service exposes manager-only counts only to writers; pass. |
| `MemberController` | directory, invite, status, invitation preview/accept | member permissions on tenant operations; public token endpoints are intentional. Abuse control is open under SEC-19-01. |
| `MemberImportController` | inspect, preview, confirm | `MEMBERS_WRITE`; pass. |
| `NotificationController` | feed, unread count, mark read/all | authenticated at class level; repository binds current membership and tenant. Pass. |
| `ReportController` | member/meeting/voting export | `REPORTS_READ`; pass. |
| `MotionController` | list, create, edit, cancel | `VOTES_READ`/`VOTES_CREATE`; pass. |
| `VoteController` | cast, view result, publish result | `VOTES_CAST`, `VOTES_READ`, `VOTES_CREATE`; service protects unpublished tallies. Pass. |

All 17 controllers and their mapped endpoints were reviewed. Tenant requests pass through `TenantContextFilter`, which reloads active membership and permissions from the database before method authorization. This prevents a suspended member or removed role from retaining access until an access token expires.

## Controls verified

- Access tokens use issuer-validated HS256 JWTs and reject signing keys shorter than 32 bytes.
- Passwords use BCrypt with cost 12; inbound password length is capped for predictable hashing cost.
- Refresh tokens and reset/invitation tokens are stored as hashes. Refresh tokens rotate, and reuse revokes the token family.
- The refresh cookie is HTTP-only, `SameSite=Strict`, and scoped to `/api/v1/auth`; the secure flag is configurable.
- Login errors and password-reset requests do not disclose whether an account exists.
- CORS uses an explicit configured origin list and does not use a wildcard with credentials.
- Uploaded document/minutes types and sizes are allow-listed; local and Supabase storage keys are tenant-prefixed and traversal-checked.
- Health details are hidden. CI receives read-only repository permissions. No private key, certificate, credential file, or populated `.env` is tracked.

## Decision log

Record one of `Fix in Feature 19`, `Accepted`, or `Deferred to <feature/issue>` for every finding before Feature 19 is marked complete.

| Finding | Decision | Owner/link | Notes |
|---|---|---|---|
| SEC-19-01 | Pending | | |
| SEC-19-02 | Pending | | |
| SEC-19-03 | Pending | | |
| SEC-19-04 | Pending | | |
| SEC-19-05 | Pending | | |
