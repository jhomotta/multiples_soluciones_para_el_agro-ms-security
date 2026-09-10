#!/usr/bin/env bash
# Logs in every reference user seeded by V18__seed_appgestion_rbac.sql and writes each
# request together with its response into one report file.
#
#   ./docs/login-seed-users.sh
#   BASE=http://host:8080 OUT=/tmp/report.txt ./docs/login-seed-users.sh
#   SEED_PASSWORD='otra-clave' ./docs/login-seed-users.sh
#
# The report is plain text and self-contained: it repeats the exact curl invocation before
# every answer, so a failing row can be replayed by hand straight out of the file.
#
# Every login is expected to succeed except usuario.desactivado, whose security_user row is
# seeded with enabled = FALSE. The script exits non-zero when reality and that expectation
# disagree. Requires curl and python3.

# The script uses arrays and [[ ]]. On distributions where /bin/sh is dash, `sh script.sh`
# would die on the first of them, so re-run under bash instead of failing halfway.
if [ -z "${BASH_VERSION:-}" ]; then
  command -v bash >/dev/null 2>&1 || { echo "This script needs bash." >&2; exit 2; }
  exec bash "$0" "$@"
fi

set -uo pipefail

BASE="${BASE:-http://localhost:8080}"
SEED_PASSWORD="${SEED_PASSWORD:-Cambiar-2026-Seguro!}"
APPLICATION_CODE="${APPLICATION_CODE:-APP_GESTION}"
OUT="${OUT:-$(dirname "$0")/login-seed-users.out.txt}"

# Resolving the application id needs the database, because no public endpoint lists
# applications: reading them requires a token, and getting a token requires the very id we are
# looking for. So the lookup is a convenience, never a requirement — set APPLICATION_ID to skip
# it, which is what you want when the script runs away from the database host.
DB_CONTAINER="${DB_CONTAINER:-ms-security-db}"
DB_NAME="${DB_NAME:-security_db}"
DB_USER="${DB_USER:-msagro}"

# Used when the database cannot be reached. APP_GESTION is the third application inserted:
# AGRO_CORE and AGRO_FIELD come from V17, APP_GESTION from V18.
DEFAULT_APPLICATION_ID="${DEFAULT_APPLICATION_ID:-3}"

# username:expected-http-status. The seed grants all eight users access to APP_GESTION;
# only the deactivated account is refused, and the refusal is the generic 401 the endpoint
# returns for every failure so it never reveals why.
USERS=(
  "gerente:200"
  "tecnico.campo:200"
  "tecnico.sin.asignacion:200"
  "punto.venta:200"
  "jefe.taller:200"
  "contabilidad:200"
  "modulo.apagado:200"
  "usuario.desactivado:401"
)

# ── Application id ───────────────────────────
# The login body carries an id, not a code, and the id depends on how many applications
# earlier migrations inserted. Reading it from the database beats hardcoding a number that a
# new V-file would silently invalidate — where the database is reachable.
resolve_application_id() {
  local engine=""
  for candidate in docker podman; do
    command -v "$candidate" >/dev/null 2>&1 || continue
    "$candidate" exec "$DB_CONTAINER" true >/dev/null 2>&1 && { engine="$candidate"; break; }
  done
  [ -n "$engine" ] || return 1
  "$engine" exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -At \
      -c "SELECT id FROM application WHERE code = '$APPLICATION_CODE';" 2>/dev/null
}

APPLICATION_ID_SOURCE="the APPLICATION_ID variable"
if [ -z "${APPLICATION_ID:-}" ]; then
  APPLICATION_ID="$(resolve_application_id)"
  APPLICATION_ID_SOURCE="a lookup in $DB_CONTAINER"
fi
if ! [[ "$APPLICATION_ID" =~ ^[0-9]+$ ]]; then
  APPLICATION_ID="$DEFAULT_APPLICATION_ID"
  APPLICATION_ID_SOURCE="the built-in default"
  echo "Note: could not read the id of $APPLICATION_CODE from $DB_CONTAINER; using $APPLICATION_ID." >&2
  echo "      Export APPLICATION_ID=<id> if that is the wrong one." >&2
fi

# ── Report ───────────────────────────────────
: > "$OUT"
log() { printf '%s\n' "$*" >> "$OUT"; }
rule() { log "────────────────────────────────────────────────────────────────────────"; }

log "Login of the V18 reference users of $APPLICATION_CODE (application id $APPLICATION_ID)"
log "Id taken from: $APPLICATION_ID_SOURCE"
log "Host: $BASE"
log "Date: $(date --iso-8601=seconds)"
rule

