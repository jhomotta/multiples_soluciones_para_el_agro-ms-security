-- ─────────────────────────────────────────────
-- PostgreSQL has no "ON UPDATE CURRENT_TIMESTAMP", so a trigger keeps updated_at
-- honest on the tables that carry it. Append-only and insert-only tables
-- (user_role, role_permission, refresh_token, login_attempt,
--  password_reset_token, security_audit) have no updated_at and no trigger.
-- ─────────────────────────────────────────────
CREATE OR REPLACE FUNCTION set_updated_at() RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_company_updated_at
    BEFORE UPDATE ON company
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER tr_application_updated_at
    BEFORE UPDATE ON application
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER tr_profession_updated_at
    BEFORE UPDATE ON profession
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER tr_person_updated_at
    BEFORE UPDATE ON person
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER tr_security_user_updated_at
    BEFORE UPDATE ON security_user
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER tr_user_application_updated_at
    BEFORE UPDATE ON user_application
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER tr_role_updated_at
    BEFORE UPDATE ON role
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER tr_permission_updated_at
    BEFORE UPDATE ON permission
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
