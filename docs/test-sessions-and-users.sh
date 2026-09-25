#!/usr/bin/env bash
# ─────────────────────────────────────────────
# End-to-end checks of HU-04 (my sessions), HU-05 (modules by role) and HU-82
# (user administration) against a running ms-security with the seed of V18.
#
#   BASE=http://localhost:8081 ./docs/test-sessions-and-users.sh
#
# It writes in the database it points at: it creates a user "qa.<run>" and
# closes sessions of the seed users. Run it against a local or test database.
# ─────────────────────────────────────────────
set -uo pipefail

BASE="${BASE:-http://localhost:8080}"
APP="${APPLICATION_ID:-3}"
SEED_PASSWORD="${SEED_PASSWORD:-Cambiar-2026-Seguro!}"
RUN=$(date +%s)
PASS=0
FAIL=0
BODY=""
CODE=""

call() { # METHOD PATH TOKEN [JSON] [DEVICE]
  local out
  out=$(curl -s -w $'\n%{http_code}' -X "$1" "$BASE$2" \
    -H "Content-Type: application/json" \
    ${3:+-H "Authorization: Bearer $3"} \
    ${5:+-H "X-Device-Id: $5"} \
    ${4:+-d "$4"})
  CODE=${out##*$'\n'}
  BODY=${out%$'\n'*}
}

check() { # STORY TEXT CONDITION
  if eval "$3"; then
    PASS=$((PASS + 1)); printf '  \033[32m✓\033[0m %-6s %s\n' "$1" "$2"
  else
    FAIL=$((FAIL + 1)); printf '  \033[31m✗\033[0m %-6s %s  (HTTP %s: %s)\n' "$1" "$2" "$CODE" "${BODY:0:200}"
  fi
}

jqb() { jq -r "$1" <<<"$BODY"; }

login() { # USER PASSWORD DEVICE → TOKEN, REFRESH
  call POST /api/v1/auth/login "" \
    "{\"applicationId\":$APP,\"username\":\"$1\",\"password\":\"$2\",\"deviceId\":\"$3\"}" "$3"
  TOKEN=$(jqb '.answer.accessToken // empty')
  REFRESH=$(jqb '.answer.refreshToken // empty')
}

echo "ms-security · sessions and users · $BASE"

# ── HU-04 · my sessions ──────────────────────
login tecnico.campo "$SEED_PASSWORD" "qa-phone-$RUN"; PHONE=$TOKEN
login tecnico.campo "$SEED_PASSWORD" "qa-laptop-$RUN"; LAPTOP=$TOKEN; LAPTOP_REFRESH=$REFRESH
check HU-04 "The technician logs in on two devices" '[[ -n $PHONE && -n $LAPTOP ]]'

call GET /api/v1/me/sessions "$PHONE"
check HU-04 "I list my open sessions, with their device" \
  '[[ $CODE == 200 ]] && [[ $(jqb "[.answer[] | select(.deviceId == \"qa-laptop-$RUN\")] | length") == 1 ]]'
LAPTOP_SESSION=$(jqb ".answer[] | select(.deviceId == \"qa-laptop-$RUN\") | .id")

call POST "/api/v1/me/sessions/$LAPTOP_SESSION/revoke" "$PHONE" '{"reason":""}'
check HU-04 "Closing a session asks for a reason" '[[ $CODE == 400 ]]'
call POST "/api/v1/me/sessions/$LAPTOP_SESSION/revoke" "$PHONE" '{"reason":"Presté el portátil"}'
check HU-04 "I close the session of the other device" '[[ $CODE == 200 ]]'
call POST /api/v1/auth/refresh "" "{\"refreshToken\":\"$LAPTOP_REFRESH\"}"
check HU-04 "The closed session cannot refresh any more" '[[ $CODE == 401 ]]'
call GET /api/v1/me/sessions "$PHONE"
check HU-04 "It is gone from the list" \
  '[[ $(jqb "[.answer[] | select(.deviceId == \"qa-laptop-$RUN\")] | length") == 0 ]]'

login punto.venta "$SEED_PASSWORD" "qa-other-$RUN"; OTHER=$TOKEN
call GET /api/v1/me/sessions "$OTHER"
OTHER_SESSION=$(jqb '.answer[0].id')
call POST "/api/v1/me/sessions/$OTHER_SESSION/revoke" "$PHONE" '{"reason":"No es mía"}'
check HU-04 "Nobody closes a session of another user from /me" '[[ $CODE == 404 ]]'

# ── HU-05 · modules by role ──────────────────
call GET /api/v1/me/access "$PHONE"
check HU-05 "The modules come from the permissions of my role" \
  '[[ $(jqb "[.answer[] | select(.applicationId == $APP) | .permissions[]] | index(\"OWN_WORK_ACCESS\") != null") == true ]]'
check HU-05 "A technician never gets project costs (RN-008)" \
  '[[ $(jqb "[.answer[] | select(.applicationId == $APP) | .permissions[]] | index(\"PROJECT_COST_READ\")") == null ]]'

# ── HU-82 · user administration ──────────────
login gerente "$SEED_PASSWORD" "qa-admin-$RUN"; ADMIN=$TOKEN
call GET /api/v1/admin/users "$PHONE"
check HU-82 "A technician does not administer users" '[[ $CODE == 403 ]]'
call GET /api/v1/admin/roles "$ADMIN"
check HU-82 "The administrator sees the roles of the application" \
  '[[ $CODE == 200 ]] && [[ $(jqb "[.answer[].code] | index(\"TECNICO\") != null") == true ]]'
call GET /api/v1/admin/users "$ADMIN"
check HU-82 "The administrator lists the users, with role and state" \
  '[[ $CODE == 200 ]] && [[ $(jqb ".answer[] | select(.username == \"tecnico.campo\") | .roles[0]") == TECNICO ]]'

NEW="qa.$RUN"
USER_JSON="{\"username\":\"$NEW\",\"temporaryPassword\":\"Temporal-2026-QA!\",\"identificationType\":\"CC\",\"identificationNumber\":\"99$RUN\",\"firstName\":\"Prueba\",\"lastName\":\"QA\",\"email\":\"$NEW@msagro.local\",\"roleCode\":\"TECNICO\"}"
call POST /api/v1/admin/users "$ADMIN" "$USER_JSON"
check HU-82 "The administrator creates a user with a role" '[[ $CODE == 201 ]] && [[ $(jqb .answer.mustChangePassword) == true ]]'
NEW_ID=$(jqb .answer.securityUserId)
call POST /api/v1/admin/users "$ADMIN" "$USER_JSON"
check HU-82 "The same username twice is a conflict" '[[ $CODE == 409 ]]'
call POST /api/v1/admin/users "$ADMIN" "${USER_JSON/TECNICO/NO_EXISTE}"
check HU-82 "A role outside the application is refused" '[[ $CODE == 400 ]]'

call PUT "/api/v1/admin/users/$NEW_ID" "$ADMIN" \
  "{\"firstName\":\"Prueba\",\"lastName\":\"Editada\",\"email\":\"$NEW@msagro.local\",\"roleCode\":\"TALLER\"}"
check HU-82 "Editing changes the names and leaves one role" \
  '[[ $CODE == 200 ]] && [[ $(jqb ".answer.roles | join(\",\")") == TALLER ]] && [[ $(jqb .answer.lastName) == Editada ]]'

login "$NEW" "Temporal-2026-QA!" "qa-new-$RUN"; NEW_TOKEN=$TOKEN
check HU-82 "The new user logs in with the temporary password" '[[ -n $NEW_TOKEN ]]'
call GET /api/v1/me "$NEW_TOKEN"
check HU-82 "Its token is valid" '[[ $CODE == 200 ]]'

call GET "/api/v1/admin/users/$NEW_ID/sessions" "$ADMIN"
check HU-82 "The administrator sees the sessions of any user" '[[ $CODE == 200 ]] && [[ $(jqb ".answer | length") -ge 1 ]]'
NEW_SESSION=$(jqb '.answer[0].id')
call POST "/api/v1/admin/users/$NEW_ID/sessions/$NEW_SESSION/revoke" "$ADMIN" '{"reason":"Revisión de seguridad"}'
check HU-82 "The administrator closes a session of any user" '[[ $CODE == 200 ]]'

login "$NEW" "Temporal-2026-QA!" "qa-new2-$RUN"; NEW_TOKEN=$TOKEN
call POST "/api/v1/admin/users/$NEW_ID/deactivate" "$ADMIN" '{"reason":"Salió de la empresa"}'
check HU-82 "The administrator deactivates a user, saying why" '[[ $CODE == 200 ]]'
call GET /api/v1/me "$NEW_TOKEN"
check HU-82 "The token of a deactivated user stops at once" '[[ $CODE == 401 ]]'
login "$NEW" "Temporal-2026-QA!" "qa-new3-$RUN"
check HU-82 "A deactivated user cannot log in" '[[ -z $TOKEN ]]'
call GET /api/v1/admin/users "$ADMIN"
check HU-82 "The history stays: the user is listed as inactive" \
  '[[ $(jqb ".answer[] | select(.username == \"$NEW\") | .active") == false ]]'

call POST "/api/v1/admin/users/$NEW_ID/activate" "$ADMIN"
check HU-82 "The administrator activates the user again" '[[ $CODE == 200 ]]'
call POST "/api/v1/admin/users/$NEW_ID/reset-password" "$ADMIN" '{"temporaryPassword":"Otra-Temporal-2026!"}'
check HU-82 "The administrator resets the password" '[[ $CODE == 200 ]]'
login "$NEW" "Temporal-2026-QA!" "qa-new4-$RUN"
check HU-82 "The old password no longer works" '[[ -z $TOKEN ]]'
login "$NEW" "Otra-Temporal-2026!" "qa-new5-$RUN"
check HU-82 "The temporary password works and must be changed" \
  '[[ -n $TOKEN ]] && [[ $(jqb .answer.mustChangePassword) == true ]]'

ADMIN_ID=$(curl -s "$BASE/api/v1/me" -H "Authorization: Bearer $ADMIN" | jq -r .answer.securityUserId)
call POST "/api/v1/admin/users/$ADMIN_ID/deactivate" "$ADMIN" '{"reason":"prueba"}'
check HU-82 "An administrator cannot deactivate themselves" '[[ $CODE == 400 ]]'

# ── HU-04 · change my password ───────────────
call POST /api/v1/me/change-password "$TOKEN" '{"currentPassword":"Otra-Temporal-2026!","newPassword":"Mi-Clave-Propia-2026!"}'
check HU-04 "I change my password giving the current one" '[[ $CODE == 200 ]]'
call GET /api/v1/me "$TOKEN"
check HU-04 "After the change, the old token no longer stands" '[[ $CODE == 401 ]]'

echo
echo "$PASS passed, $FAIL failed."
[[ $FAIL == 0 ]]
