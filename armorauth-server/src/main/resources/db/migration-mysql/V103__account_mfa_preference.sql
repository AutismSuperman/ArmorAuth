ALTER TABLE `user_info`
    ADD COLUMN `mfa_enabled` tinyint(1) NOT NULL DEFAULT 0;
