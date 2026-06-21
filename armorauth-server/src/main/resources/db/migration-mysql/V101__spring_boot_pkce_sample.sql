DELETE FROM `oauth2_scope`
WHERE `client_id` IN ('react-spa-pkce', 'spring-pkce', 'tenant-demo-spa', 'tenant-demo-pkce');

DELETE FROM `oauth2_client_settings`
WHERE `client_id` IN ('react-spa-pkce', 'spring-pkce', 'tenant-demo-spa', 'tenant-demo-pkce');

DELETE FROM `oauth2_token_settings`
WHERE `client_id` IN ('react-spa-pkce', 'spring-pkce', 'tenant-demo-spa', 'tenant-demo-pkce');

DELETE FROM `token_statistics`
WHERE `client_id` IN ('react-spa-pkce', 'spring-pkce', 'tenant-demo-spa', 'tenant-demo-pkce');

DELETE FROM `oauth2_client`
WHERE `client_id` IN ('react-spa-pkce', 'spring-pkce', 'tenant-demo-spa', 'tenant-demo-pkce')
   OR `id` IN ('react-spa-pkce-001', 'spring-pkce-sample', 'tenant-demo-spa', 'tenant-demo-pkce');

INSERT IGNORE INTO `oauth2_client`
    (`id`, `tenant_id`, `registration_source`, `client_id`, `client_secret`, `client_name`,
     `client_authentication_methods`, `authorization_grant_types`, `redirect_uris`,
     `post_logout_redirect_uris`, `client_id_issued_at`, `client_secret_expires_at`,
     `enabled`, `mfa_required`, `role_mfa_required`)
VALUES
    ('spring-pkce-sample', 'tenant-default', 'ADMIN', 'spring-pkce', '',
     'Spring Boot PKCE Sample', 'none', 'authorization_code,refresh_token',
     'http://localhost:8086/login/oauth2/code/spring-pkce', 'http://localhost:8086/',
     CURRENT_TIMESTAMP(6), NULL, 1, 0, ''),
    ('tenant-demo-pkce', 'tenant-demo', 'ADMIN', 'tenant-demo-pkce', '',
     'Tenant Demo Spring Boot PKCE Sample', 'none', 'authorization_code,refresh_token',
     'http://localhost:8086/login/oauth2/code/tenant-demo-pkce', 'http://localhost:8086/',
     CURRENT_TIMESTAMP(6), NULL, 1, 0, '');

INSERT IGNORE INTO `oauth2_scope` (`client_id`, `scope`, `description`) VALUES
    ('spring-pkce', 'openid', 'OpenID Connect'),
    ('spring-pkce', 'profile', 'Basic profile'),
    ('spring-pkce', 'email', 'Email address'),
    ('spring-pkce', 'message.read', 'Read demo messages'),
    ('tenant-demo-pkce', 'openid', 'OpenID Connect'),
    ('tenant-demo-pkce', 'profile', 'Basic profile'),
    ('tenant-demo-pkce', 'email', 'Email address'),
    ('tenant-demo-pkce', 'message.read', 'Read tenant demo messages');

INSERT IGNORE INTO `oauth2_client_settings`
    (`client_id`, `jwk_set_url`, `require_authorization_consent`, `require_proof_key`,
     `signing_algorithm`, `x509_certificate_subject_dn`, `dpop_enabled`, `dpop_required`,
     `dpop_allowed_algorithms`)
VALUES
    ('spring-pkce', '', 0, 1, '', NULL, 0, 0, NULL),
    ('tenant-demo-pkce', '', 0, 1, '', NULL, 0, 0, NULL);

INSERT IGNORE INTO `oauth2_token_settings`
    (`client_id`, `access_token_time_to_live`, `refresh_token_time_to_live`, `device_code_time_to_live`,
     `authorization_code_time_to_live`, `id_token_signature_algorithm`, `reuse_refresh_tokens`,
     `token_format`, `x509_certificate_bound_access_tokens`)
VALUES
    ('spring-pkce', 300000000000, 3600000000000, 300000000000, 300000000000,
     'RS256', 0, 'self-contained', 0),
    ('tenant-demo-pkce', 300000000000, 3600000000000, 300000000000, 300000000000,
     'RS256', 0, 'self-contained', 0);
