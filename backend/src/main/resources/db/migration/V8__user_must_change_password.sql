-- ============================================================
-- V8: Users must choose their own password after an administrator
--     creates their account or resets their password.
-- ============================================================
ALTER TABLE users
    ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;
