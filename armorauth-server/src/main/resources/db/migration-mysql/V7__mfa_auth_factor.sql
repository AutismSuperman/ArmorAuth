-- V7: Add auth_factor table for MFA

CREATE TABLE IF NOT EXISTS `auth_factor` (
    `id` varchar(255) NOT NULL,
    `user_id` varchar(255) NOT NULL,
    `factor_type` varchar(50) NOT NULL,
    `name` varchar(100) DEFAULT NULL,
    `secret` varchar(500) DEFAULT NULL,
    `recovery_codes` text DEFAULT NULL,
    `verified` tinyint(1) DEFAULT 0,
    `enabled` tinyint(1) DEFAULT 1,
    `created_at` datetime(6) NOT NULL,
    `last_used_at` datetime(6) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_auth_factor_user_id` (`user_id`),
    KEY `idx_auth_factor_user_type` (`user_id`, `factor_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
