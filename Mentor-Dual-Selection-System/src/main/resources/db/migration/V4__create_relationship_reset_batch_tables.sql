ALTER TABLE admin_relationship_action_logs
    ADD COLUMN batch_id BIGINT NULL;

CREATE INDEX idx_admin_relationship_action_logs_batch_id
    ON admin_relationship_action_logs (batch_id);

CREATE TABLE IF NOT EXISTS relationship_reset_batches (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    reason LONGTEXT NULL,
    status VARCHAR(32) NOT NULL,
    created_by_id BIGINT NOT NULL,
    created_by_name_snapshot VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    affected_student_count INT NOT NULL,
    affected_mentor_count INT NOT NULL,
    restore_count INT NOT NULL DEFAULT 0,
    last_restored_at TIMESTAMP NULL,
    last_restored_by_id BIGINT NULL,
    last_restored_by_name_snapshot VARCHAR(255) NULL,
    INDEX idx_relationship_reset_batches_status (status),
    INDEX idx_relationship_reset_batches_created_at (created_at)
);

CREATE TABLE IF NOT EXISTS relationship_reset_batch_snapshots (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    mentor_id BIGINT NOT NULL,
    student_name_snapshot VARCHAR(255) NOT NULL,
    mentor_name_snapshot VARCHAR(255) NOT NULL,
    INDEX idx_relationship_reset_batch_snapshots_batch_id (batch_id),
    INDEX idx_relationship_reset_batch_snapshots_student_id (student_id),
    INDEX idx_relationship_reset_batch_snapshots_mentor_id (mentor_id)
);
