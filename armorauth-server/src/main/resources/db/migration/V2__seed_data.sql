-- ArmorAuth Seed Data
-- Version: V2

-- OAuth2 clients
INSERT IGNORE INTO `oauth2_client` (`id`, `client_id`, `client_secret`, `client_name`,
    `client_authentication_methods`, `authorization_grant_types`, `redirect_uris`,
    `post_logout_redirect_uris`, `client_id_issued_at`, `client_secret_expires_at`)
VALUES ('2c9c20818099c695018099cbca030000', 'f62ac251-36d7-42c8-9f75-c31c90111bd4',
    '{bcrypt}$2a$10$uHWdt9Ackncw6s5BJlYO9OOdpD3Q44aan0SjttGRCZU2qvvk3fAZO', 'autism',
    'client_secret_basic,client_secret_post', 'authorization_code,client_credentials,refresh_token',
    'http://armorauth-demo:8083/login/oauth2/code/autism', 'http://armorauth-demo:8083/',
    '2022-05-06 22:35:11.000000', NULL);
INSERT IGNORE INTO `oauth2_client` (`id`, `client_id`, `client_secret`, `client_name`,
    `client_authentication_methods`, `authorization_grant_types`, `redirect_uris`,
    `post_logout_redirect_uris`, `client_id_issued_at`, `client_secret_expires_at`)
VALUES ('8a79987882ed8bc10182edb71d5b1007', '8a349006-b8e3-427b-8814-bc4b32e8930a',
    '0c1501f4a8a35db0a725d1f547f5466f', 'silent', 'client_secret_jwt',
    'authorization_code,client_credentials,refresh_token', 'http://armorauth-demo:8084/login/oauth2/code/silent',
    '', '2022-05-06 22:35:11.000000', NULL);
INSERT IGNORE INTO `oauth2_client` (`id`, `client_id`, `client_secret`, `client_name`,
    `client_authentication_methods`, `authorization_grant_types`, `redirect_uris`,
    `post_logout_redirect_uris`, `client_id_issued_at`, `client_secret_expires_at`)
VALUES ('3e82dfde853649a0af86c10b744a88d3', 'b3d64549-0c6b-4306-9170-886dd8652704',
    '', 'quietly', 'private_key_jwt', 'authorization_code,client_credentials,refresh_token',
    'http://armorauth-demo:8084/login/oauth2/code/quietly', '', '2022-05-06 22:35:11.000000', NULL);
INSERT IGNORE INTO `oauth2_client` (`id`, `client_id`, `client_secret`, `client_name`,
    `client_authentication_methods`, `authorization_grant_types`, `redirect_uris`,
    `post_logout_redirect_uris`, `client_id_issued_at`, `client_secret_expires_at`)
VALUES ('22aaa0568121584aa983df7a129fe7fd', '4569bca1-bca7-49eb-a03c-7898e9197d5f',
    '', 'clever', 'none', 'authorization_code,refresh_token',
    'http://armorauth-demo:8085/login/oauth2/code/clever', 'http://armorauth-demo:8085/',
    '2022-05-06 22:35:11.000000', NULL);
INSERT IGNORE INTO `oauth2_client` (`id`, `client_id`, `client_secret`, `client_name`,
    `client_authentication_methods`, `authorization_grant_types`, `redirect_uris`,
    `post_logout_redirect_uris`, `client_id_issued_at`, `client_secret_expires_at`)
VALUES ('9e6ab3c88d89499a8ae055e5e2e89567', '8ee3a98e-89a8-438d-a314-1ef9df815279',
    '', 'device', 'none', 'urn:ietf:params:oauth:grant-type:device_code,refresh_token',
    '', '', '2022-05-06 22:35:11.000000', NULL);
INSERT IGNORE INTO `oauth2_client` (`id`, `client_id`, `client_secret`, `client_name`,
    `client_authentication_methods`, `authorization_grant_types`, `redirect_uris`,
    `post_logout_redirect_uris`, `client_id_issued_at`, `client_secret_expires_at`)
VALUES ('react-spa-pkce-001', 'react-spa-pkce', '', 'React SPA PKCE Sample', 'none',
    'authorization_code,refresh_token', 'http://localhost:3000/callback', 'http://localhost:3000/',
    '2022-05-06 22:35:11.000000', NULL);

