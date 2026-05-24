-- V17: Add SAML-specific identity provider configuration fields

ALTER TABLE `identity_provider`
    ADD COLUMN `saml_entity_id` varchar(500) DEFAULT NULL,
    ADD COLUMN `saml_sso_url` varchar(1000) DEFAULT NULL,
    ADD COLUMN `saml_slo_url` varchar(1000) DEFAULT NULL,
    ADD COLUMN `saml_x509_certificate` clob DEFAULT NULL,
    ADD COLUMN `saml_metadata_url` varchar(1000) DEFAULT NULL,
    ADD COLUMN `saml_sp_entity_id` varchar(500) DEFAULT NULL,
    ADD COLUMN `saml_acs_url` varchar(1000) DEFAULT NULL,
    ADD COLUMN `saml_name_id_format` varchar(200) DEFAULT NULL;
