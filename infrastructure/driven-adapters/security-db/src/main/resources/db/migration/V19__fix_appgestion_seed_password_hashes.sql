-- ─────────────────────────────────────────────
-- Repairs the credentials of the eight reference users seeded in V18.
--
-- The hashes written by V18 have the right shape — the {argon2} prefix, the parameter block
-- of Argon2PasswordHasherAdapter, a 16-byte salt and a 32-byte digest — but they were never
-- produced by actually running Argon2id over a password. Verification parses them and always
-- fails, so every one of those accounts answered 401 with failure_reason = BAD_PASSWORD and
-- no password could ever open them.
--
-- V18 is already applied in existing databases, so its checksum must not change. This
-- migration overwrites the credentials instead, which leaves a fresh database and an existing
-- one at the same end state.
--
-- The hashes below are real: each was produced with m=19456, t=2, p=1, a 16-byte random salt
-- and a 32-byte output — the very parameters of Argon2PasswordHasherAdapter — and verified
-- against the password before being written here. Every row carries its own salt, so two
-- users sharing a password do not share a hash.
--
-- Shared development password: Cambiar-2026-Seguro!
-- A local-only value for testing and acceptance, never to be reused anywhere else.
-- must_change_password stays TRUE, so none of these credentials survives a first real login.
-- ─────────────────────────────────────────────

UPDATE security_user u
   SET password_hash = s.password_hash
  FROM (VALUES
    ('gerente',
     '{argon2}$argon2id$v=19$m=19456,t=2,p=1$URCkfPrSfF59BQXaCOQeTA$ydlJ7e1Tjf98h/ccBCjW3JPGX3GV0IxuIOtqEUUJv9g'),
    ('tecnico.campo',
     '{argon2}$argon2id$v=19$m=19456,t=2,p=1$HuUEQYjbsWFTFS8RILkK9Q$dnCayo4r9KNw55ClYpDRkNlbtfwqFGhm1HRyYH5OOGo'),
    ('tecnico.sin.asignacion',
     '{argon2}$argon2id$v=19$m=19456,t=2,p=1$lpW2rnZdPodWuRufE3kvMA$g8s7vBsST8pqi3A7m8hnrlizDbZMbfEm1cmah7GnvpA'),
    ('punto.venta',
     '{argon2}$argon2id$v=19$m=19456,t=2,p=1$fX7Yo9oxsbyhPaUtLK32bg$xQ86djIkYvv6Va4HmLRj5gjvMkgqehD5KfzAoJZSxGM'),
    ('jefe.taller',
     '{argon2}$argon2id$v=19$m=19456,t=2,p=1$RMfbEFz+2ONsuwco/GaPIQ$e/l4CTpWaDPTsOqq6aCsdrf4lym8roBmZYuzT9z6mZk'),
    ('contabilidad',
     '{argon2}$argon2id$v=19$m=19456,t=2,p=1$0rKO3nz7FDWvxd1gBRDbPg$8QX1R2B/9wUczWDqUwqE4u5yu5J2e3fKXucjpsAg7oY'),
    ('modulo.apagado',
     '{argon2}$argon2id$v=19$m=19456,t=2,p=1$yCNJJzLcZoOfH9O8iUnf5Q$qTJvh6SJNqZpBLIw0f7UGpIFpuREJRZQGoZMP28x7Zg'),
    -- Its password is repaired like the rest, so the account is refused for the one reason
    -- the scenario is about: security_user.enabled is FALSE, not a credential that cannot work.
    ('usuario.desactivado',
     '{argon2}$argon2id$v=19$m=19456,t=2,p=1$g488ZNcXHTGlgrMQmFosRg$Jdvz7TCGxwd5JpVDV4Cd7VA/xEEsR0kDV511ydEQHOI')
  ) AS s(username, password_hash)
 WHERE u.username = s.username;

-- ── Lockout reset ────────────────────────────
-- The broken hashes produced real failed logins while they were in place, and every one of
-- them incremented failed_attempts. Left alone, that counter would lock an account whose
-- password is now correct, on the very first attempt after this repair.
--
-- Only these counters are cleared. The rows in login_attempt stay untouched: LoginAttemptAdapter
-- is insert-only by design because attempts are evidence, and erasing the failures would erase
-- the record of this defect along with them.
UPDATE security_user
   SET failed_attempts = 0,
       locked          = FALSE,
       locked_until    = NULL
 WHERE username IN (
       'gerente', 'tecnico.campo', 'tecnico.sin.asignacion', 'punto.venta',
       'jefe.taller', 'contabilidad', 'modulo.apagado', 'usuario.desactivado');
