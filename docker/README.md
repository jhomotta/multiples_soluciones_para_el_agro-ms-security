# docker/ — local infrastructure

Everything ms-security needs to run on a developer machine. Today that is a single
PostgreSQL 16 instance; anything else local (a cache, a mail catcher) belongs here too.

| File | What it is |
| --- | --- |
| `compose.yaml` | PostgreSQL 16 on port 5432, named volume, healthcheck, `restart: unless-stopped` |
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
docker compose -f docker/compose.yaml up -d     # or: podman compose -f docker/compose.yaml up -d
docker compose -f docker/compose.yaml down      # stop; the volume and its data stay
```

### Always on

The container is declared `restart: unless-stopped`, so the engine brings it back after a
crash and after a reboot. `up -d` once is enough — there is no need to start it again on
every login. Two things it depends on:

* The engine itself must start at boot: `systemctl enable --now docker`.
* The policy only survives an explicit stop as a *stop*. `docker compose down` or
  `docker stop ms-security-db` keeps it down until the next `up -d`, which is the point of
  `unless-stopped` rather than `always`.

Under Podman the same key is accepted, but a rootless container needs
`systemctl --user enable podman-restart.service` (or a quadlet unit) to come back after a
reboot — the policy alone only covers crashes while the Podman service is alive.

> Docker and Podman keep separate volumes, so `ms-security-pgdata` under one engine is not
> the one under the other. Moving between them means a `pg_dump` out and a `psql` back in;
> otherwise the new engine starts from an empty database and Flyway replays every migration.

## Without compose

`podman` alone cannot read a compose file — it delegates to `docker-compose` or
`podman-compose`, and fails with *"looking up compose provider failed"* when neither is
installed. Either install one (`pip install podman-compose`, or `dnf install podman-compose`),
or use the script, which creates exactly the same container and volume (also with
`--restart unless-stopped`):

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
