#!/usr/bin/env bash
# Extra databases created on the first start of an empty data volume.
#
# The postgres image creates POSTGRES_DB on its own; every other database this instance
# hosts is created here. The scripts in /docker-entrypoint-initdb.d/ only run when the
# data directory is empty, so on a volume that already exists add the database by hand:
#
#   docker exec -u postgres ms-security-db createdb -O msagro msa_operaciones
#
# `docker/README.md` lists what each database is for.
set -euo pipefail

EXTRA_DATABASES="msa_operaciones"

for db in $EXTRA_DATABASES; do
  echo "Creating database '$db' owned by '$POSTGRES_USER'."
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-SQL
    CREATE DATABASE "$db" OWNER "$POSTGRES_USER";
SQL
done
