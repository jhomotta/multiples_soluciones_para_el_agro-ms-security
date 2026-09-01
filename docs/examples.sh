#!/usr/bin/env bash
# Runnable version of docs/API_EXAMPLES.md.
#
#   ./docs/examples.sh            # against http://localhost:8080
#   BASE=http://host:8080 ./docs/examples.sh
#
# Assumes a freshly migrated database and a bootstrap administrator created with
# BOOTSTRAP_ADMIN_PASSWORD='Adm1n-Local-2026!'. Requires curl and python3.
set -uo pipefail

BASE="${BASE:-http://localhost:8080}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-Adm1n-Local-2026!}"

say()  { printf '\n\033[1m── %s\033[0m\n' "$*"; }
step() { printf '   %s\n' "$*"; }

# POST <path> <json> [bearer-token]
post() {
  local path="$1" body="$2" token="${3:-}"
  if [ -n "$token" ]; then
    curl -s -X POST "$BASE$path" -H 'Content-Type: application/json' \
         -H "Authorization: Bearer $token" -d "$body"
  else
    curl -s -X POST "$BASE$path" -H 'Content-Type: application/json' -d "$body"
  fi
}

get() { curl -s "$BASE$1" -H "Authorization: Bearer $2"; }

# Prints "<httpStatus> <message>" from the response envelope.
status() { python3 -c 'import sys,json;d=json.load(sys.stdin);print(d["httpStatus"], d["message"])'; }

# Extracts answer.<field>.
field() { python3 -c "import sys,json;print(json.load(sys.stdin)['answer']['$1'])"; }

# ─────────────────────────────────────────────────────────────────────────────
say "Example 1 — the administrator logs in"
LOGIN=$(post /api/v1/auth/login "{\"applicationId\":1,\"username\":\"admin\",\"password\":\"$ADMIN_PASSWORD\"}")
echo "$LOGIN" | status
ADMIN=$(echo "$LOGIN" | field accessToken)
step "authorities in the token:"
echo "$ADMIN" | python3 -c '
import sys, base64, json
payload = sys.stdin.read().strip().split(".")[1]
claims = json.loads(base64.urlsafe_b64decode(payload + "=" * (-len(payload) % 4)))
print("     sub =", claims["sub"], "| app =", claims["appId"], "| grant =", claims["uaid"], "| stamp =", claims["stamp"])
print("     " + ", ".join(claims["authorities"]))'

# ─────────────────────────────────────────────────────────────────────────────
say "Example 2 — a farmer registers and reads their own identity"
post /api/v1/auth/register '{
  "applicationId": 1, "username": "jperez", "password": "Cosecha-2026-Segura!",
  "identificationType": "CC", "identificationNumber": "1098765432",
  "firstName": "Juan", "middleName": "Carlos", "lastName": "Perez", "secondLastName": "Gomez",
  "email": "juan.perez@msagro.local", "mobile": "3001234567", "birthDate": "1990-05-14",
  "professionId": 5
}' | status
FARMER_LOGIN=$(post /api/v1/auth/login '{"applicationId":1,"username":"jperez","password":"Cosecha-2026-Segura!"}')
FARMER=$(echo "$FARMER_LOGIN" | field accessToken)
step "GET /api/v1/me:"
get /api/v1/me "$FARMER" | python3 -m json.tool | sed 's/^/     /'
step "registering the same identification again is refused:"
post /api/v1/auth/register '{
  "applicationId": 1, "username": "otro", "password": "Otra-Clave-Larga-2026!",
  "identificationType": "CC", "identificationNumber": "1098765432",
  "firstName": "Otro", "lastName": "Usuario", "email": "otro@msagro.local"
}' | status

# ─────────────────────────────────────────────────────────────────────────────
say "Example 3 — RBAC: the same route, two callers"
step "the farmer (role USER) tries to create a company:"
post /api/v1/companies '{"code":"OTRA","name":"Otra empresa"}' "$FARMER" | status
step "the administrator creates a profession:"
post /api/v1/professions '{"code":"apicultor","name":"Apicultor","description":"Manejo de colmenas"}' "$ADMIN" \
  | python3 -c 'import sys,json;d=json.load(sys.stdin);a=d["answer"];print("  ",d["httpStatus"],d["message"],"| id",a["id"],"| code",a["code"])'
step "the administrator creates a role in AGRO_FIELD (application 2):"
FIELD_ROLE=$(post /api/v1/roles '{"applicationId":2,"code":"FIELD_OPERATOR","name":"Operario de campo"}' "$ADMIN")
echo "$FIELD_ROLE" | python3 -c 'import sys,json;d=json.load(sys.stdin);a=d["answer"];print("  ",d["httpStatus"],d["message"],"| role",a["id"],"in application",a["applicationId"])'
ROLE_ID=$(echo "$FIELD_ROLE" | field id)

