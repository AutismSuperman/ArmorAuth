-- V19: Add LDAP/AD identity provider fields

ALTER TABLE `identity_provider` ADD COLUMN IF NOT EXISTS `ldap_url` varchar(1000) DEFAULT NULL;
ALTER TABLE `identity_provider` ADD COLUMN IF NOT EXISTS `ldap_base_dn` varchar(500) DEFAULT NULL;
ALTER TABLE `identity_provider` ADD COLUMN IF NOT EXISTS `ldap_bind_dn` varchar(500) DEFAULT NULL;
ALTER TABLE `identity_provider` ADD COLUMN IF NOT EXISTS `ldap_bind_password` varchar(1000) DEFAULT NULL;
ALTER TABLE `identity_provider` ADD COLUMN IF NOT EXISTS `ldap_user_search_base` varchar(500) DEFAULT NULL;
ALTER TABLE `identity_provider` ADD COLUMN IF NOT EXISTS `ldap_user_search_filter` varchar(500) DEFAULT NULL;
ALTER TABLE `identity_provider` ADD COLUMN IF NOT EXISTS `ldap_username_attribute` varchar(100) DEFAULT NULL;
ALTER TABLE `identity_provider` ADD COLUMN IF NOT EXISTS `ldap_email_attribute` varchar(100) DEFAULT NULL;
ALTER TABLE `identity_provider` ADD COLUMN IF NOT EXISTS `ldap_phone_attribute` varchar(100) DEFAULT NULL;
ALTER TABLE `identity_provider` ADD COLUMN IF NOT EXISTS `ldap_display_name_attribute` varchar(100) DEFAULT NULL;
ALTER TABLE `identity_provider` ADD COLUMN IF NOT EXISTS `ldap_group_attribute` varchar(100) DEFAULT NULL;
ALTER TABLE `identity_provider` ADD COLUMN IF NOT EXISTS `ldap_use_ssl` boolean DEFAULT false;
ALTER TABLE `identity_provider` ADD COLUMN IF NOT EXISTS `ldap_start_tls` boolean DEFAULT false;
ALTER TABLE `identity_provider` ADD COLUMN IF NOT EXISTS `ldap_page_size` int DEFAULT 200;
