# KDS Club Manager

KDS Club Manager is a configurable, multi-tenant operating system for
membership organisations. The repository contains a Spring Boot API and a
responsive React application.

## Run with Docker

Prerequisites: Docker Desktop with Docker Compose.

```powershell
Copy-Item .env.example .env
docker compose up --build
```

The local services are available at:

- Frontend: <http://localhost:5173>
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
