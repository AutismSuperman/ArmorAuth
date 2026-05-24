-- V9: Add tenant and organization tables

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
    `enabled` tinyint(1) NOT NULL DEFAULT 1,
    `created_at` datetime(6) NOT NULL,
    `updated_at` datetime(6) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_code` (`tenant_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `organization` (
    `id` varchar(255) NOT NULL,
    `tenant_id` varchar(255) NOT NULL,
    `org_code` varchar(100) NOT NULL,
    `org_name` varchar(200) NOT NULL,
    `description` varchar(500) DEFAULT NULL,
    `logo` varchar(512) DEFAULT NULL,
    `parent_id` varchar(255) DEFAULT NULL,
    `enabled` tinyint(1) NOT NULL DEFAULT 1,
    `created_at` datetime(6) NOT NULL,
    `updated_at` datetime(6) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_org_tenant_id` (`tenant_id`),
    UNIQUE KEY `uk_org_tenant_code` (`tenant_id`, `org_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `organization_member` (
    `id` varchar(255) NOT NULL,
    `org_id` varchar(255) NOT NULL,
    `user_id` varchar(255) NOT NULL,
    `org_role` varchar(100) DEFAULT NULL,
    `created_at` datetime(6) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_org_member` (`org_id`, `user_id`),
    KEY `idx_org_member_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
