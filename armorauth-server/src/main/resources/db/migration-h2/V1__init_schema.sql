-- ArmorAuth Schema Initialization (H2)
-- Version: V1

CREATE TABLE IF NOT EXISTS `authorization` (
    `id` varchar(255) NOT NULL,
    `registered_client_id` varchar(255) DEFAULT NULL,
    `principal_name` varchar(255) DEFAULT NULL,
    `state` varchar(255) DEFAULT NULL,
    `authorization_grant_type` varchar(255) DEFAULT NULL,
    `authorized_scopes` varchar(255) DEFAULT NULL,
    `attributes` clob DEFAULT NULL,
    `authorization_code_value` clob DEFAULT NULL,
    `authorization_code_issued_at` timestamp(6) DEFAULT NULL,
    `authorization_code_expires_at` timestamp(6) DEFAULT NULL,
    `authorization_code_metadata` clob DEFAULT NULL,
    `access_token_value` clob DEFAULT NULL,
    `access_token_scopes` clob DEFAULT NULL,
    `access_token_type` varchar(255) DEFAULT NULL,
    `access_token_issued_at` timestamp(6) DEFAULT NULL,
    `access_token_expires_at` timestamp(6) DEFAULT NULL,
    `access_token_metadata` clob DEFAULT NULL,
    `oidc_id_token_value` clob DEFAULT NULL,
    `oidc_id_token_issued_at` timestamp(6) DEFAULT NULL,
    `oidc_id_token_expires_at` timestamp(6) DEFAULT NULL,
    `oidc_id_token_claims` clob DEFAULT NULL,
    `oidc_id_token_metadata` clob DEFAULT NULL,
    `refresh_token_value` clob DEFAULT NULL,
    `refresh_token_issued_at` timestamp(6) DEFAULT NULL,
    `refresh_token_expires_at` timestamp(6) DEFAULT NULL,
    `refresh_token_metadata` clob DEFAULT NULL,
    `user_code_value` clob DEFAULT NULL,
    `user_code_issued_at` timestamp(6) DEFAULT NULL,
    `user_code_expires_at` timestamp(6) DEFAULT NULL,
    `user_code_metadata` clob DEFAULT NULL,
    `device_code_value` clob DEFAULT NULL,
    `device_code_issued_at` timestamp(6) DEFAULT NULL,
    `device_code_expires_at` timestamp(6) DEFAULT NULL,
    `device_code_metadata` clob DEFAULT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS `authorization_consent` (
    `principal_name` varchar(255) NOT NULL,
    `registered_client_id` varchar(255) NOT NULL,
    `authorities` varchar(255) DEFAULT NULL,
    PRIMARY KEY (`principal_name`, `registered_client_id`)
);

CREATE TABLE IF NOT EXISTS `oauth2_authorized_client` (
    `client_registration_id` varchar(100) NOT NULL,
    `principal_name` varchar(200) NOT NULL,
    `access_token_type` varchar(100) NOT NULL,
    `access_token_value` blob NOT NULL,
    `access_token_issued_at` timestamp NOT NULL,
    `access_token_expires_at` timestamp NOT NULL,
    `access_token_scopes` varchar(1000) DEFAULT NULL,
    `refresh_token_value` blob DEFAULT NULL,
    `refresh_token_issued_at` timestamp DEFAULT NULL,
    `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`client_registration_id`, `principal_name`)
);

CREATE TABLE IF NOT EXISTS `oauth2_client` (
    `id` varchar(255) NOT NULL,
    `client_id` varchar(255) DEFAULT NULL,
    `client_secret` varchar(255) DEFAULT NULL,
    `client_name` varchar(255) DEFAULT NULL,
    `client_authentication_methods` varchar(255) DEFAULT NULL,
    `authorization_grant_types` varchar(255) DEFAULT NULL,
    `redirect_uris` varchar(255) DEFAULT NULL,
    `post_logout_redirect_uris` varchar(255) DEFAULT NULL,
    `client_id_issued_at` timestamp(6) DEFAULT NULL,
    `client_secret_expires_at` timestamp(6) DEFAULT NULL,
    `enabled` boolean DEFAULT true,
    `mfa_required` boolean DEFAULT false,
    `role_mfa_required` varchar(500) DEFAULT 'SUPER_ADMIN,TENANT_ADMIN',
    PRIMARY KEY (`id`),
    UNIQUE (`client_id`)
);

CREATE TABLE IF NOT EXISTS `oauth2_client_registered` (
    `registration_id` varchar(100) NOT NULL,
    `client_id` varchar(100) NOT NULL,
    `client_secret` varchar(200) DEFAULT NULL,
    `client_authentication_method` varchar(100) NOT NULL,
    `authorization_grant_type` varchar(100) NOT NULL,
    `client_name` varchar(200) DEFAULT NULL,
    `redirect_uri` varchar(1000) NOT NULL,
    `scopes` varchar(1000) NOT NULL,
    `authorization_uri` varchar(1000) DEFAULT NULL,
    `token_uri` varchar(1000) NOT NULL,
    `jwk_set_uri` varchar(1000) DEFAULT NULL,
    `issuer_uri` varchar(1000) DEFAULT NULL,
    `user_info_uri` varchar(1000) DEFAULT NULL,
    `user_info_authentication_method` varchar(100) DEFAULT NULL,
    `user_name_attribute_name` varchar(100) DEFAULT NULL,
    `configuration_metadata` clob DEFAULT NULL,
    PRIMARY KEY (`registration_id`)
);

CREATE TABLE IF NOT EXISTS `oauth2_client_settings` (
    `client_id` varchar(255) NOT NULL,
    `jwk_set_url` varchar(255) DEFAULT NULL,
    `require_authorization_consent` boolean NOT NULL,
    `require_proof_key` boolean NOT NULL,
    `signing_algorithm` varchar(255) DEFAULT NULL,
    PRIMARY KEY (`client_id`)
);

CREATE TABLE IF NOT EXISTS `oauth2_scope` (
    `client_id` varchar(255) NOT NULL,
    `scope` varchar(255) NOT NULL,
    `description` varchar(255) DEFAULT NULL,
    PRIMARY KEY (`client_id`, `scope`)
);

CREATE TABLE IF NOT EXISTS `oauth2_token_settings` (
    `client_id` varchar(255) NOT NULL,
    `access_token_time_to_live` decimal(21, 0) DEFAULT NULL,
    `refresh_token_time_to_live` decimal(21, 0) DEFAULT NULL,
    `device_code_time_to_live` decimal(21, 0) DEFAULT NULL,
    `authorization_code_time_to_live` decimal(21, 0) DEFAULT NULL,
    `id_token_signature_algorithm` varchar(255) DEFAULT NULL,
    `reuse_refresh_tokens` boolean NOT NULL,
    `token_format` varchar(255) DEFAULT NULL,
    PRIMARY KEY (`client_id`)
);

CREATE TABLE IF NOT EXISTS `user_federated_binding` (
    `id` varchar(255) NOT NULL,
    `user_id` varchar(255) NOT NULL,
    `registration_id` varchar(255) NOT NULL,
    `provider_user_id` varchar(255) NOT NULL,
    `provider_username` varchar(255) DEFAULT NULL,
    `provider_attributes` clob DEFAULT NULL,
    `create_time` timestamp NOT NULL,
    `last_login_time` timestamp NOT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS `user_info` (
    `id` varchar(255) NOT NULL,
    `username` varchar(255) NOT NULL,
    `password` varchar(255) DEFAULT NULL,
    `phone` varchar(20) DEFAULT NULL,
    `email` varchar(255) DEFAULT NULL,
    `avatar` varchar(512) DEFAULT NULL,
    `email_verified` boolean DEFAULT false,
    `phone_verified` boolean DEFAULT false,
    `locked_until` timestamp DEFAULT NULL,
    `display_name` varchar(255) NOT NULL,
    `create_time` timestamp NOT NULL,
    `last_login_time` timestamp DEFAULT NULL,
    `update_time` timestamp DEFAULT NULL,
    `profile` clob DEFAULT NULL,
    `status` int NOT NULL DEFAULT 0,
    `failed_login_attempts` int DEFAULT 0,
    `password_changed_at` timestamp(6) DEFAULT NULL,
    PRIMARY KEY (`id`)
);

CREATE UNIQUE INDEX IF NOT EXISTS `idx_user_info_username` ON `user_info` (`username`);

CREATE TABLE IF NOT EXISTS `jwk_key` (
    `id` varchar(255) NOT NULL,
    `kid` varchar(255) NOT NULL,
    `key_type` varchar(20) NOT NULL,
    `algorithm` varchar(50) NOT NULL,
    `public_key` clob NOT NULL,
    `private_key` clob NOT NULL,
    `status` varchar(20) NOT NULL DEFAULT 'active',
    `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `expires_at` timestamp DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE (`kid`)
);

CREATE TABLE IF NOT EXISTS `sys_role` (
    `id` varchar(255) NOT NULL,
    `role_code` varchar(100) NOT NULL,
    `role_name` varchar(200) NOT NULL,
    `description` varchar(500) DEFAULT NULL,
    `builtin` boolean DEFAULT false,
    PRIMARY KEY (`id`),
    UNIQUE (`role_code`)
);

CREATE TABLE IF NOT EXISTS `user_role` (
    `id` varchar(255) NOT NULL,
    `user_id` varchar(255) NOT NULL,
    `role_id` varchar(255) NOT NULL,
    PRIMARY KEY (`id`)
);

CREATE INDEX IF NOT EXISTS `idx_user_role_user_id` ON `user_role` (`user_id`);
CREATE INDEX IF NOT EXISTS `idx_user_role_role_id` ON `user_role` (`role_id`);

CREATE TABLE IF NOT EXISTS `audit_event` (
    `id` varchar(255) NOT NULL,
    `event_type` varchar(100) NOT NULL,
    `principal_name` varchar(255) DEFAULT NULL,
    `resource_type` varchar(100) DEFAULT NULL,
    `resource_id` varchar(255) DEFAULT NULL,
    `detail` clob DEFAULT NULL,
    `ip_address` varchar(50) DEFAULT NULL,
    `user_agent` varchar(500) DEFAULT NULL,
    `created_at` timestamp(6) NOT NULL,
    PRIMARY KEY (`id`)
);

CREATE INDEX IF NOT EXISTS `idx_audit_event_type` ON `audit_event` (`event_type`);
CREATE INDEX IF NOT EXISTS `idx_audit_principal` ON `audit_event` (`principal_name`);
CREATE INDEX IF NOT EXISTS `idx_audit_created_at` ON `audit_event` (`created_at`);

CREATE TABLE IF NOT EXISTS `auth_factor` (
    `id` varchar(255) NOT NULL,
    `user_id` varchar(255) NOT NULL,
    `factor_type` varchar(50) NOT NULL,
    `name` varchar(100) DEFAULT NULL,
    `secret` varchar(500) DEFAULT NULL,
    `recovery_codes` clob DEFAULT NULL,
    `webauthn_challenge` varchar(500) DEFAULT NULL,
    `credential_id` varchar(500) DEFAULT NULL,
    `credential_public_key` clob DEFAULT NULL,
    `sign_count` bigint DEFAULT NULL,
    `transports` varchar(200) DEFAULT NULL,
    `aaguid` varchar(100) DEFAULT NULL,
    `webauthn_user_handle` varchar(500) DEFAULT NULL,
    `backup_eligible` boolean DEFAULT NULL,
    `backup_state` boolean DEFAULT NULL,
    `verified` boolean DEFAULT false,
    `enabled` boolean DEFAULT true,
    `created_at` timestamp(6) NOT NULL,
    `last_used_at` timestamp(6) DEFAULT NULL,
    PRIMARY KEY (`id`)
);

CREATE INDEX IF NOT EXISTS `idx_auth_factor_user_id` ON `auth_factor` (`user_id`);
CREATE INDEX IF NOT EXISTS `idx_auth_factor_user_type` ON `auth_factor` (`user_id`, `factor_type`);
CREATE INDEX IF NOT EXISTS `idx_auth_factor_credential_id` ON `auth_factor` (`credential_id`);

CREATE TABLE IF NOT EXISTS `password_history` (
    `id` varchar(255) NOT NULL,
    `user_id` varchar(255) NOT NULL,
    `password_hash` varchar(500) NOT NULL,
    `created_at` timestamp(6) NOT NULL,
    PRIMARY KEY (`id`)
);

CREATE INDEX IF NOT EXISTS `idx_password_history_user_id` ON `password_history` (`user_id`);

CREATE TABLE IF NOT EXISTS `tenant` (
    `id` varchar(255) NOT NULL,
    `tenant_code` varchar(100) NOT NULL,
    `tenant_name` varchar(200) NOT NULL,
    `description` varchar(500) DEFAULT NULL,
    `logo` varchar(512) DEFAULT NULL,
    `primary_color` varchar(20) DEFAULT NULL,
    `custom_domain` varchar(255) DEFAULT NULL,
    `login_page_title` varchar(200) DEFAULT NULL,
    `privacy_policy_url` varchar(512) DEFAULT NULL,
    `terms_of_service_url` varchar(512) DEFAULT NULL,
    `enabled` boolean NOT NULL DEFAULT true,
    `created_at` timestamp(6) NOT NULL,
    `updated_at` timestamp(6) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE (`tenant_code`)
);

CREATE TABLE IF NOT EXISTS `organization` (
    `id` varchar(255) NOT NULL,
    `tenant_id` varchar(255) NOT NULL,
    `org_code` varchar(100) NOT NULL,
    `org_name` varchar(200) NOT NULL,
    `description` varchar(500) DEFAULT NULL,
    `logo` varchar(512) DEFAULT NULL,
    `parent_id` varchar(255) DEFAULT NULL,
    `enabled` boolean NOT NULL DEFAULT true,
    `created_at` timestamp(6) NOT NULL,
    `updated_at` timestamp(6) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE (`tenant_id`, `org_code`)
);

CREATE INDEX IF NOT EXISTS `idx_org_tenant_id` ON `organization` (`tenant_id`);

CREATE TABLE IF NOT EXISTS `organization_member` (
    `id` varchar(255) NOT NULL,
    `org_id` varchar(255) NOT NULL,
    `user_id` varchar(255) NOT NULL,
    `org_role` varchar(100) DEFAULT NULL,
    `created_at` timestamp(6) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE (`org_id`, `user_id`)
);

CREATE INDEX IF NOT EXISTS `idx_org_member_user_id` ON `organization_member` (`user_id`);

CREATE TABLE IF NOT EXISTS `identity_provider` (
    `id` varchar(255) NOT NULL,
    `provider_name` varchar(100) NOT NULL,
    `provider_type` varchar(50) NOT NULL,
    `registration_id` varchar(100) NOT NULL,
    `client_id` varchar(500) DEFAULT NULL,
    `client_secret` varchar(500) DEFAULT NULL,
    `authorization_uri` varchar(1000) DEFAULT NULL,
    `token_uri` varchar(1000) DEFAULT NULL,
    `userinfo_uri` varchar(1000) DEFAULT NULL,
    `jwk_set_uri` varchar(1000) DEFAULT NULL,
    `scopes` varchar(500) DEFAULT NULL,
    `icon_key` varchar(80) DEFAULT NULL,
    `icon_url` clob DEFAULT NULL,
    `display_on_login` boolean NOT NULL DEFAULT true,
    `attribute_mapping` clob DEFAULT NULL,
    `linking_strategy` varchar(50) DEFAULT 'AUTO_REGISTER',
    `saml_entity_id` varchar(500) DEFAULT NULL,
    `saml_sso_url` varchar(1000) DEFAULT NULL,
    `saml_slo_url` varchar(1000) DEFAULT NULL,
    `saml_x509_certificate` clob DEFAULT NULL,
    `saml_metadata_url` varchar(1000) DEFAULT NULL,
    `saml_sp_entity_id` varchar(500) DEFAULT NULL,
    `saml_acs_url` varchar(1000) DEFAULT NULL,
    `saml_name_id_format` varchar(200) DEFAULT NULL,
    `ldap_url` varchar(1000) DEFAULT NULL,
    `ldap_base_dn` varchar(500) DEFAULT NULL,
    `ldap_bind_dn` varchar(500) DEFAULT NULL,
    `ldap_bind_password` varchar(1000) DEFAULT NULL,
    `ldap_user_search_base` varchar(500) DEFAULT NULL,
    `ldap_user_search_filter` varchar(500) DEFAULT NULL,
    `ldap_username_attribute` varchar(100) DEFAULT NULL,
    `ldap_email_attribute` varchar(100) DEFAULT NULL,
    `ldap_phone_attribute` varchar(100) DEFAULT NULL,
    `ldap_display_name_attribute` varchar(100) DEFAULT NULL,
    `ldap_group_attribute` varchar(100) DEFAULT NULL,
    `ldap_use_ssl` boolean DEFAULT false,
    `ldap_start_tls` boolean DEFAULT false,
    `ldap_page_size` int DEFAULT 200,
    `display_order` int DEFAULT 0,
    `enabled` boolean NOT NULL DEFAULT true,
    `created_at` timestamp(6) NOT NULL,
    `updated_at` timestamp(6) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE (`registration_id`)
);

CREATE TABLE IF NOT EXISTS `webhook_endpoint` (
    `id` varchar(255) NOT NULL,
    `name` varchar(200) NOT NULL,
    `url` varchar(1000) NOT NULL,
    `secret` varchar(500) DEFAULT NULL,
    `event_types` varchar(1000) DEFAULT NULL,
    `enabled` boolean NOT NULL DEFAULT true,
    `created_at` timestamp(6) NOT NULL,
    `updated_at` timestamp(6) DEFAULT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS `webhook_delivery` (
    `id` varchar(255) NOT NULL,
    `endpoint_id` varchar(255) NOT NULL,
    `event_type` varchar(100) NOT NULL,
    `payload` clob DEFAULT NULL,
    `response_status` int DEFAULT NULL,
    `response_body` clob DEFAULT NULL,
    `success` boolean DEFAULT NULL,
    `retry_count` int DEFAULT 0,
    `created_at` timestamp(6) NOT NULL,
    PRIMARY KEY (`id`)
);

CREATE INDEX IF NOT EXISTS `idx_webhook_delivery_endpoint` ON `webhook_delivery` (`endpoint_id`);
CREATE INDEX IF NOT EXISTS `idx_webhook_delivery_created` ON `webhook_delivery` (`created_at`);

CREATE TABLE IF NOT EXISTS `token_statistics` (
    `id` varchar(255) NOT NULL,
    `client_id` varchar(255) NOT NULL,
    `grant_type` varchar(100) NOT NULL,
    `token_type` varchar(50) NOT NULL,
    `count` bigint NOT NULL DEFAULT 0,
    `last_issued_at` timestamp(6) DEFAULT NULL,
    `date` date NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE (`client_id`, `grant_type`, `token_type`, `date`)
);

CREATE TABLE IF NOT EXISTS `sys_permission` (
    `id` varchar(255) NOT NULL,
    `permission_code` varchar(200) NOT NULL,
    `permission_name` varchar(200) NOT NULL,
    `resource_type` varchar(100) DEFAULT NULL,
    `action` varchar(100) DEFAULT NULL,
    `description` varchar(500) DEFAULT NULL,
    `builtin` boolean DEFAULT false,
    PRIMARY KEY (`id`),
    UNIQUE (`permission_code`)
);

CREATE TABLE IF NOT EXISTS `sys_role_permission` (
    `id` varchar(255) NOT NULL,
    `role_id` varchar(255) NOT NULL,
    `permission_id` varchar(255) NOT NULL,
    PRIMARY KEY (`id`)
);

CREATE INDEX IF NOT EXISTS `idx_role_permission_role` ON `sys_role_permission` (`role_id`);
CREATE INDEX IF NOT EXISTS `idx_role_permission_perm` ON `sys_role_permission` (`permission_id`);

CREATE TABLE IF NOT EXISTS `identity_provider_display_preference` (
    `registration_id` varchar(100) NOT NULL,
    `display_on_login` boolean NOT NULL DEFAULT true,
    `updated_at` timestamp NOT NULL,
    PRIMARY KEY (`registration_id`)
);
