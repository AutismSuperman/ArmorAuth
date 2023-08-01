INSERT INTO `oauth2_client` VALUES ('2c9c20818099c695018099cbca030000', 'e2fa7e64-249b-46f0-ae1d-797610e88615', '{bcrypt}$2a$10$uHWdt9Ackncw6s5BJlYO9OOdpD3Q44aan0SjttGRCZU2qvvk3fAZO', 'autism', 'client_secret_basic,client_secret_post', 'authorization_code,client_credentials,refresh_token', NULL, '2022-05-06 22:35:11.000000', NULL);

INSERT INTO `oauth2_client_settings` VALUES ('e2fa7e64-249b-46f0-ae1d-797610e88615', '', b'1', b'0', '');
INSERT INTO `oauth2_client_settings` VALUES ('e4da4a32-592b-46f0-ae1d-784310e88423', '', b'1', b'0', '');



INSERT INTO `oauth2_scope` VALUES ('e2fa7e64-249b-46f0-ae1d-797610e88615', 'message.read', '读取信息');
INSERT INTO `oauth2_scope` VALUES ('e2fa7e64-249b-46f0-ae1d-797610e88615', 'message.write', '写入信息');
INSERT INTO `oauth2_scope` VALUES ('e2fa7e64-249b-46f0-ae1d-797610e88615', 'userinfo', '用户信息');

INSERT INTO `oauth2_token_settings` VALUES ('e2fa7e64-249b-46f0-ae1d-797610e88615', 300000000000, 'RS256', 3600000000000, b'1', 'self-contained');


INSERT INTO `user_info` VALUES ('0d7c83d900a441c988926af0289de0b2', 'fulin', '{bcrypt}$2a$10$r0uAg8jMsz0V8Z1/FKf/.eWSXIcJwavLURkHrofVZJDARyWjAAhgG', '13103777777', '付林', '0000-00-00 00:00:00', '0000-00-00 00:00:00', 0);
