ALTER TABLE `user_info`
    ADD COLUMN `mfa_enabled` boolean NOT NULL DEFAULT false;
