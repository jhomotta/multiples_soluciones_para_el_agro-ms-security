# Architecture — ms-security

## 1. Layers

```
                    ┌──────────────────────────────────────────┐
   HTTP ──────────► │  reactive-web        (entry point)       │
                    │  controllers, exception handler          │
                    └───────────────┬──────────────────────────┘
                                    │ calls business interfaces
                    ┌───────────────▼──────────────────────────┐
                    │  usecase             (domain)            │
                    │  business rules, outbound ports          │
                    └───────────────┬──────────────────────────┘
                                    │ depends only on
                    ┌───────────────▼──────────────────────────┐
                    │  model               (domain)            │
                    │  entities, DTOs, enums, errors           │
                    └──────────────────────────────────────────┘
                                    ▲ implement the ports
                    ┌───────────────┴──────────────────────────┐
                    │  security-db  │  security-jwt            │
                    │  R2DBC+Flyway │  JWT, Argon2id, SHA-256  │
                    └──────────────────────────────────────────┘
                    ┌──────────────────────────────────────────┐
                    │  configuration │ app-service │ utility   │
                    └──────────────────────────────────────────┘
```

Dependencies point inward. The use-case layer names what it needs as an interface
(`SecurityUserRepositoryPort`, `PasswordHasherPort`, …) and never learns which database or which
hashing library satisfies it. Swapping PostgreSQL for something else, or Argon2id for its
successor, is an adapter change.

## 2. Request flow — login

```
POST /api/v1/auth/login
  │
  ├─ AuthController                     validates the body, resolves the ClientContext
  ├─ AuthenticateUseCaseImpl
  │    ├─ find the user by LOWER(username)          → security_user
  │    ├─ check enabled / locked / expired          → login_attempt on failure
  │    ├─ verify the password (Argon2id, off-loop)  → login_attempt on failure
  │    ├─ re-hash if the stored hash is outdated    → security_user
  │    ├─ resolve the access grant for the app      → user_application
  │    ├─ clear the failure counter, stamp login    → security_user
  │    └─ TokenIssuer
  │         ├─ resolve authorities from the grant   → user_role ⋈ role ⋈ role_permission ⋈ permission
  │         ├─ sign the access token (HS256)
  │         └─ store the refresh token hash         → refresh_token
  └─ 200 with the token pair                        → security_audit (LOGIN_SUCCEEDED)
```

Note what the shape enforces: valid credentials alone do not authenticate anyone. Without an
active `user_application` grant whose window is open, login fails — with the same message as a
wrong password.

## 3. Where authorization actually happens

Three independent layers, deliberately:

1. **The token.** The JWT carries the authority list resolved at issuing time, so a protected
   route decides without a database read. `@PreAuthorize("hasAuthority('ROLE_ASSIGN')")` on the
   controller is checked against it.
2. **The use case.** Before writing, it checks that the role and the grant belong to the same
   application, so the caller gets a clear `400` explaining the mismatch.
3. **The database.** The composite foreign keys on `user_role` and `role_permission` make the
   cross-application assignment structurally impossible. If the service had a bug, or someone
   wrote SQL by hand, the constraint still fires.

The third layer is the one that matters. The first two are convenience and good error messages.

## 4. Token lifecycle

**Access token** — HS256 JWT, 15 minutes by default. Claims: subject (`security_user.id`), person
id, username, `user_application` id, application id, `security_stamp`, authorities. Stateless: no
database read per request.

**Refresh token** — 256 random bits, opaque, 7 days by default. Only its SHA-256 hash is stored.
Every rotation of one login shares a `token_family`, and `replaced_by_token_id` chains old to new.

```
login ──► T1 (family F)
             │ refresh
             ▼
          T1 revoked, replaced_by=T2 ──► T2 (family F)
                                            │ refresh
                                            ▼
                                         T2 revoked ──► T3 (family F)

replay T1 ──► reuse detected ──► every live token of family F revoked ──► 401
```

The consequence is what makes rotation worth the complexity: a stolen refresh token can be used at
most once before the theft becomes visible, and when it does, both the thief and the legitimate
holder are locked out until someone logs in again.

### Why the revocation is committed before the 401

Reuse detection revokes tokens and *then* fails the call. Under a method-wide `@Transactional`,
that thrown error would roll the revocation back and the replayed token would keep working — the
protection would silently do nothing. `RefreshTokenUseCaseImpl` therefore uses an explicit
`TransactionalOperator`: the revocation is committed in its own unit of work and the error is
raised outside it, while the rotation itself (revoke the old row, insert the new one) stays inside
one transaction where atomicity is what matters.

This was a real bug during development, caught by checking the database rather than the response:
the API returned the right `401`, but `refresh_token.revoked_at` was still null and the
`TOKEN_REUSE_DETECTED` audit row was missing — only the gap in the identity sequence showed that a
row had been inserted and rolled back.

### Revocation latency

