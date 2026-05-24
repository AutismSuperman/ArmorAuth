-- V4: Make last_login_time nullable for new users

ALTER TABLE `user_info` MODIFY COLUMN `last_login_time` datetime DEFAULT NULL;
