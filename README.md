# KDS Club Manager

KDS Club Manager is a configurable, multi-tenant operating system for
membership organisations. The repository contains a Spring Boot API and a
responsive React application.

## Run with Docker

Prerequisites: Docker Desktop with Docker Compose.

```powershell
if (!(Test-Path .env)) { Copy-Item .env.example .env }
docker compose up --build
```

The local services are available at:

- Frontend: <http://localhost:5175>
- Backend health: <http://localhost:8080/actuator/health>
- PostgreSQL: `localhost:5432`

The defaults in `.env.example` are intended only for local development. Change
all credentials before using the containers outside a local environment.

Stop the services with:

```powershell
docker compose down
```

Add `--volumes` only when you intentionally want to remove the local PostgreSQL
data volume as well.

## Run the frontend from Bash (development mode)

Both modes use **http://localhost:5175**. Run only one frontend at a time.
For an existing `.env`, set `FRONTEND_PORT=5175`; any `FRONTEND_ORIGINS`
or `FRONTEND_URL` overrides must also use port 5175. Do not replace your
existing secrets by copying `.env.example` over `.env`.

From the project root in Git Bash:

```bash
docker compose stop frontend
docker compose up -d --build backend
cd frontend
npm ci
npm run dev
```

Vite forwards `/api` requests to the backend on port 8080, just as Docker's
Nginx proxy does. PostgreSQL and the backend remain in Docker. Vite binds
to IPv4 loopback and refuses to select a different port if 5175 is occupied.
If startup fails, stop the other frontend rather than changing the URL.

To switch back to Docker, press `Ctrl+C` in the Vite terminal, return to
the project root, and run `docker compose up -d --build`.

Password-reset links also use http://localhost:5175. The local development
reset adapter prints links to `docker compose logs backend`; it does not
send real email.

## Create and switch clubs (Feature 3)

After rebuilding the backend and frontend, open http://localhost:5175 and
register or log in. You will arrive at **Your clubs**:

1. Enter a club name. Investment Club is currently the only type.
2. Select **Review club**, then **Create club**. You become its administrator
   and enter its workspace.
3. Use **Switch or create club** in the workspace header to create another
   club or open an existing one.

Reloading keeps the selected club through the refresh session. A fresh login
asks you to select a club again. If membership/session access is rejected,
sign in again. For now, test session switching in one browser tab; cross-tab
refresh coordination is not implemented.

Flyway applies the new club tables automatically; no database reset is needed.
Existing module pages remain placeholders, not live member/contribution data.

## Roles and permissions (Feature 4)

Rebuild with `docker compose up -d --build`, reload the frontend, log in and
select your club. Existing club creators retain Administrator access through
the migration. No database reset is needed.

Open **Roles** in the sidebar to view the role catalog. Administrators can
select a role beside an existing membership and click **Save role**.
Chairpersons can view the catalog but cannot assign roles. Other roles
do not see the Roles page. You cannot remove the last administrator.
To transfer management access, first assign Administrator to another member.

Each membership currently has one role. Roles are Administrator, Chairperson,
Treasurer, Secretary and Member. Custom role editing and invitations are not
part of this feature; until Feature 5, a newly created club contains only its
creator. Do not modify the database manually to test production memberships.

Backend permission removal applies on subsequent requests even with an older
JWT. Another user's UI updates when their session refreshes or they log in
again. The 8-character password minimum and port 5175 are unchanged.

## Member invitations and directory (Feature 5)

Open **Members** after selecting a club. Users with `MEMBERS_WRITE` can invite
a person using their name, email, and optional phone number. Invitations join
with the Member role; administrators can change the role after acceptance.
The directory distinguishes pending invitations from active memberships and
supports name, email, phone, and status filtering.

Local Docker development prints invitation links to the output of
`docker compose logs backend`. Following the link creates an account when the email is new, or
links the club to an existing account. Acceptance signs the person in and
selects the invited club. Links expire after seven days and can be used once.
Real email delivery remains part of the later notification feature.

## Contribution schedules (Feature 7)

Open **Contributions** after selecting a club. Administrators and Treasurers
can create monthly or once-off schedules, choose all currently active members
or a selected subset, and view upcoming expected contributions. Other active
members have a read-only view.

Assignments are snapshots: a member joining later is not silently added.
Choose **Create revision** to change terms or assignments. The new terms start
on the selected effective date and the previous revision is retained; existing
or future payment records are therefore not rewritten. Schedule dates cannot
start in the past, the amount supports two decimal places, and the current
currency is ZAR.

Flyway migration V6 creates the schedule, version, and assignment tables. No
manual database changes are required. Tenant and permission checks are applied
on the server even when controls are hidden in the browser.

## Payment tracking and member ledger (Feature 8)

Administrators and Treasurers can allocate a full or partial receipt to an
outstanding member contribution from **Contributions**. A reference and note
are optional, as is a PDF/JPEG/PNG proof of at most 1 MB. Proof bytes go through
`FileStorageService`; local development stores them below `KDS_STORAGE_ROOT`
(or the operating-system temporary directory when it is unset).

Every active member sees **My ledger**, with expected, paid, outstanding, and
chronological running balances. That API derives the membership from the
authenticated user and current club; it has no member-ID input that could be
changed to view somebody else's ledger. Payment and schedule queries also
apply explicit `club_id` predicates. Flyway migration V7 creates the payment
table and indexes.

## Run checks without Docker

Backend (Java 21):

```powershell
Set-Location backend
.\mvnw.cmd verify
```

Frontend (Node.js 24):

```powershell
Set-Location frontend
npm ci
npm run lint
npm run test:run
npm run build
```

The GitHub Actions workflow runs the same backend and frontend checks for pull
requests and pushes to `main`.
