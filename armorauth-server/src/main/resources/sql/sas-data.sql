INSERT INTO `oauth2_client` VALUES ('2c9c20818099c695018099cbca030000', 'f62ac251-36d7-42c8-9f75-c31c90111bd4', '{bcrypt}$2a$10$uHWdt9Ackncw6s5BJlYO9OOdpD3Q44aan0SjttGRCZU2qvvk3fAZO', 'autism', 'client_secret_basic,client_secret_post', 'authorization_code,client_credentials,refresh_token', 'http://armorauth-demo:8083/login/oauth2/code/autism', '2022-05-06 22:35:11.000000', NULL);
INSERT INTO `oauth2_client` VALUES ('8a79987882ed8bc10182edb71d5b1007', '8a349006-b8e3-427b-8814-bc4b32e8930a', '0c1501f4a8a35db0a725d1f547f5466f', 'silent', 'client_secret_jwt', 'authorization_code,client_credentials,refresh_token', 'http://armorauth-demo:8084/login/oauth2/code/silent', '2022-05-06 22:35:11.000000', NULL);
INSERT INTO `oauth2_client` VALUES ('3e82dfde853649a0af86c10b744a88d3', 'b3d64549-0c6b-4306-9170-886dd8652704', '', 'quietly', 'private_key_jwt', 'authorization_code,client_credentials,refresh_token', 'http://armorauth-demo:8084/login/oauth2/code/quietly', '2022-05-06 22:35:11.000000', NULL);
INSERT INTO `oauth2_client` VALUES ('22aaa0568121584aa983df7a129fe7fd', '4569bca1-bca7-49eb-a03c-7898e9197d5f', '', 'clever', 'none', 'authorization_code,refresh_token', 'http://armorauth-demo:8085/login/oauth2/code/clever', '2022-05-06 22:35:11.000000', NULL);


INSERT INTO `oauth2_scope` VALUES ('f62ac251-36d7-42c8-9f75-c31c90111bd4', 'message.read', '读取信息');
INSERT INTO `oauth2_scope` VALUES ('f62ac251-36d7-42c8-9f75-c31c90111bd4', 'message.write', '写入信息');
INSERT INTO `oauth2_scope` VALUES ('f62ac251-36d7-42c8-9f75-c31c90111bd4', 'userinfo', '用户信息');

INSERT INTO `oauth2_scope` VALUES ('8a349006-b8e3-427b-8814-bc4b32e8930a', 'message.read', '读取信息');
INSERT INTO `oauth2_scope` VALUES ('8a349006-b8e3-427b-8814-bc4b32e8930a', 'message.write', '写入信息');
INSERT INTO `oauth2_scope` VALUES ('8a349006-b8e3-427b-8814-bc4b32e8930a', 'userinfo', '用户信息');

INSERT INTO `oauth2_scope` VALUES ('b3d64549-0c6b-4306-9170-886dd8652704', 'message.read', '读取信息');
INSERT INTO `oauth2_scope` VALUES ('b3d64549-0c6b-4306-9170-886dd8652704', 'message.write', '写入信息');
INSERT INTO `oauth2_scope` VALUES ('b3d64549-0c6b-4306-9170-886dd8652704', 'userinfo', '用户信息');


INSERT INTO `oauth2_scope` VALUES ('4569bca1-bca7-49eb-a03c-7898e9197d5f', 'message.read', '读取信息');
INSERT INTO `oauth2_scope` VALUES ('4569bca1-bca7-49eb-a03c-7898e9197d5f', 'message.write', '写入信息');
INSERT INTO `oauth2_scope` VALUES ('4569bca1-bca7-49eb-a03c-7898e9197d5f', 'userinfo', '用户信息');


INSERT INTO `oauth2_client_settings` VALUES ('f62ac251-36d7-42c8-9f75-c31c90111bd4', '', b'1', b'0', '');
INSERT INTO `oauth2_client_settings` VALUES ('8a349006-b8e3-427b-8814-bc4b32e8930a', '', b'1', b'0', 'HS256');
INSERT INTO `oauth2_client_settings` VALUES ('b3d64549-0c6b-4306-9170-886dd8652704', 'http://armorauth-demo:8084/jwks', b'1', b'0', 'RS256');
INSERT INTO `oauth2_client_settings` VALUES ('4569bca1-bca7-49eb-a03c-7898e9197d5f', '', b'1', b'1', '');

INSERT INTO `oauth2_token_settings` VALUES ('f62ac251-36d7-42c8-9f75-c31c90111bd4', 300000000000, 'RS256', 3600000000000, b'1', 'self-contained');
INSERT INTO `oauth2_token_settings` VALUES ('8a349006-b8e3-427b-8814-bc4b32e8930a', 300000000000, 'RS256', 3600000000000, b'1', 'self-contained');
INSERT INTO `oauth2_token_settings` VALUES ('b3d64549-0c6b-4306-9170-886dd8652704', 300000000000, 'RS256', 3600000000000, b'1', 'self-contained');
INSERT INTO `oauth2_token_settings` VALUES ('4569bca1-bca7-49eb-a03c-7898e9197d5f', 300000000000, 'RS256', 3600000000000, b'1', 'self-contained');


INSERT INTO `user_info` VALUES ('0d7c83d900a441c988926af0289de0b2', 'admin', '{bcrypt}$2a$10$9XQTKexOScPtqKGHlKYCrO9IVTXrwE2uxl.aRoVhHrNDoyVVjXHDm', '13103777777', '付林', '0000-00-00 00:00:00', '0000-00-00 00:00:00', 0);
