-- V8: Add password history and account lockout fields

CREATE TABLE IF NOT EXISTS `password_history` (
    `id` varchar(255) NOT NULL,
    `user_id` varchar(255) NOT NULL,
    `password_hash` varchar(500) NOT NULL,
    `created_at` datetime(6) NOT NULL,
    PRIMARY KEY (`id`)
);

CREATE INDEX IF NOT EXISTS idx_password_history_user_id ON `password_history`(`user_id`);

ALTER TABLE `user_info` ADD COLUMN IF NOT EXISTS `failed_login_attempts` int DEFAULT 0;
ALTER TABLE `user_info` ADD COLUMN IF NOT EXISTS `password_changed_at` datetime(6) DEFAULT NULL;
