-- V20: Add login page display metadata to identity providers

ALTER TABLE `identity_provider`
    ADD COLUMN `icon_key` varchar(80) DEFAULT NULL AFTER `scopes`,
    ADD COLUMN `display_on_login` tinyint(1) NOT NULL DEFAULT 1 AFTER `icon_key`;

UPDATE `identity_provider`
SET `icon_key` = LOWER(REPLACE(`provider_type`, '_', '-'))
WHERE `icon_key` IS NULL;
