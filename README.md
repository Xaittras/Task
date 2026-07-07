# Mini Test: Two Spring Boot Apps with Postgres and Docker

Two Spring Boot services orchestrated with docker-compose:

- **auth-api** (Service A, port `8080`) — registration/login with JWT, a
  protected `/api/process` endpoint that calls Service B and logs the
  result in Postgres.
- **data-api** (Service B, port `8081`) — a `/api/transform` endpoint that
  only accepts calls carrying a valid `X-Internal-Token` header.
- **postgres** (port `5432`) — stores `users` and `processing_log`.

## Project structure

```
/auth-api          Spring Boot project (Service A)
/data-api          Spring Boot project (Service B)
docker-compose.yml
.env.example       copy to .env to customize secrets/credentials
README.md
```

## How it fits together

1. Client registers and logs in against `auth-api` → gets a JWT.
2. Client calls `auth-api`'s `POST /api/process` with `Authorization: Bearer <token>`.
3. `auth-api` validates the JWT, then calls `data-api`'s `POST /api/transform`
   over the internal docker network (`http://data-api:8081`), attaching
   `X-Internal-Token: <INTERNAL_TOKEN>`.
4. `data-api` checks the token; if missing/invalid it returns `403`.
   Otherwise it transforms the text (reverses it and uppercases it) and
   returns `{ "result": "..." }`.
5. `auth-api` saves a row in `processing_log` (`user_id`, `input_text`,
   `output_text`, `created_at`) and returns the result to the client.

## Requirements

- Docker + Docker Compose v2
- JDK 17 and Maven 3.9+ (only needed if you want to build the jars locally
  instead of letting Docker do it — see note below)

## Configuration

All configuration is via environment variables (see `.env.example`):

| Variable | Used by | Purpose |
|---|---|---|
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | postgres, auth-api | DB credentials |
| `JWT_SECRET` | auth-api | HMAC secret for signing/validating JWTs |
| `INTERNAL_TOKEN` | auth-api, data-api | Shared secret for the `X-Internal-Token` header |

Copy `.env.example` to `.env` and adjust values before starting the stack:

```bash
cp .env.example .env
```

## Run

### Option A — let Docker build everything (simplest)

```bash
docker compose up -d --build
```

Each service's `Dockerfile` expects a pre-built jar at `target/<name>.jar`
(see below), so if you use this option make sure you've run the Maven
build at least once, or add a multi-stage Dockerfile build step in your
own fork. For this test the expected flow is to build the jars first:

### Option B — build jars locally, then start the stack

```bash
mvn -f auth-api/pom.xml clean package -DskipTests
mvn -f data-api/pom.xml clean package -DskipTests
docker compose up -d --build
```

Check that everything is healthy:

```bash
docker compose ps
docker compose logs -f auth-api
```

## Try it

**Register:**

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"a@a.com","password":"pass1234"}'
```

Expected: `201 Created` with the new user's id/email.

**Login:**

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"a@a.com","password":"pass1234"}'
```

Expected: `200 OK` with `{ "token": "<jwt>" }`. Save the token:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"a@a.com","password":"pass1234"}' | python3 -c "import sys,json;print(json.load(sys.stdin)['token'])")
```

**Process (protected):**

```bash
curl -X POST http://localhost:8080/api/process \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"text":"hello"}'
```

Expected: `200 OK` with `{ "result": "OLLEH" }` (data-api reverses then
uppercases the input), plus a new row in `processing_log`.

**Confirm Service B rejects direct/unauthenticated calls:**

```bash
curl -i -X POST http://localhost:8081/api/transform \
  -H "Content-Type: application/json" \
  -d '{"text":"hello"}'
# -> 403, missing X-Internal-Token

curl -i -X POST http://localhost:8081/api/transform \
  -H "Content-Type: application/json" \
  -H "X-Internal-Token: wrong-token" \
  -d '{"text":"hello"}'
# -> 403, invalid token
```

**Inspect stored data:**

```bash
docker compose exec postgres psql -U postgres -d appdb -c "select id, email, created_at from users;"
docker compose exec postgres psql -U postgres -d appdb -c "select id, user_id, input_text, output_text, created_at from processing_log;"
```

## Design notes

- **Passwords** are hashed with BCrypt (`spring-security-crypto`), never
  stored or logged in plain text.
- **JWT** is signed with HS256 using `JWT_SECRET`; the token carries the
  user id and email as claims and expires after 1 hour by default
  (`JWT_EXPIRATION_MS`).
- **Service-to-service trust**: `data-api` never talks to Postgres or
  handles user identities — it only trusts requests that carry the shared
  `X-Internal-Token`, checked with a constant-time comparison to avoid
  timing attacks. It doesn't do JWT validation at all; that's Service A's
  job.
- **Schema management**: Flyway (`auth-api/src/main/resources/db/migration/V1__init.sql`)
  creates `users` and `processing_log` on startup, so no manual DB setup
  is required beyond the `postgres` container itself.
- **No secrets in logs**: application logging is kept at `INFO` and
  neither service logs headers or request bodies, so tokens and passwords
  never end up in `docker compose logs`.
- **Networking**: both services share the `backend` docker-compose
  network; `auth-api` reaches `data-api` by its service name
  (`http://data-api:8081`), which is docker-compose's built-in DNS.

## Known limitations (kept intentionally simple per the brief)

- No refresh tokens / token revocation — a single short-lived JWT is
  issued at login.
- No rate limiting or account lockout on login attempts.
- The "transform" logic in Service B is intentionally trivial (reverse +
  uppercase) as allowed by the brief.
- No pagination on `processing_log`; there's no endpoint to list logs
  since it wasn't required, only the DB is used to verify persistence.
# Task
# Task
# Task
