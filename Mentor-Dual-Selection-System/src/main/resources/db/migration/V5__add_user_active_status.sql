ALTER TABLE users
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE users
    ADD COLUMN deactivated_at TIMESTAMP NULL;

ALTER TABLE users
    ADD COLUMN deactivated_reason TEXT NULL;

ALTER TABLE users
    ADD COLUMN deactivated_by_id BIGINT NULL;

CREATE INDEX idx_users_active
    ON users (active);
