-- V3: User directory enhancement and RBAC schema (H2)

-- Add new columns to user_info
ALTER TABLE `user_info` ADD COLUMN IF NOT EXISTS `email` varchar(255) DEFAULT NULL;
ALTER TABLE `user_info` ADD COLUMN IF NOT EXISTS `avatar` varchar(512) DEFAULT NULL;
ALTER TABLE `user_info` ADD COLUMN IF NOT EXISTS `email_verified` boolean DEFAULT FALSE;
ALTER TABLE `user_info` ADD COLUMN IF NOT EXISTS `phone_verified` boolean DEFAULT FALSE;
ALTER TABLE `user_info` ADD COLUMN IF NOT EXISTS `locked_until` timestamp DEFAULT NULL;
ALTER TABLE `user_info` ADD COLUMN IF NOT EXISTS `update_time` timestamp DEFAULT NULL;

-- Add unique index on username
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_info_username ON `user_info` (`username`);

-- Role table
CREATE TABLE IF NOT EXISTS `sys_role` (
    `id` varchar(255) NOT NULL,
    `role_code` varchar(100) NOT NULL,
    `role_name` varchar(200) NOT NULL,
    `description` varchar(500) DEFAULT NULL,
    `builtin` boolean DEFAULT FALSE,
    PRIMARY KEY (`id`),
    UNIQUE (`role_code`)
);

-- User-role binding table
CREATE TABLE IF NOT EXISTS `user_role` (
    `id` varchar(255) NOT NULL,
    `user_id` varchar(255) NOT NULL,
    `role_id` varchar(255) NOT NULL,
    PRIMARY KEY (`id`)
);

CREATE INDEX IF NOT EXISTS idx_user_role_user_id ON `user_role` (`user_id`);
CREATE INDEX IF NOT EXISTS idx_user_role_role_id ON `user_role` (`role_id`);

-- Seed default roles
INSERT IGNORE INTO `sys_role` VALUES ('1', 'SUPER_ADMIN', '超级管理员', '系统最高权限', TRUE);
INSERT IGNORE INTO `sys_role` VALUES ('2', 'TENANT_ADMIN', '租户管理员', '租户级别管理权限', TRUE);
INSERT IGNORE INTO `sys_role` VALUES ('3', 'APPLICATION_ADMIN', '应用管理员', '应用管理权限', TRUE);
INSERT IGNORE INTO `sys_role` VALUES ('4', 'USER_ADMIN', '用户管理员', '用户管理权限', TRUE);
INSERT IGNORE INTO `sys_role` VALUES ('5', 'AUDIT_VIEWER', '审计查看者', '审计日志只读权限', TRUE);
INSERT IGNORE INTO `sys_role` VALUES ('6', 'USER', '普通用户', '基础用户权限', TRUE);

-- Assign admin user the SUPER_ADMIN role
INSERT IGNORE INTO `user_role` VALUES ('1', '0d7c83d900a441c988926af0289de0b2', '1');
