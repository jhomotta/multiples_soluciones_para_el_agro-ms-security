# docker/ — local infrastructure

Everything ms-security needs to run on a developer machine. Today that is a single
PostgreSQL 16 instance; anything else local (a cache, a mail catcher) belongs here too.

| File | What it is |
| --- | --- |
| `compose.yaml` | PostgreSQL 16 on port 5432, named volume, healthcheck |
| `postgres.sh` | Starts the same container with plain `podman`/`docker`, for machines with no compose provider |

The credentials below are local-development values. They match `.env.example`, they are not
secrets, and they must not be reused anywhere else.

| Setting | Value |
| --- | --- |
| Database | `security_db` |
| User | `msagro` |
| Password | `msagro_local` |
| Port | `5432` |
| Container | `ms-security-db` |
| Volume | `ms-security-pgdata` |

## With compose

```bash
podman compose -f docker/compose.yaml up -d     # or: docker compose -f docker/compose.yaml up -d
podman compose -f docker/compose.yaml down      # stop; the volume and its data stay
```

## Without compose

`podman` alone cannot read a compose file — it delegates to `docker-compose` or
`podman-compose`, and fails with *"looking up compose provider failed"* when neither is
installed. Either install one (`pip install podman-compose`, or `dnf install podman-compose`),
or use the script, which creates exactly the same container and volume:

```bash
./docker/postgres.sh up        # create and start, wait until healthy
./docker/postgres.sh stop      # stop, keep the data
./docker/postgres.sh psql      # open a psql shell
./docker/postgres.sh destroy   # remove the container AND the volume — deletes all data
```

## Then run the app

```bash
cp .env.example .env && $EDITOR .env && source .env
./gradlew :app-service:bootRun
```

Flyway creates the 14 tables and seeds the catalogues on the first start. To wipe the schema
and replay every migration from scratch, `destroy` the volume and start again.