# ─────────────────────────────────────────────────────────────────────────────
say "Example 4 — one person, two applications, different roles"
step "the farmer cannot log into AGRO_FIELD yet:"
post /api/v1/auth/login '{"applicationId":2,"username":"jperez","password":"Cosecha-2026-Segura!"}' | status
step "the administrator grants access to application 2:"
GRANT=$(post /api/v1/access '{"securityUserId":2,"applicationId":2}' "$ADMIN")
echo "$GRANT" | status
GRANT_ID=$(echo "$GRANT" | field id)
step "a role from application 1 is refused for a grant in application 2:"
post /api/v1/access/roles "{\"userApplicationId\":$GRANT_ID,\"roleId\":2}" "$ADMIN" | status
step "the right role is accepted:"
post /api/v1/access/roles "{\"userApplicationId\":$GRANT_ID,\"roleId\":$ROLE_ID}" "$ADMIN" | status
step "the farmer now logs into application 2 with a different authority set:"
post /api/v1/auth/login '{"applicationId":2,"username":"jperez","password":"Cosecha-2026-Segura!"}' \
  | python3 -c 'import sys,json;a=json.load(sys.stdin)["answer"];print("     app",a["applicationId"],"grant",a["userApplicationId"],"→",a["authorities"])'
step "the full picture:"
get /api/v1/access/users/2 "$ADMIN" | python3 -c '
import sys, json
for v in json.load(sys.stdin)["answer"]:
    code, roles, perms = v["applicationCode"], v["roles"], v["permissions"]
    print("     %-12s roles=%s permissions=%s" % (code, roles, perms))'

# ─────────────────────────────────────────────────────────────────────────────
say "Example 5 — token lifecycle, lockout and password reset"
step "rotation:"
T1=$(post /api/v1/auth/login '{"applicationId":1,"username":"jperez","password":"Cosecha-2026-Segura!","deviceId":"pc-jperez"}' | field refreshToken)
T2=$(post /api/v1/auth/refresh "{\"refreshToken\":\"$T1\"}" | field refreshToken)
echo "     T1 ${T1:0:12}…  →  T2 ${T2:0:12}…"
step "replaying T1 is detected as reuse:"
post /api/v1/auth/refresh "{\"refreshToken\":\"$T1\"}" | status
step "and T2 died with it — the whole family was revoked:"
post /api/v1/auth/refresh "{\"refreshToken\":\"$T2\"}" | status

step "five wrong passwords lock the account:"
for i in 1 2 3 4 5; do
  printf "     attempt %d: " "$i"
  post /api/v1/auth/login '{"applicationId":1,"username":"jperez","password":"WrongPassword123"}' | status
done
printf "     correct password now: "
post /api/v1/auth/login '{"applicationId":1,"username":"jperez","password":"Cosecha-2026-Segura!"}' | status

step "password reset (EXPOSE_RESET_TOKEN=true echoes the token for local testing):"
TICKET=$(post /api/v1/auth/password-reset/request '{"applicationId":1,"username":"jperez"}')
RESET=$(echo "$TICKET" | field resetToken)
printf "     an unknown username gets the same answer: "
post /api/v1/auth/password-reset/request '{"applicationId":1,"username":"does-not-exist"}' \
  | python3 -c 'import sys,json;d=json.load(sys.stdin);print(d["httpStatus"], d["message"], "| token:", d["answer"]["resetToken"])'
printf "     confirm: "
post /api/v1/auth/password-reset/confirm "{\"resetToken\":\"$RESET\",\"newPassword\":\"Nueva-Clave-Agro-2026!\"}" | status
printf "     the token is single-use: "
post /api/v1/auth/password-reset/confirm "{\"resetToken\":\"$RESET\",\"newPassword\":\"Otra-Clave-Agro-2026!\"}" | status
printf "     login with the new password (the lock was lifted): "
post /api/v1/auth/login '{"applicationId":1,"username":"jperez","password":"Nueva-Clave-Agro-2026!"}' | status

step "everything above is in the audit trail:"
get "/api/v1/audit/users/2?limit=8" "$ADMIN" | python3 -c '
import sys, json
for e in json.load(sys.stdin)["answer"]:
    print("     %3d  %-26s ok=%-5s %s" % (
        e["id"], e["eventType"], e["success"], (e["description"] or "")[:52]))'

printf '\n\033[1mDone.\033[0m\n'
