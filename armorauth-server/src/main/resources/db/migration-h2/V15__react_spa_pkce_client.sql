-- V15: Add React SPA PKCE client

-- React SPA PKCE Client (public client, no secret)
INSERT IGNORE INTO `oauth2_client` (`id`, `client_id`, `client_secret`, `client_name`,
    `client_authentication_methods`, `authorization_grant_types`,
    `redirect_uris`, `post_logout_redirect_uris`,
    `client_id_issued_at`, `client_secret_expires_at`)
VALUES ('react-spa-pkce-001', 'react-spa-pkce', '', 'React SPA PKCE Sample',
    'none', 'authorization_code,refresh_token',
    'http://localhost:3000/callback', 'http://localhost:3000/',
    CURRENT_TIMESTAMP, NULL);

INSERT IGNORE INTO `oauth2_client_settings` (`client_id`, `jwk_set_url`, `require_authorization_consent`, `require_proof_key`, `signing_algorithm`)
VALUES ('react-spa-pkce-001', '', 0, 1, '');

INSERT IGNORE INTO `oauth2_token_settings` (`client_id`, `access_token_time_to_live`, `refresh_token_time_to_live`,
    `device_code_time_to_live`, `authorization_code_time_to_live`,
    `id_token_signature_algorithm`, `reuse_refresh_tokens`, `token_format`)
VALUES ('react-spa-pkce-001', 300000000000, 3600000000000, 300000000000, 300000000000, 'RS256', 0, 'self-contained');

INSERT IGNORE INTO `oauth2_scope` (`client_id`, `scope`, `description`)
VALUES ('react-spa-pkce-001', 'openid', 'OpenID');
INSERT IGNORE INTO `oauth2_scope` (`client_id`, `scope`, `description`)
VALUES ('react-spa-pkce-001', 'profile', 'Profile');
INSERT IGNORE INTO `oauth2_scope` (`client_id`, `scope`, `description`)
VALUES ('react-spa-pkce-001', 'email', 'Email');
INSERT IGNORE INTO `oauth2_scope` (`client_id`, `scope`, `description`)
VALUES ('react-spa-pkce-001', 'message.read', 'Read Messages');
INSERT IGNORE INTO `oauth2_scope` (`client_id`, `scope`, `description`)
VALUES ('react-spa-pkce-001', 'message.write', 'Write Messages');
