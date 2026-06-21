DELETE FROM `oauth2_scope`
WHERE `client_id` IN ('react-spa-pkce', 'spring-pkce');

DELETE FROM `oauth2_client_settings`
WHERE `client_id` IN ('react-spa-pkce', 'spring-pkce');

DELETE FROM `oauth2_token_settings`
WHERE `client_id` IN ('react-spa-pkce', 'spring-pkce');

DELETE FROM `token_statistics`
WHERE `client_id` IN ('react-spa-pkce', 'spring-pkce')
   OR `id` IN ('mock-token-react-code-access', 'mock-token-spring-pkce-code-access');

DELETE FROM `oauth2_client`
WHERE `client_id` IN ('react-spa-pkce', 'spring-pkce')
   OR `id` IN ('react-spa-pkce-001', 'spring-pkce-sample');

MERGE INTO `oauth2_client`
    (`id`, `client_id`, `client_secret`, `client_name`, `client_authentication_methods`,
     `authorization_grant_types`, `redirect_uris`, `post_logout_redirect_uris`,
     `client_id_issued_at`, `client_secret_expires_at`, `enabled`, `mfa_required`)
KEY(`id`) VALUES
    ('spring-pkce-sample', 'spring-pkce', '', 'Spring Boot PKCE Sample', 'none',
     'authorization_code,refresh_token', 'http://localhost:8086/login/oauth2/code/spring-pkce',
     'http://localhost:8086/', CURRENT_TIMESTAMP, NULL, TRUE, FALSE);

MERGE INTO `oauth2_client_settings`
    (`client_id`, `jwk_set_url`, `require_authorization_consent`, `require_proof_key`, `signing_algorithm`)
KEY(`client_id`) VALUES
    ('spring-pkce', '', FALSE, TRUE, '');

MERGE INTO `oauth2_token_settings`
    (`client_id`, `access_token_time_to_live`, `refresh_token_time_to_live`, `device_code_time_to_live`,
     `authorization_code_time_to_live`, `id_token_signature_algorithm`, `reuse_refresh_tokens`, `token_format`)
KEY(`client_id`) VALUES
    ('spring-pkce', 300000000000, 3600000000000, 300000000000, 300000000000,
     'RS256', FALSE, 'self-contained');

MERGE INTO `oauth2_scope` (`client_id`, `scope`, `description`) KEY(`client_id`, `scope`) VALUES
    ('spring-pkce', 'openid', 'OpenID Connect'),
    ('spring-pkce', 'profile', 'Basic profile'),
    ('spring-pkce', 'email', 'Email address'),
    ('spring-pkce', 'message.read', 'Read demo messages');

UPDATE `webhook_delivery`
SET `payload` = '{"principal":"admin","clientId":"spring-pkce"}'
WHERE `id` = 'mock-delivery-login';

MERGE INTO `token_statistics`
    (`id`, `client_id`, `grant_type`, `token_type`, `count`, `last_issued_at`, `date`)
KEY(`id`) VALUES
    ('mock-token-spring-pkce-code-access', 'spring-pkce', 'authorization_code', 'access_token', 42,
     CURRENT_TIMESTAMP, CURRENT_DATE);
