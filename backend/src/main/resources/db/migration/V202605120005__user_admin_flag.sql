-- Iter 6: lightweight admin distinction so the deactivation / tombstone
-- endpoints can require an elevated role. Phase 2 will replace this with
-- group-based authorisation against the Entra tenant.

ALTER TABLE user_account ADD COLUMN is_admin BOOLEAN NOT NULL DEFAULT FALSE;
