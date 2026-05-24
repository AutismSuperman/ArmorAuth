-- V6: Add enabled column to oauth2_client

ALTER TABLE `oauth2_client` ADD COLUMN `enabled` tinyint(1) DEFAULT 1;
