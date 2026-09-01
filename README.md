# ms-security

Security microservice of **`multiples_soluciones_para_el_agro`**. It owns people, credentials,
multi-application access, RBAC, JWT with rotating refresh tokens, password reset, and an
append-only security audit trail.

```
Company ─→ Application ─→ Role ─→ Permission
                 ↑
Person ─→ SecurityUser ─→ UserApplication ─→ UserRole
```

The load-bearing idea is `user_application`: a person has **one** set of credentials but a
separate access grant per application, and roles hang off the grant rather than off the user.
The same person can be `ADMIN` in one application and `FIELD_OPERATOR` in another, and the
database — not just the service — refuses to mix the two.

## Stack

- **Java 25** · **Gradle multi-module** (Bancolombia Clean Architecture scaffold)
- **Spring Boot 3.5.x + WebFlux** — reactive, non-blocking end to end
- **Spring Security (reactive)** + **JWT** (HS256 access + opaque rotating refresh tokens)
- **Argon2id** password hashing behind a `DelegatingPasswordEncoder`
- **PostgreSQL** + **Spring Data R2DBC** (runtime) + **Flyway** (migrations over JDBC at start-up)
- **Lombok** + **MapStruct** + **springdoc-openapi**

## Modules

```
applications/app-service                    :app-service   — the only runnable Spring Boot module
domain/model                                :model         — domain model, DTOs, enums, errors
domain/usecase                              :usecase       — business rules + outbound gateway ports
infrastructure/entry-points/reactive-web    :reactive-web  — WebFlux controllers + exception handler
infrastructure/driven-adapters/security-db  :security-db   — R2DBC (PostgreSQL) + Flyway, owns db/migration
infrastructure/driven-adapters/security-jwt :security-jwt  — JWT, Argon2id, SHA-256, token generation
infrastructure/configuration                :configuration — reactive security, JWT filter, CORS, OpenAPI
infrastructure/helpers/utility              :utility       — shared constants
```

Dependencies point inward: `usecase` knows only `model` and its own ports; the adapters implement
those ports and are never referenced by the domain.

## Documentation

- [`docs/DATABASE_MODEL.md`](docs/DATABASE_MODEL.md) — the 14 tables, keys, constraints, indexes.
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — layers, flows, and the security decisions behind them.
- [`docs/API_EXAMPLES.md`](docs/API_EXAMPLES.md) — five runnable end-to-end examples.
- [`docs/BUILD_SUMMARY.md`](docs/BUILD_SUMMARY.md) — what was built, what was verified, and the known gaps.

## Requirements

- **JDK 25**. If it is not installed, Gradle downloads it automatically (Foojay toolchain resolver).
- A reachable **PostgreSQL 14+** with a database named `security_db`.

## Configuration

Every secret comes from the environment. Nothing real belongs in Git:

```bash
cp .env.example .env
# edit .env, then:
source .env
```

| Variable | Meaning |
|---|---|
| `R2DBC_URL` | `r2dbc:postgresql://<host>:5432/security_db` |
| `DB_JDBC_URL` | `jdbc:postgresql://<host>:5432/security_db` (Flyway only) |
| `DB_USERNAME` / `DB_PASSWORD` | database credentials, shared by both |
| `JWT_SECRET` | Base64 random secret, ≥ 32 bytes after decoding |
| `JWT_ACCESS_TOKEN_MINUTES` | access-token lifetime (default 15) |
| `JWT_REFRESH_TOKEN_DAYS` | refresh-token lifetime (default 7) |
| `MAX_FAILED_ATTEMPTS` / `LOCK_MINUTES` | lockout policy (default 5 / 15) |
| `PASSWORD_RESET_MINUTES` | reset-token lifetime (default 30) |
| `EXPOSE_RESET_TOKEN` | **local testing only** — echoes the raw reset token in the response |
| `BOOTSTRAP_ADMIN_ENABLED` / `BOOTSTRAP_ADMIN_PASSWORD` | create the first administrator on start-up |
| `CORS_ALLOWED_ORIGINS` | comma-separated origins; narrow this outside local development |

Generate a JWT secret with `openssl rand -base64 48`.

## Run it

```bash
# 1. A local PostgreSQL
podman compose up -d          # or: docker compose up -d   (see compose.yaml)

# 2. Environment
cp .env.example .env && $EDITOR .env && source .env
export BOOTSTRAP_ADMIN_ENABLED=true BOOTSTRAP_ADMIN_PASSWORD='Adm1n-Local-2026!'

# 3. Build and run — Flyway creates the schema and seeds the catalogues on the first start
./gradlew clean build
./gradlew :app-service:bootRun
```

