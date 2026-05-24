-- V17: Add SAML-specific identity provider configuration fields

ALTER TABLE `identity_provider` ADD COLUMN IF NOT EXISTS `saml_entity_id` varchar(500) DEFAULT NULL;
ALTER TABLE `identity_provider` ADD COLUMN IF NOT EXISTS `saml_sso_url` varchar(1000) DEFAULT NULL;
ALTER TABLE `identity_provider` ADD COLUMN IF NOT EXISTS `saml_slo_url` varchar(1000) DEFAULT NULL;
ALTER TABLE `identity_provider` ADD COLUMN IF NOT EXISTS `saml_x509_certificate` clob DEFAULT NULL;
ALTER TABLE `identity_provider` ADD COLUMN IF NOT EXISTS `saml_metadata_url` varchar(1000) DEFAULT NULL;
ALTER TABLE `identity_provider` ADD COLUMN IF NOT EXISTS `saml_sp_entity_id` varchar(500) DEFAULT NULL;
ALTER TABLE `identity_provider` ADD COLUMN IF NOT EXISTS `saml_acs_url` varchar(1000) DEFAULT NULL;
ALTER TABLE `identity_provider` ADD COLUMN IF NOT EXISTS `saml_name_id_format` varchar(200) DEFAULT NULL;
