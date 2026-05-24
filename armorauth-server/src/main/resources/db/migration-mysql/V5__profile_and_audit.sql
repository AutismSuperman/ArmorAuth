-- V5: Add profile JSON field and audit_event table

-- Add profile JSON column to user_info
ALTER TABLE `user_info` ADD COLUMN `profile` json DEFAULT NULL AFTER `update_time`;

-- Audit event table
CREATE TABLE IF NOT EXISTS `audit_event` (
    `id` varchar(255) NOT NULL,
    `event_type` varchar(100) NOT NULL,
    `principal_name` varchar(255) DEFAULT NULL,
    `resource_type` varchar(100) DEFAULT NULL,
    `resource_id` varchar(255) DEFAULT NULL,
    `detail` text DEFAULT NULL,
    `ip_address` varchar(50) DEFAULT NULL,
    `user_agent` varchar(500) DEFAULT NULL,
    `created_at` datetime(6) NOT NULL,
    PRIMARY KEY (`id`),
    INDEX idx_audit_event_type (`event_type`),
    INDEX idx_audit_principal (`principal_name`),
    INDEX idx_audit_created_at (`created_at`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
