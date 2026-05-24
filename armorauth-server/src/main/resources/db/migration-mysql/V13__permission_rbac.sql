-- V13: Add permission and role-permission tables for RBAC

CREATE TABLE IF NOT EXISTS `sys_permission` (
    `id` varchar(255) NOT NULL,
    `permission_code` varchar(200) NOT NULL,
    `permission_name` varchar(200) NOT NULL,
    `resource_type` varchar(100) DEFAULT NULL,
    `action` varchar(100) DEFAULT NULL,
    `description` varchar(500) DEFAULT NULL,
    `builtin` tinyint(1) DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_permission_code` (`permission_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `sys_role_permission` (
    `id` varchar(255) NOT NULL,
    `role_id` varchar(255) NOT NULL,
    `permission_id` varchar(255) NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_role_permission_role` (`role_id`),
    KEY `idx_role_permission_perm` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed built-in permissions
INSERT INTO `sys_permission` (`id`, `permission_code`, `permission_name`, `resource_type`, `action`, `description`, `builtin`)
VALUES
    ('perm-app-read', 'app:read', '查看应用', 'application', 'read', '查看应用列表和详情', 1),
    ('perm-app-write', 'app:write', '管理应用', 'application', 'write', '创建、更新、删除应用', 1),
    ('perm-user-read', 'user:read', '查看用户', 'user', 'read', '查看用户列表和详情', 1),
    ('perm-user-write', 'user:write', '管理用户', 'user', 'write', '创建、更新、删除用户', 1),
    ('perm-role-read', 'role:read', '查看角色', 'role', 'read', '查看角色列表', 1),
    ('perm-role-write', 'role:write', '管理角色', 'role', 'write', '创建、更新、删除角色', 1),
    ('perm-org-read', 'org:read', '查看组织', 'organization', 'read', '查看组织列表和详情', 1),
    ('perm-org-write', 'org:write', '管理组织', 'organization', 'write', '创建、更新、删除组织', 1),
    ('perm-audit-read', 'audit:read', '查看审计日志', 'audit', 'read', '查看审计事件', 1),
    ('perm-idp-read', 'idp:read', '查看身份源', 'identity_provider', 'read', '查看身份源配置', 1),
    ('perm-idp-write', 'idp:write', '管理身份源', 'identity_provider', 'write', '创建、更新、删除身份源', 1),
    ('perm-webhook-read', 'webhook:read', '查看Webhook', 'webhook', 'read', '查看Webhook配置', 1),
    ('perm-webhook-write', 'webhook:write', '管理Webhook', 'webhook', 'write', '创建、更新、删除Webhook', 1);
