CREATE TABLE IF NOT EXISTS admin_relationship_action_logs (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    mentor_id BIGINT NOT NULL,
    admin_id BIGINT NOT NULL,
    student_name_snapshot VARCHAR(255) NOT NULL,
    mentor_name_snapshot VARCHAR(255) NOT NULL,
    admin_name_snapshot VARCHAR(255) NOT NULL,
    action_type VARCHAR(32) NOT NULL,
    action_reason LONGTEXT NULL,
    action_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_admin_relationship_action_logs_student_id (student_id),
    INDEX idx_admin_relationship_action_logs_mentor_id (mentor_id),
    INDEX idx_admin_relationship_action_logs_action_date (action_date)
);