-- OAuth2 scopes
INSERT IGNORE INTO `oauth2_scope` VALUES ('f62ac251-36d7-42c8-9f75-c31c90111bd4', 'message.read', '读取信息');
INSERT IGNORE INTO `oauth2_scope` VALUES ('f62ac251-36d7-42c8-9f75-c31c90111bd4', 'message.write', '写入信息');
INSERT IGNORE INTO `oauth2_scope` VALUES ('f62ac251-36d7-42c8-9f75-c31c90111bd4', 'userinfo', '用户信息');
INSERT IGNORE INTO `oauth2_scope` VALUES ('8a349006-b8e3-427b-8814-bc4b32e8930a', 'message.read', '读取信息');
INSERT IGNORE INTO `oauth2_scope` VALUES ('8a349006-b8e3-427b-8814-bc4b32e8930a', 'message.write', '写入信息');
INSERT IGNORE INTO `oauth2_scope` VALUES ('8a349006-b8e3-427b-8814-bc4b32e8930a', 'userinfo', '用户信息');
INSERT IGNORE INTO `oauth2_scope` VALUES ('b3d64549-0c6b-4306-9170-886dd8652704', 'message.read', '读取信息');
INSERT IGNORE INTO `oauth2_scope` VALUES ('b3d64549-0c6b-4306-9170-886dd8652704', 'message.write', '写入信息');
INSERT IGNORE INTO `oauth2_scope` VALUES ('b3d64549-0c6b-4306-9170-886dd8652704', 'userinfo', '用户信息');
INSERT IGNORE INTO `oauth2_scope` VALUES ('4569bca1-bca7-49eb-a03c-7898e9197d5f', 'message.read', '读取信息');
INSERT IGNORE INTO `oauth2_scope` VALUES ('4569bca1-bca7-49eb-a03c-7898e9197d5f', 'message.write', '写入信息');
INSERT IGNORE INTO `oauth2_scope` VALUES ('4569bca1-bca7-49eb-a03c-7898e9197d5f', 'userinfo', '用户信息');
INSERT IGNORE INTO `oauth2_scope` VALUES ('8ee3a98e-89a8-438d-a314-1ef9df815279', 'message.read', '读取信息');
INSERT IGNORE INTO `oauth2_scope` VALUES ('8ee3a98e-89a8-438d-a314-1ef9df815279', 'message.write', '写入信息');
INSERT IGNORE INTO `oauth2_scope` VALUES ('8ee3a98e-89a8-438d-a314-1ef9df815279', 'userinfo', '用户信息');
INSERT IGNORE INTO `oauth2_scope` VALUES ('react-spa-pkce', 'openid', 'OpenID');
INSERT IGNORE INTO `oauth2_scope` VALUES ('react-spa-pkce', 'profile', 'Profile');
INSERT IGNORE INTO `oauth2_scope` VALUES ('react-spa-pkce', 'email', 'Email');
INSERT IGNORE INTO `oauth2_scope` VALUES ('react-spa-pkce', 'message.read', 'Read Messages');
INSERT IGNORE INTO `oauth2_scope` VALUES ('react-spa-pkce', 'message.write', 'Write Messages');

-- OAuth2 client settings
INSERT IGNORE INTO `oauth2_client_settings` VALUES ('f62ac251-36d7-42c8-9f75-c31c90111bd4', '', 1, 0, '');
INSERT IGNORE INTO `oauth2_client_settings` VALUES ('8a349006-b8e3-427b-8814-bc4b32e8930a', '', 1, 0, 'HS256');
INSERT IGNORE INTO `oauth2_client_settings` VALUES ('b3d64549-0c6b-4306-9170-886dd8652704', 'http://armorauth-demo:8084/jwks', 1, 0, 'RS256');
INSERT IGNORE INTO `oauth2_client_settings` VALUES ('4569bca1-bca7-49eb-a03c-7898e9197d5f', '', 1, 1, '');
INSERT IGNORE INTO `oauth2_client_settings` VALUES ('8ee3a98e-89a8-438d-a314-1ef9df815279', '', 1, 0, '');
INSERT IGNORE INTO `oauth2_client_settings` VALUES ('react-spa-pkce', '', 0, 1, '');

-- OAuth2 token settings
INSERT IGNORE INTO `oauth2_token_settings` VALUES ('f62ac251-36d7-42c8-9f75-c31c90111bd4', 300000000000, 3600000000000, 300000000000, 300000000000, 'RS256', 1, 'self-contained');
INSERT IGNORE INTO `oauth2_token_settings` VALUES ('8a349006-b8e3-427b-8814-bc4b32e8930a', 300000000000, 3600000000000, 300000000000, 300000000000, 'RS256', 1, 'self-contained');
INSERT IGNORE INTO `oauth2_token_settings` VALUES ('b3d64549-0c6b-4306-9170-886dd8652704', 300000000000, 3600000000000, 300000000000, 300000000000, 'RS256', 1, 'self-contained');
INSERT IGNORE INTO `oauth2_token_settings` VALUES ('4569bca1-bca7-49eb-a03c-7898e9197d5f', 300000000000, 3600000000000, 300000000000, 300000000000, 'RS256', 1, 'self-contained');
INSERT IGNORE INTO `oauth2_token_settings` VALUES ('8ee3a98e-89a8-438d-a314-1ef9df815279', 300000000000, 3600000000000, 300000000000, 300000000000, 'RS256', 1, 'self-contained');
INSERT IGNORE INTO `oauth2_token_settings` VALUES ('react-spa-pkce', 300000000000, 3600000000000, 300000000000, 300000000000, 'RS256', 0, 'self-contained');

