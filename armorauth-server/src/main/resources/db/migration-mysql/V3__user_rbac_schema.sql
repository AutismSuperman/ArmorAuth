-- V3: User directory enhancement and RBAC schema (MySQL)

-- Add new columns to user_info
ALTER TABLE `user_info` ADD COLUMN `email` varchar(255) DEFAULT NULL AFTER `phone`;
ALTER TABLE `user_info` ADD COLUMN `avatar` varchar(512) DEFAULT NULL AFTER `email`;
ALTER TABLE `user_info` ADD COLUMN `email_verified` boolean DEFAULT FALSE AFTER `avatar`;
ALTER TABLE `user_info` ADD COLUMN `phone_verified` boolean DEFAULT FALSE AFTER `email_verified`;
ALTER TABLE `user_info` ADD COLUMN `locked_until` timestamp DEFAULT NULL AFTER `phone_verified`;
ALTER TABLE `user_info` ADD COLUMN `update_time` timestamp DEFAULT NULL AFTER `last_login_time`;

-- Add unique index on username
ALTER TABLE `user_info` ADD UNIQUE INDEX idx_user_info_username (`username`);

-- Role table
CREATE TABLE IF NOT EXISTS `sys_role` (
    `id` varchar(255) NOT NULL,
    `role_code` varchar(100) NOT NULL,
    `role_name` varchar(200) NOT NULL,
    `description` varchar(500) DEFAULT NULL,
    `builtin` boolean DEFAULT FALSE,
    PRIMARY KEY (`id`),
    UNIQUE KEY uk_role_code (`role_code`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- User-role binding table
CREATE TABLE IF NOT EXISTS `user_role` (
    `id` varchar(255) NOT NULL,
    `user_id` varchar(255) NOT NULL,
    `role_id` varchar(255) NOT NULL,
    PRIMARY KEY (`id`),
    INDEX idx_user_role_user_id (`user_id`),
    INDEX idx_user_role_role_id (`role_id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Seed default roles
INSERT IGNORE INTO `sys_role` VALUES ('1', 'SUPER_ADMIN', '超级管理员', '系统最高权限', TRUE);
INSERT IGNORE INTO `sys_role` VALUES ('2', 'TENANT_ADMIN', '租户管理员', '租户级别管理权限', TRUE);
INSERT IGNORE INTO `sys_role` VALUES ('3', 'APPLICATION_ADMIN', '应用管理员', '应用管理权限', TRUE);
INSERT IGNORE INTO `sys_role` VALUES ('4', 'USER_ADMIN', '用户管理员', '用户管理权限', TRUE);
INSERT IGNORE INTO `sys_role` VALUES ('5', 'AUDIT_VIEWER', '审计查看者', '审计日志只读权限', TRUE);
INSERT IGNORE INTO `sys_role` VALUES ('6', 'USER', '普通用户', '基础用户权限', TRUE);

-- Assign admin user the SUPER_ADMIN role
INSERT IGNORE INTO `user_role` VALUES ('1', '0d7c83d900a441c988926af0289de0b2', '1');
