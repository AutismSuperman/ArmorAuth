-- V13: Add permission and role-permission tables for RBAC

CREATE TABLE IF NOT EXISTS sys_permission (
    id varchar(255) NOT NULL,
    permission_code varchar(200) NOT NULL,
    permission_name varchar(200) NOT NULL,
    resource_type varchar(100) DEFAULT NULL,
    action varchar(100) DEFAULT NULL,
    description varchar(500) DEFAULT NULL,
    builtin boolean DEFAULT false,
    PRIMARY KEY (id),
    CONSTRAINT uk_permission_code UNIQUE (permission_code)
);

CREATE TABLE IF NOT EXISTS sys_role_permission (
    id varchar(255) NOT NULL,
    role_id varchar(255) NOT NULL,
    permission_id varchar(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_role_permission_role ON sys_role_permission(role_id);
CREATE INDEX IF NOT EXISTS idx_role_permission_perm ON sys_role_permission(permission_id);

-- Seed built-in permissions
INSERT INTO sys_permission (id, permission_code, permission_name, resource_type, action, description, builtin)
VALUES
    ('perm-app-read', 'app:read', '查看应用', 'application', 'read', '查看应用列表和详情', true),
    ('perm-app-write', 'app:write', '管理应用', 'application', 'write', '创建、更新、删除应用', true),
    ('perm-user-read', 'user:read', '查看用户', 'user', 'read', '查看用户列表和详情', true),
    ('perm-user-write', 'user:write', '管理用户', 'user', 'write', '创建、更新、删除用户', true),
    ('perm-role-read', 'role:read', '查看角色', 'role', 'read', '查看角色列表', true),
    ('perm-role-write', 'role:write', '管理角色', 'role', 'write', '创建、更新、删除角色', true),
    ('perm-org-read', 'org:read', '查看组织', 'organization', 'read', '查看组织列表和详情', true),
    ('perm-org-write', 'org:write', '管理组织', 'organization', 'write', '创建、更新、删除组织', true),
    ('perm-audit-read', 'audit:read', '查看审计日志', 'audit', 'read', '查看审计事件', true),
    ('perm-idp-read', 'idp:read', '查看身份源', 'identity_provider', 'read', '查看身份源配置', true),
    ('perm-idp-write', 'idp:write', '管理身份源', 'identity_provider', 'write', '创建、更新、删除身份源', true),
    ('perm-webhook-read', 'webhook:read', '查看Webhook', 'webhook', 'read', '查看Webhook配置', true),
    ('perm-webhook-write', 'webhook:write', '管理Webhook', 'webhook', 'write', '创建、更新、删除Webhook', true);