-- Admin user
INSERT IGNORE INTO `user_info` (`id`, `username`, `password`, `phone`, `display_name`, `create_time`, `last_login_time`, `status`)
VALUES ('0d7c83d900a441c988926af0289de0b2', 'admin',
    '{bcrypt}$2a$10$tzEgeAwC8jD8Lxvz.IYiCeM4JOhXvZ2GD0UeBWWbVf3.hpL./my02',
    '13103777777', '管理员', '2022-05-06 22:35:11', '2022-05-06 22:35:11', 0);

-- Built-in roles
INSERT IGNORE INTO `sys_role` VALUES ('1', 'SUPER_ADMIN', '超级管理员', '系统最高权限', 1);
INSERT IGNORE INTO `sys_role` VALUES ('2', 'TENANT_ADMIN', '租户管理员', '租户级别管理权限', 1);
INSERT IGNORE INTO `sys_role` VALUES ('3', 'APPLICATION_ADMIN', '应用管理员', '应用管理权限', 1);
INSERT IGNORE INTO `sys_role` VALUES ('4', 'USER_ADMIN', '用户管理员', '用户管理权限', 1);
INSERT IGNORE INTO `sys_role` VALUES ('5', 'AUDIT_VIEWER', '审计查看者', '审计日志只读权限', 1);
INSERT IGNORE INTO `sys_role` VALUES ('6', 'USER', '普通用户', '基础用户权限', 1);

-- Assign the default admin user the SUPER_ADMIN role
INSERT IGNORE INTO `user_role` VALUES ('1', '0d7c83d900a441c988926af0289de0b2', '1');

-- Built-in permissions
INSERT IGNORE INTO `sys_permission` VALUES ('perm-app-read', 'app:read', '查看应用', 'application', 'read', '查看应用列表和详情', 1);
INSERT IGNORE INTO `sys_permission` VALUES ('perm-app-write', 'app:write', '管理应用', 'application', 'write', '创建、更新、删除应用', 1);
INSERT IGNORE INTO `sys_permission` VALUES ('perm-user-read', 'user:read', '查看用户', 'user', 'read', '查看用户列表和详情', 1);
INSERT IGNORE INTO `sys_permission` VALUES ('perm-user-write', 'user:write', '管理用户', 'user', 'write', '创建、更新、删除用户', 1);
INSERT IGNORE INTO `sys_permission` VALUES ('perm-role-read', 'role:read', '查看角色', 'role', 'read', '查看角色列表', 1);
INSERT IGNORE INTO `sys_permission` VALUES ('perm-role-write', 'role:write', '管理角色', 'role', 'write', '创建、更新、删除角色', 1);
INSERT IGNORE INTO `sys_permission` VALUES ('perm-org-read', 'org:read', '查看组织', 'organization', 'read', '查看组织列表和详情', 1);
INSERT IGNORE INTO `sys_permission` VALUES ('perm-org-write', 'org:write', '管理组织', 'organization', 'write', '创建、更新、删除组织', 1);
INSERT IGNORE INTO `sys_permission` VALUES ('perm-audit-read', 'audit:read', '查看审计日志', 'audit', 'read', '查看审计事件', 1);
INSERT IGNORE INTO `sys_permission` VALUES ('perm-idp-read', 'idp:read', '查看身份源', 'identity_provider', 'read', '查看身份源配置', 1);
INSERT IGNORE INTO `sys_permission` VALUES ('perm-idp-write', 'idp:write', '管理身份源', 'identity_provider', 'write', '创建、更新、删除身份源', 1);
INSERT IGNORE INTO `sys_permission` VALUES ('perm-webhook-read', 'webhook:read', '查看Webhook', 'webhook', 'read', '查看Webhook配置', 1);
INSERT IGNORE INTO `sys_permission` VALUES ('perm-webhook-write', 'webhook:write', '管理Webhook', 'webhook', 'write', '创建、更新、删除Webhook', 1);
