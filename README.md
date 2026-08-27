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
Full role/permission management is the next feature. Existing module pages
remain placeholders, not live member/contribution data.

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
