-- V10: Add identity_provider table

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
    `attribute_mapping` text DEFAULT NULL,
    `linking_strategy` varchar(50) DEFAULT 'AUTO_REGISTER',
    `display_order` int DEFAULT 0,
    `enabled` tinyint(1) NOT NULL DEFAULT 1,
    `created_at` datetime(6) NOT NULL,
    `updated_at` datetime(6) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_idp_registration_id` (`registration_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
