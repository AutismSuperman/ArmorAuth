-- V23: Allow custom uploaded login icons for identity providers

ALTER TABLE `identity_provider` ADD COLUMN `icon_url` text DEFAULT NULL AFTER `icon_key`;
