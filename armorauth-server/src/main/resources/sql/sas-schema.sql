/*
 Navicat Premium Data Transfer

 Source Server         : 127.0.0.1-mariadb
 Source Server Type    : MariaDB
 Source Server Version : 100407 (10.4.7-MariaDB-1:10.4.7+maria~bionic)
 Source Host           : 127.0.0.1:3306
 Source Schema         : identity_server

 Target Server Type    : MariaDB
 Target Server Version : 100407 (10.4.7-MariaDB-1:10.4.7+maria~bionic)
 File Encoding         : 65001

 Date: 13/08/2023 12:52:12
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for authorization
-- ----------------------------
DROP TABLE IF EXISTS `authorization`;
CREATE TABLE `authorization`  (
                                  `id` varchar(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL,
                                  `registered_client_id` varchar(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                                  `principal_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                                  `state` varchar(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                                  `authorization_grant_type` varchar(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                                  `authorized_scopes` varchar(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                                  `attributes` text CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                                  `authorization_code_value` text CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                                  `authorization_code_issued_at` datetime(6) NULL DEFAULT NULL,
                                  `authorization_code_expires_at` datetime(6) NULL DEFAULT NULL,
                                  `authorization_code_metadata` text CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                                  `access_token_value` text CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                                  `access_token_scopes` text CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                                  `access_token_type` varchar(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                                  `access_token_issued_at` datetime(6) NULL DEFAULT NULL,
                                  `access_token_expires_at` datetime(6) NULL DEFAULT NULL,
                                  `access_token_metadata` text CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                                  `oidc_id_token_value` text CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                                  `oidc_id_token_issued_at` datetime(6) NULL DEFAULT NULL,
                                  `oidc_id_token_expires_at` datetime(6) NULL DEFAULT NULL,
                                  `oidc_id_token_claims` text CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                                  `oidc_id_token_metadata` text CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                                  `refresh_token_value` text CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                                  `refresh_token_issued_at` datetime(6) NULL DEFAULT NULL,
                                  `refresh_token_expires_at` datetime(6) NULL DEFAULT NULL,
                                  `refresh_token_metadata` text CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                                  `user_code_value` text CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                                  `user_code_issued_at` datetime(6) NULL DEFAULT NULL,
                                  `user_code_expires_at` datetime(6) NULL DEFAULT NULL,
                                  `user_code_metadata` text CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                                  `device_code_value` text CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                                  `device_code_issued_at` datetime(6) NULL DEFAULT NULL,
                                  `device_code_expires_at` datetime(6) NULL DEFAULT NULL,
                                  `device_code_metadata` text CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                                  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for authorization_consent
-- ----------------------------
DROP TABLE IF EXISTS `authorization_consent`;
CREATE TABLE `authorization_consent`  (
                                          `principal_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL,
                                          `registered_client_id` varchar(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL,
                                          `authorities` varchar(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                                          PRIMARY KEY (`principal_name`, `registered_client_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for oauth2_authorized_client
-- ----------------------------
DROP TABLE IF EXISTS `oauth2_authorized_client`;
CREATE TABLE `oauth2_authorized_client`  (
                                             `client_registration_id` varchar(100) CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL,
                                             `principal_name` varchar(200) CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL,
                                             `access_token_type` varchar(100) CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL,
                                             `access_token_value` blob NOT NULL,
                                             `access_token_issued_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE CURRENT_TIMESTAMP,
                                             `access_token_expires_at` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
                                             `access_token_scopes` varchar(1000) CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                                             `refresh_token_value` blob NULL DEFAULT NULL,
                                             `refresh_token_issued_at` timestamp NULL DEFAULT NULL,
                                             `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
                                             PRIMARY KEY (`client_registration_id`, `principal_name`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for oauth2_client
-- ----------------------------
DROP TABLE IF EXISTS `oauth2_client`;
CREATE TABLE `oauth2_client`  (
                                  `id` varchar(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL,
                                  `client_id` varchar(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                                  `client_secret` varchar(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                                  `client_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                                  `client_authentication_methods` varchar(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                                  `authorization_grant_types` varchar(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                                  `redirect_uris` varchar(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                                  `post_logout_redirect_uris` varchar(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                                  `client_id_issued_at` datetime(6) NULL DEFAULT NULL,
                                  `client_secret_expires_at` datetime(6) NULL DEFAULT NULL,
                                  PRIMARY KEY (`id`) USING BTREE,
                                  UNIQUE INDEX `UK_drwlno5wbex09l0acnnwecp7r`(`client_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for oauth2_client_registered
-- ----------------------------
DROP TABLE IF EXISTS `oauth2_client_registered`;
CREATE TABLE `oauth2_client_registered`  (
                                             `registration_id` varchar(100) CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL,
                                             `client_id` varchar(100) CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL,
                                             `client_secret` varchar(200) CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                                             `client_authentication_method` varchar(100) CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL,
                                             `authorization_grant_type` varchar(100) CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL,
                                             `client_name` varchar(200) CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                                             `redirect_uri` varchar(1000) CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL,
                                             `scopes` varchar(1000) CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL,
                                             `authorization_uri` varchar(1000) CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                                             `token_uri` varchar(1000) CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL,
                                             `jwk_set_uri` varchar(1000) CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                                             `issuer_uri` varchar(1000) CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                                             `user_info_uri` varchar(1000) CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                                             `user_info_authentication_method` varchar(100) CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                                             `user_name_attribute_name` varchar(100) CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                                             `configuration_metadata` text CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                                             PRIMARY KEY (`registration_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for oauth2_client_settings
-- ----------------------------
DROP TABLE IF EXISTS `oauth2_client_settings`;
CREATE TABLE `oauth2_client_settings`  (
                                           `client_id` varchar(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL,
                                           `jwk_set_url` varchar(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                                           `require_authorization_consent` bit(1) NOT NULL,
                                           `require_proof_key` bit(1) NOT NULL,
                                           `signing_algorithm` varchar(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                                           PRIMARY KEY (`client_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for oauth2_scope
-- ----------------------------
DROP TABLE IF EXISTS `oauth2_scope`;
CREATE TABLE `oauth2_scope`  (
                                 `client_id` varchar(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL,
                                 `scope` varchar(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL,
                                 `description` varchar(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                                 PRIMARY KEY (`client_id`, `scope`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for oauth2_token_settings
-- ----------------------------
DROP TABLE IF EXISTS `oauth2_token_settings`;
CREATE TABLE `oauth2_token_settings`  (
                                          `client_id` varchar(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL,
                                          `access_token_time_to_live` decimal(21, 0) NULL DEFAULT NULL,
                                          `refresh_token_time_to_live` decimal(21, 0) NULL DEFAULT NULL,
                                          `device_code_time_to_live` decimal(21, 0) NULL DEFAULT NULL,
                                          `authorization_code_time_to_live` decimal(21, 0) NULL DEFAULT NULL,
                                          `id_token_signature_algorithm` varchar(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                                          `reuse_refresh_tokens` bit(1) NOT NULL,
                                          `token_format` varchar(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                                          PRIMARY KEY (`client_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_federated_binding
-- ----------------------------
DROP TABLE IF EXISTS `user_federated_binding`;
CREATE TABLE `user_federated_binding`  (
                                           `id` varchar(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL,
                                           `user_id` varchar(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                                           `unique_identification` varchar(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                                           `registration_id` varchar(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                                           PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_info
-- ----------------------------
DROP TABLE IF EXISTS `user_info`;
CREATE TABLE `user_info`  (
                              `id` varchar(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL,
                              `username` varchar(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL,
                              `password` varchar(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL COMMENT '密码',
                              `phone` varchar(20) CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
                              `display_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL,
                              `create_time` datetime NOT NULL,
                              `last_login_time` datetime NOT NULL,
                              `status` int(11) NOT NULL DEFAULT 0,
                              PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_unicode_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
