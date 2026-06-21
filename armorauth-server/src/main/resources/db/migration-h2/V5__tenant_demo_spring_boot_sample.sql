MERGE INTO `tenant`
    (`id`, `tenant_code`, `tenant_name`, `description`, `logo`, `primary_color`, `custom_domain`,
     `login_page_title`, `privacy_policy_url`, `terms_of_service_url`, `enabled`, `created_at`, `updated_at`)
KEY(`id`) VALUES
    ('tenant-demo', 'demo', 'Demo Tenant', 'Tenant issuer demo for SAS path-based testing.', '',
     '#2563EB', 'demo.localhost', 'Demo Tenant Login', '', '', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO `oauth2_client`
    (`id`, `tenant_id`, `registration_source`, `client_id`, `client_secret`, `client_name`,
     `client_authentication_methods`, `authorization_grant_types`, `redirect_uris`,
     `post_logout_redirect_uris`, `client_id_issued_at`, `client_secret_expires_at`,
     `enabled`, `mfa_required`, `role_mfa_required`)
KEY(`id`) VALUES
    ('tenant-demo-spring', 'tenant-demo', 'ADMIN', 'tenant-demo-spring',
     '{bcrypt}$2a$10$uHWdt9Ackncw6s5BJlYO9OOdpD3Q44aan0SjttGRCZU2qvvk3fAZO',
     'Tenant Demo Spring Boot OIDC Sample', 'client_secret_basic',
     'authorization_code,refresh_token', 'http://localhost:8083/login/oauth2/code/tenant-demo',
     'http://localhost:8083/', CURRENT_TIMESTAMP, NULL, TRUE, FALSE, '');

MERGE INTO `oauth2_scope` (`client_id`, `scope`, `description`) KEY(`client_id`, `scope`) VALUES
    ('tenant-demo-spring', 'openid', 'OpenID Connect'),
    ('tenant-demo-spring', 'profile', 'Basic profile'),
    ('tenant-demo-spring', 'email', 'Email address'),
    ('tenant-demo-spring', 'message.read', 'Read tenant demo messages');

MERGE INTO `oauth2_client_settings`
    (`client_id`, `jwk_set_url`, `require_authorization_consent`, `require_proof_key`,
     `signing_algorithm`, `x509_certificate_subject_dn`, `dpop_enabled`, `dpop_required`,
     `dpop_allowed_algorithms`)
KEY(`client_id`) VALUES
    ('tenant-demo-spring', '', FALSE, FALSE, '', NULL, FALSE, FALSE, NULL);

MERGE INTO `oauth2_token_settings`
    (`client_id`, `access_token_time_to_live`, `refresh_token_time_to_live`, `device_code_time_to_live`,
     `authorization_code_time_to_live`, `id_token_signature_algorithm`, `reuse_refresh_tokens`,
     `token_format`, `x509_certificate_bound_access_tokens`)
KEY(`client_id`) VALUES
    ('tenant-demo-spring', 300000000000, 3600000000000, 300000000000, 300000000000,
     'RS256', TRUE, 'self-contained', FALSE);
