# API examples — ms-security

Five end-to-end examples against a freshly migrated database. Everything below was run against a
real PostgreSQL 16 and the output is copied from that run, not written by hand.

`docs/examples.sh` runs all five in sequence:

```bash
./docs/examples.sh                    # against http://localhost:8080
BASE=http://host:8080 ./docs/examples.sh
```

**Setup.** Start PostgreSQL, set the environment (see the README), and let the service boot once
with the bootstrap administrator enabled:

```bash
export BOOTSTRAP_ADMIN_ENABLED=true
export BOOTSTRAP_ADMIN_PASSWORD='Adm1n-Local-2026!'
export EXPOSE_RESET_TOKEN=true      # local only — example 5 needs to read the reset token
./gradlew :app-service:bootRun
```

Flyway applies the 17 migrations and seeds the `MSAGRO` company, applications `AGRO_CORE` (id 1)
and `AGRO_FIELD` (id 2), six professions, the `ADMIN` and `USER` roles, and 16 permissions.

---

## 1 · The administrator logs in

```bash
curl -s -X POST localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"applicationId":1,"username":"admin","password":"Adm1n-Local-2026!"}'
```

```json
{
  "status": "SUCCESS",
  "message": "Authenticated",
  "httpStatus": 200,
  "answer": {
    "securityUserId": 1,
    "username": "admin",
    "applicationId": 1,
    "userApplicationId": 1,
    "tokenType": "Bearer",
    "accessToken": "eyJhbGciOiJIUzM4NCJ9...",
    "accessTokenExpiresAt": "2026-08-31T17:19:39.665638869Z",
    "refreshToken": "WLLtDoNXnq8pXHtPaq6Cf4d8jMgPFgerS2KdAhsJkiM",
    "refreshTokenExpiresAt": "2026-09-07T17:04:39.699710590Z",
    "mustChangePassword": false,
    "authorities": ["APPLICATION_CREATE", "APPLICATION_READ", "AUDIT_READ", "..."]
  },
  "applicationProvider": "ms-security",
  "serverDateTime": "2026-08-31T12:04:39-05:00"
}
```

Decoding the access token shows what an authorization decision is made from, with no database read:

```
sub = 1 | app = 1 | grant = 1 | stamp = 0
APPLICATION_CREATE, APPLICATION_READ, AUDIT_READ, COMPANY_CREATE, COMPANY_READ,
PERMISSION_CREATE, PERMISSION_GRANT, PERMISSION_READ, PROFESSION_CREATE, PROFESSION_READ,
ROLE_ADMIN, ROLE_ASSIGN, ROLE_CREATE, ROLE_READ, USER_CREATE, USER_GRANT_ACCESS, USER_READ
```

The refresh token is the only time that value is ever visible: the database holds its SHA-256 hash.

---

## 2 · A farmer registers

One call creates the `person`, the `security_user`, the `user_application` grant, the default
`USER` role assignment, and returns a token pair.

```bash
curl -s -X POST localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' -d '{
    "applicationId": 1, "username": "jperez", "password": "Cosecha-2026-Segura!",
    "identificationType": "CC", "identificationNumber": "1098765432",
    "firstName": "Juan", "middleName": "Carlos",
    "lastName": "Perez", "secondLastName": "Gomez",
    "email": "juan.perez@msagro.local", "mobile": "3001234567",
    "birthDate": "1990-05-14", "professionId": 5
  }'
```

```
201 User registered
```

`GET /api/v1/me` with the resulting token:

```json
{
  "status": "SUCCESS", "message": "OK", "httpStatus": 200,
  "answer": {
    "securityUserId": 2,
    "personId": 2,
    "username": "jperez",
    "userApplicationId": 2,
    "applicationId": 1,
    "securityStamp": 0,
    "authorities": ["APPLICATION_READ", "PROFESSION_READ", "ROLE_READ", "ROLE_USER"]
  }
}
```

Registering the same identification again does not silently take the person over:

```
409 This person already has a user account
```

---

## 3 · RBAC: the same route, two callers

The farmer holds `ROLE_USER`, which does not carry `COMPANY_CREATE`:

