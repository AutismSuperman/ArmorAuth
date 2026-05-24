-- ArmorAuth Schema Initialization (H2)
-- Version: V1

CREATE TABLE IF NOT EXISTS `authorization` (
    `id` varchar(255) NOT NULL,
    `registered_client_id` varchar(255) DEFAULT NULL,
    `principal_name` varchar(255) DEFAULT NULL,
    `state` varchar(255) DEFAULT NULL,
    `authorization_grant_type` varchar(255) DEFAULT NULL,
    `authorized_scopes` varchar(255) DEFAULT NULL,
    `attributes` text DEFAULT NULL,
    `authorization_code_value` text DEFAULT NULL,
    `authorization_code_issued_at` timestamp(6) DEFAULT NULL,
    `authorization_code_expires_at` timestamp(6) DEFAULT NULL,
    `authorization_code_metadata` text DEFAULT NULL,
    `access_token_value` text DEFAULT NULL,
    `access_token_scopes` text DEFAULT NULL,
    `access_token_type` varchar(255) DEFAULT NULL,
    `access_token_issued_at` timestamp(6) DEFAULT NULL,
    `access_token_expires_at` timestamp(6) DEFAULT NULL,
    `access_token_metadata` text DEFAULT NULL,
    `oidc_id_token_value` text DEFAULT NULL,
    `oidc_id_token_issued_at` timestamp(6) DEFAULT NULL,
    `oidc_id_token_expires_at` timestamp(6) DEFAULT NULL,
    `oidc_id_token_claims` text DEFAULT NULL,
    `oidc_id_token_metadata` text DEFAULT NULL,
    `refresh_token_value` text DEFAULT NULL,
    `refresh_token_issued_at` timestamp(6) DEFAULT NULL,
    `refresh_token_expires_at` timestamp(6) DEFAULT NULL,
    `refresh_token_metadata` text DEFAULT NULL,
    `user_code_value` text DEFAULT NULL,
    `user_code_issued_at` timestamp(6) DEFAULT NULL,
    `user_code_expires_at` timestamp(6) DEFAULT NULL,
    `user_code_metadata` text DEFAULT NULL,
    `device_code_value` text DEFAULT NULL,
    `device_code_issued_at` timestamp(6) DEFAULT NULL,
    `device_code_expires_at` timestamp(6) DEFAULT NULL,
    `device_code_metadata` text DEFAULT NULL,
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
    `configuration_metadata` text DEFAULT NULL,
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
    `provider_attributes` text DEFAULT NULL,
    `create_time` timestamp NOT NULL,
    `last_login_time` timestamp NOT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS `user_info` (
    `id` varchar(255) NOT NULL,
    `username` varchar(255) NOT NULL,
    `password` varchar(255) DEFAULT NULL,
    `phone` varchar(20) DEFAULT NULL,
    `display_name` varchar(255) NOT NULL,
    `create_time` timestamp NOT NULL,
    `last_login_time` timestamp NOT NULL,
    `status` int NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS `jwk_key` (
    `id` varchar(255) NOT NULL,
    `kid` varchar(255) NOT NULL,
    `key_type` varchar(20) NOT NULL,
    `algorithm` varchar(50) NOT NULL,
    `public_key` text NOT NULL,
    `private_key` text NOT NULL,
    `status` varchar(20) NOT NULL DEFAULT 'active',
    `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `expires_at` timestamp DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE (`kid`)
);
