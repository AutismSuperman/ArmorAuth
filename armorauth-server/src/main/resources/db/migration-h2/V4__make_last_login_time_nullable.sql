-- V4: Make last_login_time nullable for new users

ALTER TABLE `user_info` ALTER COLUMN `last_login_time` datetime DEFAULT NULL;
