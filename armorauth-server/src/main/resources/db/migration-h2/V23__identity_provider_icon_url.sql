-- V23: Allow custom uploaded login icons for identity providers

ALTER TABLE `identity_provider` ADD COLUMN IF NOT EXISTS `icon_url` clob DEFAULT NULL;
