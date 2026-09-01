-- ─────────────────────────────────────────────
-- Secondary indexes for the hot lookup paths.
-- Unique constraints already create their own index and are not repeated here.
-- ─────────────────────────────────────────────

CREATE INDEX ix_application_company_active ON application (company_id, active);

CREATE INDEX ix_person_email_lower  ON person (LOWER(email));
CREATE INDEX ix_person_profession   ON person (profession_id);

CREATE INDEX ix_user_application_user ON user_application (security_user_id, active);
CREATE INDEX ix_user_application_app  ON user_application (application_id, active);

CREATE INDEX ix_role_application       ON role (application_id, active);
CREATE INDEX ix_permission_application ON permission (application_id, active);

CREATE INDEX ix_user_role_user_application ON user_role (user_application_id, active);
CREATE INDEX ix_user_role_role             ON user_role (role_id);

CREATE INDEX ix_role_permission_permission ON role_permission (permission_id);

CREATE INDEX ix_refresh_token_user_application ON refresh_token (user_application_id, expires_at);
CREATE INDEX ix_refresh_token_family           ON refresh_token (token_family);

CREATE INDEX ix_login_attempt_username ON login_attempt (username_attempted, attempted_at DESC);
CREATE INDEX ix_login_attempt_ip       ON login_attempt (ip_address, attempted_at DESC);

CREATE INDEX ix_password_reset_token_user_application
    ON password_reset_token (user_application_id, expires_at);

CREATE INDEX ix_security_audit_user          ON security_audit (security_user_id, created_at DESC);
CREATE INDEX ix_security_audit_application   ON security_audit (application_id, created_at DESC);
CREATE INDEX ix_security_audit_correlation   ON security_audit (correlation_id);
