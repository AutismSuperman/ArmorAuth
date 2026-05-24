-- V7: Add auth_factor table for MFA

CREATE TABLE IF NOT EXISTS `auth_factor` (
    `id` varchar(255) NOT NULL,
    `user_id` varchar(255) NOT NULL,
    `factor_type` varchar(50) NOT NULL,
    `name` varchar(100) DEFAULT NULL,
    `secret` varchar(500) DEFAULT NULL,
    `recovery_codes` clob DEFAULT NULL,
    `verified` boolean DEFAULT false,
    `enabled` boolean DEFAULT true,
    `created_at` datetime(6) NOT NULL,
    `last_used_at` datetime(6) DEFAULT NULL,
    PRIMARY KEY (`id`)
);

CREATE INDEX IF NOT EXISTS idx_auth_factor_user_id ON `auth_factor`(`user_id`);
CREATE INDEX IF NOT EXISTS idx_auth_factor_user_type ON `auth_factor`(`user_id`, `factor_type`);