Turn `BOOTSTRAP_ADMIN_ENABLED` back off once the administrator exists. Migration `V17` seeds the
`MSAGRO` company, the `AGRO_CORE` and `AGRO_FIELD` applications, the professions, the `ADMIN` and
`USER` roles, and the permission catalogue — so registration can assign the default role
immediately.

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- Health: <http://localhost:8080/actuator/health>

To see the whole thing work at once, run `./docs/examples.sh` against a freshly migrated database.
It walks the five flows in [`docs/API_EXAMPLES.md`](docs/API_EXAMPLES.md) and prints what each returns.

## API

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/api/v1/auth/register` | public | Create person + credentials + access, return a token pair |
| POST | `/api/v1/auth/login` | public | Authenticate against one application |
| POST | `/api/v1/auth/refresh` | public | Rotate a refresh token |
| POST | `/api/v1/auth/logout` | public | Revoke one token, or every session of the grant |
| POST | `/api/v1/auth/password-reset/request` | public | Issue a single-use reset token |
| POST | `/api/v1/auth/password-reset/confirm` | public | Consume it and set the new password |
| GET | `/api/v1/me` | token | The identity the token carries |
| GET | `/api/v1/me/access` | token | Every application the caller can reach |
| POST | `/api/v1/me/change-password` | token | Change own password; revokes every session |
| POST/GET | `/api/v1/companies` | `COMPANY_CREATE` / `COMPANY_READ` | Companies |
| POST | `/api/v1/applications` | `APPLICATION_CREATE` | Applications |
| GET | `/api/v1/companies/{id}/applications` | `APPLICATION_READ` | Applications of a company |
| POST/GET | `/api/v1/professions` | `PROFESSION_CREATE` / `PROFESSION_READ` | Professions |
| POST | `/api/v1/roles` | `ROLE_CREATE` | Roles |
| GET | `/api/v1/applications/{id}/roles` | `ROLE_READ` | Roles of an application |
| POST | `/api/v1/permissions` | `PERMISSION_CREATE` | Permissions |
| GET | `/api/v1/applications/{id}/permissions` | `PERMISSION_READ` | Permissions of an application |
| POST | `/api/v1/roles/permissions` | `PERMISSION_GRANT` | Grant a permission to a role |
| POST | `/api/v1/access` | `USER_GRANT_ACCESS` | Grant a user access to an application |
| DELETE | `/api/v1/access/{id}` | `USER_GRANT_ACCESS` | Revoke an access grant |
| POST | `/api/v1/access/roles` | `ROLE_ASSIGN` | Assign a role to a grant |
| DELETE | `/api/v1/access/{id}/roles/{roleId}` | `ROLE_ASSIGN` | Revoke a role |
| GET | `/api/v1/access/{id}` | `USER_READ` | One grant, with roles and effective permissions |
| GET | `/api/v1/access/users/{id}` | `USER_READ` | Every grant of one user |
| GET | `/api/v1/audit/users/{id}` | `AUDIT_READ` | Security events of a user |
| GET | `/api/v1/audit/applications/{id}` | `AUDIT_READ` | Security events of an application |
| GET | `/api/v1/audit/correlation/{id}` | `AUDIT_READ` | Events sharing a correlation id |

Every response uses the same envelope:

```json
{
  "status": "SUCCESS",
  "message": "Authenticated",
  "httpStatus": 200,
  "answer": { },
  "applicationProvider": "ms-security",
  "metadata": null,
  "serverDateTime": "2026-08-31T12:04:39-05:00"
}
```

## Security notes

- Passwords are **Argon2id** (19 MiB, 2 iterations, parallelism 1, per-password salt), stored as
  the full encoded string with an `{argon2}` prefix. Legacy BCrypt hashes still verify and are
  re-hashed on the next successful login.
- Refresh and reset tokens are stored **only as SHA-256 hashes**. The raw value leaves the server once.
- Refresh tokens **rotate**; replaying a revoked one revokes the whole `token_family`.
- Login answers with one generic message for every failure. The real reason is kept in
  `login_attempt.failure_reason` for forensics.
- Password reset answers identically whether or not the account exists.
- `security_audit` is append-only: no endpoint updates or deletes it.
