#!/usr/bin/env bash
# Local PostgreSQL for ms-security, without a compose provider.
#
# `podman` on its own cannot read compose.yaml — it shells out to docker-compose or
# podman-compose. Where neither is installed, this script creates exactly the container and
# volume that docker/compose.yaml describes, so the two are interchangeable.
#
#   ./docker/postgres.sh up | stop | start | status | logs | psql | destroy
#
# The credentials are local-development values that match .env.example. Not secrets, and
# not to be reused anywhere else.
set -euo pipefail

IMAGE="docker.io/library/postgres:16-alpine"
CONTAINER="ms-security-db"
VOLUME="ms-security-pgdata"
DB="security_db"
USER_NAME="msagro"
PASSWORD="msagro_local"
PORT="5432"

ENGINE="${CONTAINER_ENGINE:-}"
if [ -z "$ENGINE" ]; then
  if command -v podman >/dev/null 2>&1; then
    ENGINE=podman
  elif command -v docker >/dev/null 2>&1; then
    ENGINE=docker
  else
    echo "Neither podman nor docker is installed." >&2
    exit 1
  fi
fi

exists() { "$ENGINE" container exists "$CONTAINER" 2>/dev/null || "$ENGINE" inspect "$CONTAINER" >/dev/null 2>&1; }

wait_healthy() {
  local status=""
  for _ in $(seq 1 60); do
    status="$("$ENGINE" inspect -f '{{.State.Health.Status}}' "$CONTAINER" 2>/dev/null || true)"
    [ "$status" = "healthy" ] && { echo "$CONTAINER is healthy on port $PORT."; return 0; }
    sleep 2
  done
  echo "$CONTAINER did not become healthy (last status: ${status:-unknown}). See: $0 logs" >&2
  return 1
}

case "${1:-up}" in
  up)
    if exists; then
      echo "$CONTAINER already exists; starting it."
      "$ENGINE" start "$CONTAINER" >/dev/null
    else
      "$ENGINE" volume create "$VOLUME" >/dev/null 2>&1 || true
      "$ENGINE" run -d \
        --name "$CONTAINER" \
        --restart unless-stopped \
        -e POSTGRES_DB="$DB" \
        -e POSTGRES_USER="$USER_NAME" \
        -e POSTGRES_PASSWORD="$PASSWORD" \
        -p "${PORT}:5432" \
        -v "${VOLUME}:/var/lib/postgresql/data" \
        --health-cmd "pg_isready -U $USER_NAME -d $DB" \
        --health-interval 5s \
        --health-timeout 5s \
        --health-retries 10 \
        "$IMAGE" >/dev/null
    fi
    wait_healthy
    ;;
  stop)   "$ENGINE" stop "$CONTAINER" >/dev/null && echo "$CONTAINER stopped; the data volume is untouched." ;;
  start)  "$ENGINE" start "$CONTAINER" >/dev/null && wait_healthy ;;
  status) "$ENGINE" ps -a --filter "name=^${CONTAINER}$" ;;
  logs)   "$ENGINE" logs -f "$CONTAINER" ;;
  psql)   "$ENGINE" exec -it "$CONTAINER" psql -U "$USER_NAME" -d "$DB" ;;
  destroy)
    read -r -p "Remove $CONTAINER and volume $VOLUME? All local data is lost. [y/N] " reply
    case "$reply" in
      [yY]*)
        "$ENGINE" rm -f "$CONTAINER" >/dev/null 2>&1 || true
        "$ENGINE" volume rm "$VOLUME" >/dev/null 2>&1 || true
        echo "Removed."
        ;;
      *) echo "Cancelled." ;;
    esac
    ;;
  *)
    echo "usage: $0 {up|stop|start|status|logs|psql|destroy}" >&2
    exit 2
    ;;
esac
