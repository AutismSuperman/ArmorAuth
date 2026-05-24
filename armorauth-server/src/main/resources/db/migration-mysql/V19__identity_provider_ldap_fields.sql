-- V19: Add LDAP/AD identity provider fields

ALTER TABLE `identity_provider`
    ADD COLUMN `ldap_url` varchar(1000) DEFAULT NULL AFTER `saml_name_id_format`,
    ADD COLUMN `ldap_base_dn` varchar(500) DEFAULT NULL AFTER `ldap_url`,
    ADD COLUMN `ldap_bind_dn` varchar(500) DEFAULT NULL AFTER `ldap_base_dn`,
    ADD COLUMN `ldap_bind_password` varchar(1000) DEFAULT NULL AFTER `ldap_bind_dn`,
    ADD COLUMN `ldap_user_search_base` varchar(500) DEFAULT NULL AFTER `ldap_bind_password`,
    ADD COLUMN `ldap_user_search_filter` varchar(500) DEFAULT NULL AFTER `ldap_user_search_base`,
    ADD COLUMN `ldap_username_attribute` varchar(100) DEFAULT NULL AFTER `ldap_user_search_filter`,
    ADD COLUMN `ldap_email_attribute` varchar(100) DEFAULT NULL AFTER `ldap_username_attribute`,
    ADD COLUMN `ldap_phone_attribute` varchar(100) DEFAULT NULL AFTER `ldap_email_attribute`,
    ADD COLUMN `ldap_display_name_attribute` varchar(100) DEFAULT NULL AFTER `ldap_phone_attribute`,
    ADD COLUMN `ldap_group_attribute` varchar(100) DEFAULT NULL AFTER `ldap_display_name_attribute`,
    ADD COLUMN `ldap_use_ssl` tinyint(1) DEFAULT 0 AFTER `ldap_group_attribute`,
    ADD COLUMN `ldap_start_tls` tinyint(1) DEFAULT 0 AFTER `ldap_use_ssl`,
    ADD COLUMN `ldap_page_size` int DEFAULT 200 AFTER `ldap_start_tls`;
