-- V5: Add profile JSON field and audit_event table

-- Add profile JSON column to user_info
ALTER TABLE `user_info` ADD COLUMN `profile` clob DEFAULT NULL;

-- Audit event table
CREATE TABLE IF NOT EXISTS `audit_event` (
    `id` varchar(255) NOT NULL,
    `event_type` varchar(100) NOT NULL,
    `principal_name` varchar(255) DEFAULT NULL,
    `resource_type` varchar(100) DEFAULT NULL,
    `resource_id` varchar(255) DEFAULT NULL,
    `detail` clob DEFAULT NULL,
    `ip_address` varchar(50) DEFAULT NULL,
    `user_agent` varchar(500) DEFAULT NULL,
    `created_at` datetime(6) NOT NULL,
    PRIMARY KEY (`id`)
);

CREATE INDEX IF NOT EXISTS idx_audit_event_type ON `audit_event`(`event_type`);
CREATE INDEX IF NOT EXISTS idx_audit_principal ON `audit_event`(`principal_name`);
CREATE INDEX IF NOT EXISTS idx_audit_created_at ON `audit_event`(`created_at`);
