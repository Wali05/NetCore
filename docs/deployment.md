# Deploy a private NetCore demo

The application remains a React frontend, Spring Boot API, and Oracle database. The optional gateway is deployment configuration around that application.

## Local stack

Run Docker Compose from the repository root:

```bash
docker compose up --build --wait --wait-timeout 600
docker compose ps
```

Open http://localhost:3000. The database and API have no published host ports. The frontend binds to 127.0.0.1. Compose waits for Oracle health and API health before starting the frontend.

## Server setup

Use a Linux host with Docker Compose, persistent storage, and enough memory for Oracle Free plus the JVM and image builds. Check Oracle's requirements for the selected image and measure actual usage on the host; this repository has no measured minimum VPS size.

1. Copy the repository to the server and point a DNS name at it. Permit inbound TCP 80 and 443; do not publish Oracle or API ports.
2. Copy `.env.example` to `.env`. Set different, strong `NETCORE_DB_PASSWORD` and `NETCORE_DB_ADMIN_PASSWORD` values. Set `NETCORE_DOMAIN` to the bare DNS name, such as `netcore.example.com`.
3. Generate a reviewer password hash interactively:

   ```bash
   docker run --rm -it caddy:2-alpine caddy hash-password
   ```

4. Put the returned bcrypt hash in `NETCORE_DEMO_PASSWORD_HASH`, enclosed in single quotes in `.env`. This preserves dollar signs. The login username is `reviewer`.
5. Check configuration and start:

   ```bash
   docker compose -f compose.yaml -f compose.deploy.yaml config --quiet
   docker compose -f compose.yaml -f compose.deploy.yaml up --build --wait --wait-timeout 600
   ```

Caddy requests a certificate for the configured DNS name, redirects HTTP to HTTPS, and checks the password before forwarding either UI or API requests. It rejects cross-site browser writes using Fetch Metadata and strips the gateway Authorization header before proxying. Caddy stores certificates in its persistent volume.

The shared login grants full application access. Use disposable demonstration data. This is a gate for invited reviewers, not a multi-user authorization system. The localhost-only port 3000 remains available to the host administrator; anyone with shell access can reach that local endpoint.

References: [Caddy automatic HTTPS](https://caddyserver.com/docs/automatic-https), [Caddy basic authentication](https://caddyserver.com/docs/caddyfile/directives/basic_auth), [Compose health-based startup](https://docs.docker.com/compose/how-tos/startup-order/).

## Verify before sharing

- Open the HTTPS URL in a fresh browser session: it must require the reviewer login.
- Request `/api/v1/subnets` without credentials: it must also return 401.
- Confirm the certificate is valid and the dashboard can create and release an allocation.
- Confirm `docker compose ps` shows healthy Oracle, backend, and frontend services.
- Restart the stack and verify existing records remain. Data persistence is separate from a passing Testcontainers test.
- Keep the reviewer password out of the README and source control.

No hosted verification has been performed for this change. See [the verification record](verification.md).

## Updates and troubleshooting

```bash
docker compose -f compose.yaml -f compose.deploy.yaml logs --tail=100 backend
docker compose -f compose.yaml -f compose.deploy.yaml logs --tail=100 gateway
docker compose -f compose.yaml -f compose.deploy.yaml up --build -d --wait --wait-timeout 600
```

Oracle's first initialization can take several minutes. Failed migrations or incorrect credentials keep the API unhealthy instead of exposing a half-started app. Caddy certificate failures usually require checking DNS, port reachability, and gateway logs.

The Java container runs as an unprivileged user. Nginx serves the built frontend and forwards same-origin API requests. Hashed assets are cached; HTML is revalidated and API responses are not cached. Only the health endpoint is forwarded under `/actuator`.

Container tags are version lines, not immutable digests. Before maintaining a long-lived instance, pin tested image digests and review image updates.

## Data lifecycle

`docker compose down` stops the stack without removing named volumes. **Do not add `--volumes` unless you intend to erase the data.** Back up Oracle data and test restoration before storing anything important. This repository does not automate database backups.

Oracle image passwords initialize a new database. Changing `.env` does not rotate users inside an existing Oracle volume; rotate database credentials deliberately and update the application configuration together.

Flyway applies forward migrations before Hibernate validates the schema. Do not edit an applied migration or assume that replacing an application image rolls back database changes.

For a sample server inventory, tunnel the host's loopback port instead of exposing it:

```bash
ssh -L 3000:127.0.0.1:3000 user@your-server
node scripts/seed-demo.mjs http://127.0.0.1:3000
```

The sample script refuses non-local targets and does nothing when inventory already exists.
