# Auth Service (Spring Boot) — JWT + Redis Sessions + Postgres + Liquibase

Production-grade authentication service built with:
- **Spring Boot 3.3.x**, **Java 17**
- **Spring Security 6**
- **PostgreSQL** (durable persistence)
- **Liquibase** (schema migrations)
- **Redis** (low-latency refresh-token sessions + access-token revocation + caching)
- **JWT access tokens** + **refresh tokens**
- **Per-device sessions** + **cookie-based refresh** + **CSRF-safe refresh**
- **Access token invalidation**:
    - **Redis denylist** by `jti` (single-token logout)
    - **Token version (`tv`)** by user (logout-all, scalable)

---

## 1. Features

### Authentication flows
- `POST /api/auth/register` — Create a new user and issue tokens
- `POST /api/auth/login` — Authenticate user and issue tokens
- `POST /api/auth/refresh` — Rotate refresh token (cookie-based)
- `POST /api/auth/logout-device` — Logout current device
- `POST /api/auth/logout-all` — Logout all devices

### Token model
- **Access token**: JWT (short TTL)
    - Contains:
        - `sub` = email
        - `uid` = user UUID
        - `jti` = unique token id
        - `tv` = user token-version
        - `roles` = list of roles
- **Refresh token**: opaque random string (stored only as SHA-256 hash server-side)
    - Stored in:
        - **Postgres** (audit/durability)
        - **Redis** (fast-path session store with TTL)

### Per-device sessions
- A `deviceId` is stored in cookie `did`
- Refresh tokens are bound to `deviceId`
- You can revoke:
    - **one device** (`logout-device`)
    - **all devices** (`logout-all`)

### CSRF-safe cookie refresh
- Refresh token stored as **HttpOnly** cookie `__Host-rt`
- Spring Security provides `XSRF-TOKEN` cookie (readable by JS)
- Client must send `X-XSRF-TOKEN` header for POST requests that rely on cookies (refresh/logout)

---

## 2. Technology Stack

- Spring Boot: Web, Validation, Security, Data JPA, Actuator
- PostgreSQL: user + role + refresh token tables
- Liquibase: initial schema + seed USER role
- Redis:
    - Refresh sessions (`rt:*`) with TTL
    - Index sets per user/device (for bulk revoke)
    - Denylist entries (`deny:jwt:*`) for access-token revocation
    - Token version (`tv:<userId>`) for logout-all invalidation
- JWT library: JJWT

---

## 3. Repository Layout

auth-service/
pom.xml
docker-compose.yml
src/
main/
java/com/example/auth/...
resources/
application.yml
db/changelog/...



Key packages:
- `config/` — security + JWT filter + caching
- `domain/` — JPA entities
- `repository/` — JPA repositories
- `service/` — auth logic, JWT, Redis stores
- `web/` — REST controllers + cookie utility
- `exception/` — consistent error responses

---

## 4. Local Development Setup

### Prerequisites
- Java 17+
- Maven 3.9+
- Docker + Docker Compose

### Start Postgres + Redis
From project root:

```bash
docker compose up -d