```bash
curl -s -X POST localhost:8080/api/v1/companies \
  -H "Authorization: Bearer $FARMER" -H 'Content-Type: application/json' \
  -d '{"code":"OTRA","name":"Otra empresa"}'
```

```json
{
  "status": "ERROR",
  "message": "You do not have the permission required for this operation",
  "httpStatus": 403,
  "answer": null
}
```

The administrator does carry it, and creates master data:

```bash
curl -s -X POST localhost:8080/api/v1/professions \
  -H "Authorization: Bearer $ADMIN" -H 'Content-Type: application/json' \
  -d '{"code":"apicultor","name":"Apicultor","description":"Manejo de colmenas"}'
```

```
201 Profession created | id 7 | code APICULTOR
```

The code was sent as `apicultor` and stored as `APICULTOR` — codes are normalised so the unique
constraints behave predictably.

```bash
curl -s -X POST localhost:8080/api/v1/roles \
  -H "Authorization: Bearer $ADMIN" -H 'Content-Type: application/json' \
  -d '{"applicationId":2,"code":"FIELD_OPERATOR","name":"Operario de campo"}'
```

```
201 Role created | role 3 in application 2
```

---

## 4 · One person, two applications, different roles

This is the point of `user_application`. The farmer has credentials, but credentials alone open
nothing:

```bash
curl -s -X POST localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"applicationId":2,"username":"jperez","password":"Cosecha-2026-Segura!"}'
```

```
401 Invalid credentials
```

The password was right. The answer is the same one a wrong password gets — the endpoint does not
disclose which part failed. `login_attempt.failure_reason` records `NO_APPLICATION_ACCESS`.

The administrator grants access to application 2:

```bash
curl -s -X POST localhost:8080/api/v1/access \
  -H "Authorization: Bearer $ADMIN" -H 'Content-Type: application/json' \
  -d '{"securityUserId":2,"applicationId":2}'
```

```
201 Access granted
```

Attaching a role that belongs to a **different** application is refused:

```bash
curl -s -X POST localhost:8080/api/v1/access/roles \
  -H "Authorization: Bearer $ADMIN" -H 'Content-Type: application/json' \
  -d '{"userApplicationId":3,"roleId":2}'          # role 2 = USER, application 1
```

```
400 Role 2 belongs to application 1, but the access grant is for application 2
```

That message comes from the use case, which checks first to produce a clear error. The database
refuses it independently, so the rule holds even if the service is bypassed entirely:

```sql
INSERT INTO user_role (user_application_id, role_id, application_id) VALUES (3, 2, 1);
-- ERROR: violates foreign key constraint "fk_user_role_user_application"
-- DETAIL: Key (user_application_id, application_id)=(3, 1) is not present in table "user_application".
```

With the correct role, the same person now logs into either application and gets a different
authority set:

```
201 Role assigned
app 2 grant 3 → ['ROLE_FIELD_OPERATOR']
```

```bash
curl -s localhost:8080/api/v1/access/users/2 -H "Authorization: Bearer $ADMIN"
```

```
AGRO_CORE    roles=['USER']           permissions=['APPLICATION_READ', 'PROFESSION_READ']
AGRO_FIELD   roles=['FIELD_OPERATOR'] permissions=[]
```

---

## 5 · Token lifecycle, lockout and password reset

### Rotation and reuse detection

```bash
T1=$(curl -s -X POST localhost:8080/api/v1/auth/login -H 'Content-Type: application/json' \
     -d '{"applicationId":1,"username":"jperez","password":"Cosecha-2026-Segura!","deviceId":"pc-jperez"}' \
     | jq -r .answer.refreshToken)

T2=$(curl -s -X POST localhost:8080/api/v1/auth/refresh -H 'Content-Type: application/json' \
     -d "{\"refreshToken\":\"$T1\"}" | jq -r .answer.refreshToken)
```

```
T1 Oeck-N3lppW6…  →  T2 Q-79k6opl6QO…
```

Replaying the old token — what a thief with a copied value would do:

```
401 Refresh token already used. Every session of this device family was revoked.
```

And the legitimate current token is dead too:

```
401 Refresh token already used. Every session of this device family was revoked.
```

