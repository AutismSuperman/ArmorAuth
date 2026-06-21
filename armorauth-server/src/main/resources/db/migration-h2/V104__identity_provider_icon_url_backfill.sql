ALTER TABLE `identity_provider`
    ADD COLUMN IF NOT EXISTS `icon_url` clob DEFAULT NULL;
