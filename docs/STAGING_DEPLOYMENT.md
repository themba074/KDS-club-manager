# Staging deployment

Feature 20 publishes production-shaped backend and frontend images to GitHub
Container Registry and deploys the exact CI-verified `main` revision to a
single Linux staging host. The host runs Docker Compose behind a separate TLS
reverse proxy. Application secrets stay in the host's `.env.staging` file and
are never copied into either image.

## Deployment flow

1. A pull request runs backend, frontend, container-build, non-root-image, and
   staging Compose checks.
2. After a commit reaches `main`, the same CI workflow runs again.
3. `.github/workflows/deploy.yml` responds only to a successful `main` push
   CI run. It checks out that run's immutable commit SHA.
4. Backend and frontend images are published to GHCR with both the commit SHA
   and the moving `staging` tag. Deployment always uses the SHA tags.
5. The workflow copies `docker-compose.staging.yml` to the host, validates it,
   starts the verified images, waits for container health checks, and calls
   the public `/healthz` endpoint.

The moving tags are convenient for inspection. They are not used as the
deployment source of truth and should not be used for rollback.

## One-time infrastructure setup

Provision these external services before the first deployment:

- a PostgreSQL database reachable from the staging host;
- a private Supabase Storage bucket and service-role credential;
- an authenticated STARTTLS SMTP account; and
- a DNS name with a TLS-terminating reverse proxy that forwards to
  `127.0.0.1:8080` on the host.

Set HTTP Strict Transport Security at the public TLS proxy after HTTPS is
confirmed. The application Nginx container supplies CSP, clickjacking,
MIME-sniffing, referrer, and browser-permission headers, but intentionally does
not emit HSTS over its internal HTTP connection.

The Linux deployment user needs Docker Engine and the Docker Compose plugin,
permission to run Docker without an interactive prompt, and OpenSSH access.
Docker Compose must support `up --wait` and `--wait-timeout`.

Create the deployment directory and its environment file on the host:

```bash
sudo install -d -m 0750 -o kds-deploy -g kds-deploy /opt/kds-club-manager
sudo -u kds-deploy cp .env.staging.example /opt/kds-club-manager/.env.staging
sudo chmod 0600 /opt/kds-club-manager/.env.staging
```

Copy only the variable names and replace every example value. The staging
profile refuses repository-known development credentials, insecure browser
origins, development token delivery, non-SMTP email, or non-Supabase storage.
Compose also refuses to render when a required value is missing.

## GitHub staging environment

Create a GitHub environment named `staging`. Automatic deployment requires it
not to have a required-reviewer gate. Configure these environment variables:

| Variable | Example | Purpose |
|---|---|---|
| `STAGING_HOST` | `staging.example.com` | SSH host name |
| `STAGING_PORT` | `22` | SSH port |
| `STAGING_USER` | `kds-deploy` | Restricted deployment user |
| `STAGING_DEPLOY_PATH` | `/opt/kds-club-manager` | Host deployment directory |
| `STAGING_URL` | `https://staging.example.com` | Public health-check base URL |

Configure these environment secrets:

| Secret | Purpose |
|---|---|
| `STAGING_SSH_PRIVATE_KEY` | Private key dedicated to the deployment user |
| `STAGING_SSH_KNOWN_HOSTS` | Pinned host-key entry for the staging host |

Generate the `known_hosts` entry during setup with `ssh-keyscan`, then verify
its fingerprint through the hosting provider's console before saving it. Do
not disable SSH host-key checking. The workflow uses its short-lived
`GITHUB_TOKEN` to pull private GHCR images and logs the host out after each
deployment.

## Manual verification

On the host, validate the manifest without starting containers:

```bash
BACKEND_IMAGE=ghcr.io/themba074/kds-club-manager/backend:COMMIT_SHA \
FRONTEND_IMAGE=ghcr.io/themba074/kds-club-manager/frontend:COMMIT_SHA \
docker compose --env-file .env.staging -f docker-compose.staging.yml config --quiet
```

After deployment, verify both boundaries:

```bash
docker compose --env-file .env.staging -f docker-compose.staging.yml ps
curl --fail http://127.0.0.1:8080/healthz
curl --fail https://staging.example.com/healthz
```

## Rollback

Find the last known-good SHA in the GitHub Actions deployment history, then run
the Compose command with both images pinned to that SHA:

```bash
BACKEND_IMAGE=ghcr.io/themba074/kds-club-manager/backend:PREVIOUS_SHA \
FRONTEND_IMAGE=ghcr.io/themba074/kds-club-manager/frontend:PREVIOUS_SHA \
docker compose --env-file .env.staging -f docker-compose.staging.yml up -d \
  --pull always --remove-orphans --wait --wait-timeout 180
```

Database migrations run forward when the backend starts. A rollback across an
incompatible migration needs a database recovery plan; never assume changing
the image tag reverses schema changes.