That second line is the whole mechanism. A stolen refresh token buys one use, after which the
theft is visible and both parties are locked out until someone logs in with a password. In the
database:

```
 id |  family  | revoked | replaced_by_token_id | device_id
----+----------+---------+----------------------+-----------
  6 | qiX5aFFA | t       |                    7 | pc-jperez
  7 | qiX5aFFA | t       |                    8 | pc-jperez
  8 | qiX5aFFA | t       |                      | pc-jperez
```

### Lockout

```
attempt 1: 401 Invalid credentials
attempt 2: 401 Invalid credentials
attempt 3: 401 Invalid credentials
attempt 4: 401 Invalid credentials
attempt 5: 401 Invalid credentials
correct password now: 401 Invalid credentials
```

Six identical responses, six different situations. The database keeps what the API will not say:

```sql
SELECT failure_reason, count(*) FROM login_attempt WHERE NOT success GROUP BY failure_reason;
```

```
 failure_reason | count
----------------+-------
 ACCOUNT_LOCKED |     1
 BAD_PASSWORD   |     5
```

```sql
SELECT username, locked, locked_until, failed_attempts FROM security_user WHERE id = 2;
```

```
 username | locked |         locked_until          | failed_attempts
----------+--------+-------------------------------+-----------------
 jperez   | t      | 2026-08-31 17:24:09.710691+00 |               5
```

### Password reset

```bash
curl -s -X POST localhost:8080/api/v1/auth/password-reset/request \
  -H 'Content-Type: application/json' -d '{"applicationId":1,"username":"jperez"}'
```

```json
{
  "status": "SUCCESS",
  "message": "If the account exists, a password reset token has been issued.",
  "httpStatus": 200,
  "answer": {
    "message": "If the account exists, a password reset token has been issued.",
    "resetToken": "j5i_ZGNYBVrPc1jZW3KrcinSoRVoXtrkA3hFSEdCZrU",
    "expiresAt": "2026-08-31T17:39:22.698657413Z"
  }
}
```

The token appears only because `EXPOSE_RESET_TOKEN=true`. In any other environment that field is
null and the token travels by mail. A username that does not exist gets a response that is
identical apart from the missing token:

```
200 If the account exists, a password reset token has been issued. | token: None
```

```bash
curl -s -X POST localhost:8080/api/v1/auth/password-reset/confirm \
  -H 'Content-Type: application/json' \
  -d '{"resetToken":"j5i_ZGN...","newPassword":"Nueva-Clave-Agro-2026!"}'
```

```
200 Password updated. Every session was revoked.
```

The token is single-use, and the reset lifts the lock and bumps `security_stamp`:

```
the token is single-use:  401 Password reset token is expired or already used
login with the new password: 200 Authenticated
```

### The trail

```bash
curl -s "localhost:8080/api/v1/audit/users/2?limit=8" -H "Authorization: Bearer $ADMIN"
```

```
 23  LOGIN_SUCCEEDED            ok=True   Successful login for application 1
 22  PASSWORD_RESET_COMPLETED   ok=True   Password reset completed; all sessions of the grant…
 21  PASSWORD_RESET_REQUESTED   ok=True   Password reset token issued
 20  LOGIN_FAILED               ok=False  Login failed: ACCOUNT_LOCKED
 19  LOGIN_FAILED               ok=False  Login failed: BAD_PASSWORD
 18  ACCOUNT_LOCKED             ok=False  Account locked after 5 consecutive failed attempts
 17  LOGIN_FAILED               ok=False  Login failed: BAD_PASSWORD
 16  LOGIN_FAILED               ok=False  Login failed: BAD_PASSWORD
```

`security_audit` is append-only. There is no write route:

```
POST   /api/v1/audit/users/2 -> 405
DELETE /api/v1/audit/users/2 -> 405
```

Events that touch authorization also carry structured `jsonb` metadata, which stays queryable:

```sql
SELECT id, event_type, metadata->>'roleCode' AS role_code FROM security_audit
 WHERE metadata IS NOT NULL;
```

```
 id |   event_type   |   role_code
----+----------------+----------------
 33 | ACCESS_GRANTED |
 34 | ROLE_ASSIGNED  | FIELD_OPERATOR
```