Roles revoked a minute ago are still listed in a token issued two minutes ago. Revocation lands at
the next refresh, where the grant, the account state and `security_stamp` are all re-read. The
access-token lifetime is therefore exactly the width of that window, and the reason it is short.

## 5. Passwords

`Argon2PasswordHasherAdapter` wraps a `DelegatingPasswordEncoder` whose default is Argon2id at the
OWASP baseline (19 MiB, t=2, p=1, 16-byte salt, 32-byte output). Every new hash records the
algorithm that produced it:

```
{argon2}$argon2id$v=19$m=19456,t=2,p=1$<salt>$<hash>
```

That prefix is what makes a future migration undramatic: old hashes keep verifying, and
`needsUpgrade()` tells the login flow to re-hash them the next time the raw password passes
through — the only moment it is available. BCrypt is kept as a verifier for hashes imported from
an older system and never produces one.

Argon2 is intentionally expensive, so both hashing and verification run on the bounded-elastic
scheduler and never occupy an event-loop thread.

## 6. What the caller is told, and what is recorded

| Situation | Response | Database |
|---|---|---|
| Unknown username | `401 Invalid credentials` | `login_attempt.failure_reason = UNKNOWN_USERNAME` |
| Wrong password | `401 Invalid credentials` | `BAD_PASSWORD`, counter incremented |
| Account locked | `401 Invalid credentials` | `ACCOUNT_LOCKED` |
| No access to the app | `401 Invalid credentials` | `NO_APPLICATION_ACCESS` |
| Reset for unknown user | `200` + neutral message | nothing |
| Logout with a bogus token | `200 Logged out` | nothing |

One message for every failure, one answer whether or not an account exists. The precise reason
lives in the database, where an operator can see it and an attacker cannot. Uniform answers are
also why logout is silent about unknown tokens: "that token does not exist" would turn logout into
an oracle for guessing valid ones.

## 7. Account lockout

`failed_attempts` is incremented with a single atomic `UPDATE ... RETURNING`, not a read-modify-write,
so concurrent login attempts cannot lose an increment. At `max-failed-attempts` the account is
locked until `now() + lock-minutes`. A successful login, a password change and a password reset all
clear the counter and lift the lock.

## 8. Audit trail

`AuditRecorder` is the only component that writes `security_audit`, and it offers no update or
delete. `AuditQueryUseCase` reads and nothing else. `AuditController` exposes three GET routes and
no write verb — a `POST` to any of them answers `405`.

A write failure is swallowed rather than propagated: auditing must never be the reason a legitimate
operation fails.

## 9. Persistence choices

Ten tables map cleanly through Spring Data R2DBC repositories with MapStruct entity ↔ domain
mappers. The four tables carrying `INET` or `JSONB` — `refresh_token`, `login_attempt`,
`password_reset_token`, `security_audit` — use `DatabaseClient` adapters with explicit SQL instead.

Neither type has a portable R2DBC Java mapping. The alternatives were to weaken the columns to
`VARCHAR`/`TEXT`, or to put a driver-specific class in the entity. Explicit SQL with
`CAST(:ip AS inet)` and `host(ip_address)` keeps the real column types — and with them the
validation and the index behaviour that made `inet` and `jsonb` worth choosing — without any
PostgreSQL type reaching the domain model.

State transitions on `security_user` (failed attempt, lock, password change, stamp bump) are single
UPDATE statements for the same reason: correctness under concurrency, not convenience.

## 10. Configuration and start-up

- Flyway runs at start-up over its own JDBC connection (`FlywayConfig`), because R2DBC cannot run
  migrations. Spring Boot's own Flyway auto-configuration is disabled so there is one runner.
- `-parameters` is set for every module. Spring reads real parameter names to bind `@PathVariable`;
  the Boot plugin adds the flag only to `:app-service`, and the controllers live in `:reactive-web`.
- `BootstrapAdminRunner` creates the first administrator when enabled, and is a no-op once the
  username exists, so restarting never resets an account.

## 11. Known gaps

- **No tests are included.** The behaviour described here was verified by running the service
  against a real PostgreSQL and inspecting both the responses and the tables; that is not a
  substitute for a suite. The obvious first targets are the use cases against Testcontainers.
- **Password reset delivers nothing.** The token is generated and stored correctly, but there is no
  mail adapter. `security.policy.expose-reset-token` returns it in the response for local testing
  and must stay off elsewhere.
- **A revoked token always reads as reuse.** After a logout-all or a password change, presenting an
  old token returns the reuse-detection message rather than a plainer "session ended". It fails
  closed, which is the right direction, but the message is misleading.
- **`security_stamp` is not checked per request.** By design — that would mean a database read on
  every call. It is verified at refresh, and the short access-token lifetime bounds the exposure.
- **No rate limiting.** Lockout bounds attempts per account; it does nothing against a slow spray
  across many accounts from one address. `login_attempt(ip_address, attempted_at DESC)` is indexed
  for exactly this query, but nothing consumes it yet.
