-- V20: Add login page display metadata to identity providers

ALTER TABLE `identity_provider` ADD COLUMN IF NOT EXISTS `icon_key` varchar(80) DEFAULT NULL;
ALTER TABLE `identity_provider` ADD COLUMN IF NOT EXISTS `display_on_login` boolean NOT NULL DEFAULT true;

UPDATE `identity_provider`
SET `icon_key` = LOWER(REPLACE(`provider_type`, '_', '-'))
WHERE `icon_key` IS NULL;