passed=0
failed=0
logged_in=0

for entry in "${USERS[@]}"; do
  username="${entry%%:*}"
  expected="${entry##*:}"

  # The device id travels in a header, not in the body: ClientContextResolver reads the
  # client context from the transport so a caller cannot name its own identifiers.
  body=$(python3 -c '
import json, sys
print(json.dumps({"applicationId": int(sys.argv[1]), "username": sys.argv[2], "password": sys.argv[3]}))' \
    "$APPLICATION_ID" "$username" "$SEED_PASSWORD")

  log ""
  log "▸ $username"
  log ""
  log "  REQUEST"
  log "    POST $BASE/api/v1/auth/login"
  log "    Content-Type: application/json"
  log "    X-Device-Id: seed-login-script"
  log "    curl -s -X POST $BASE/api/v1/auth/login \\"
  log "      -H 'Content-Type: application/json' \\"
  log "      -H 'X-Device-Id: seed-login-script' \\"
  log "      -d '$(printf '%s' "$body" | sed "s/$SEED_PASSWORD/<password>/")'"
  log ""

  response=$(curl -s -w '\n%{http_code}' -X POST "$BASE/api/v1/auth/login" \
      -H 'Content-Type: application/json' \
      -H 'X-Device-Id: seed-login-script' \
      -d "$body")
  code="${response##*$'\n'}"
  payload="${response%$'\n'*}"

  log "  RESPONSE"
  log "    HTTP $code"
  log ""
  # Pretty-print when the answer is JSON; echo it raw when it is not, so a proxy error page
  # or an empty body still lands in the report instead of being swallowed.
  if printf '%s' "$payload" | python3 -m json.tool >/dev/null 2>&1; then
    printf '%s' "$payload" | python3 -m json.tool | sed 's/^/    /' >> "$OUT"
  else
    printf '%s\n' "${payload:-<empty body>}" | sed 's/^/    /' >> "$OUT"
  fi

  # The token is the interesting part of a success: its claims are what RBAC decides on.
  if [ "$code" = "200" ]; then
    log ""
    log "  ACCESS TOKEN CLAIMS"
    printf '%s' "$payload" | python3 -c '
import base64, json, sys
answer = json.load(sys.stdin).get("answer") or {}
token = answer.get("accessToken")
if not token:
    print("<no access token in the answer>")
    sys.exit()
segment = token.split(".")[1]
claims = json.loads(base64.urlsafe_b64decode(segment + "=" * (-len(segment) % 4)))
print("sub =", claims.get("sub"), "| app =", claims.get("appId"), "| grant =", claims.get("uaid"))
print("authorities: " + ", ".join(claims.get("authorities", [])))
print("mustChangePassword:", answer.get("mustChangePassword"))' 2>/dev/null | sed 's/^/    /' >> "$OUT"
  fi

  [ "$code" = "200" ] && logged_in=$((logged_in + 1))

  log ""
  if [ "$code" = "$expected" ]; then
    log "  VERDICT: OK (expected HTTP $expected)"
    passed=$((passed + 1))
    printf '  \033[32m✓\033[0m %-24s HTTP %s\n' "$username" "$code"
  else
    log "  VERDICT: MISMATCH (expected HTTP $expected, got $code)"
    failed=$((failed + 1))
    printf '  \033[31m✗\033[0m %-24s HTTP %s (expected %s)\n' "$username" "$code" "$expected"
  fi
  rule
done

log ""
log "Summary: $passed as expected, $failed unexpected, of ${#USERS[@]} users."
printf '\nReport written to %s (%s as expected, %s unexpected).\n' "$OUT" "$passed" "$failed"

# A wrong application id fails every user identically, because a login without an access grant
# is refused with the same generic 401 as a wrong password. Say so, rather than leaving the
# reader to suspect the credentials.
# Counted on successful logins, not on matched expectations: usuario.desactivado is expected to
# be refused, so it passes under a wrong application id too and would mask the problem.
if [ "$logged_in" -eq 0 ]; then
  hint="Every login failed. The application id ($APPLICATION_ID, from $APPLICATION_ID_SOURCE) may be wrong,"
  printf '\n%s\n' "$hint" >&2
  printf 'or the seed password does not match the hashes in the database.\n' >&2
  log ""
  log "$hint or the seed password does not match the hashes in the database."
fi

[ "$failed" -eq 0 ]
