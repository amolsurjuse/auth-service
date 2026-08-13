# Auth Service

Spring Boot authentication service for ElectraHub with JWT access tokens, refresh-token flows, Redis-backed session state, and PostgreSQL persistence.

## Exposed Port

- `8080`

## Core Capabilities

- Register and login
- Refresh token rotation
- Logout current device / logout all devices
- JWT validation support for gateway/service-to-service auth
- Country list proxy (`/api/countries`)

## Dependencies

- PostgreSQL (`user_db`)
- Redis
- User service (`APP_USER_SERVICE_BASE_URL`)

## Local Docker Desktop

From workspace root:

```bash
./scripts/deploy-local-service.sh up auth-service
```

Run full stack:

```bash
./scripts/deploy-local-service.sh up all
```

## Health Check

```bash
curl -i http://localhost:8080/actuator/health
```

## Updated

- README reviewed and refreshed on `2026-03-21`.
