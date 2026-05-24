-- Mock showcase data for local demos and sample applications.

MERGE INTO `user_info`
    (`id`, `username`, `password`, `phone`, `display_name`, `create_time`, `last_login_time`, `status`,
     `email`, `avatar`, `email_verified`, `phone_verified`, `locked_until`, `update_time`, `profile`)
KEY(`id`) VALUES
    ('0d7c83d900a441c988926af0289de0b2', 'admin',
     '{bcrypt}$2a$10$tzEgeAwC8jD8Lxvz.IYiCeM4JOhXvZ2GD0UeBWWbVf3.hpL./my02',
     '13103777777', 'Admin User', TIMESTAMP '2022-05-06 22:35:11', CURRENT_TIMESTAMP, 0,
     'admin@mock.armorauth.local', '', TRUE, TRUE, NULL, CURRENT_TIMESTAMP,
     '{"title":"Platform owner","department":"Identity"}'),
    ('mock-user-manager', 'app.manager',
     '{bcrypt}$2a$10$tzEgeAwC8jD8Lxvz.IYiCeM4JOhXvZ2GD0UeBWWbVf3.hpL./my02',
     '13900000001', 'App Manager', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0,
     'manager@mock.armorauth.local', '', TRUE, TRUE, NULL, CURRENT_TIMESTAMP,
     '{"title":"Application manager","department":"Product"}'),
    ('mock-user-auditor', 'audit.viewer',
     '{bcrypt}$2a$10$tzEgeAwC8jD8Lxvz.IYiCeM4JOhXvZ2GD0UeBWWbVf3.hpL./my02',
     '13900000002', 'Audit Viewer', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0,
     'auditor@mock.armorauth.local', '', TRUE, TRUE, NULL, CURRENT_TIMESTAMP,
     '{"title":"Security analyst","department":"Security"}'),
    ('mock-user-demo', 'demo.user',
     '{bcrypt}$2a$10$tzEgeAwC8jD8Lxvz.IYiCeM4JOhXvZ2GD0UeBWWbVf3.hpL./my02',
     '13900000003', 'Demo User', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0,
     'demo@mock.armorauth.local', '', TRUE, TRUE, NULL, CURRENT_TIMESTAMP,
     '{"title":"Workspace member","department":"Engineering"}');

MERGE INTO `user_role` (`id`, `user_id`, `role_id`) KEY(`id`) VALUES
    ('mock-ur-admin-super', '0d7c83d900a441c988926af0289de0b2', '1'),
    ('mock-ur-manager-app', 'mock-user-manager', '3'),
    ('mock-ur-manager-user', 'mock-user-manager', '6'),
    ('mock-ur-auditor-view', 'mock-user-auditor', '5'),
    ('mock-ur-demo-user', 'mock-user-demo', '6');

MERGE INTO `tenant`
    (`id`, `tenant_code`, `tenant_name`, `description`, `logo`, `primary_color`, `custom_domain`,
     `login_page_title`, `privacy_policy_url`, `terms_of_service_url`, `enabled`, `created_at`, `updated_at`)
KEY(`id`) VALUES
    ('tenant-default', 'default', 'Default Workspace', 'Mock tenant for ArmorAuth local demos.', '',
     '#155EEF', 'localhost', 'ArmorAuth Mock Login', 'https://mock.armorauth.local/privacy',
     'https://mock.armorauth.local/terms', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('tenant-acme', 'acme', 'Acme Cloud', 'Sample customer tenant with seeded users and apps.', '',
     '#0F766E', 'acme.mock.armorauth.local', 'Acme Identity Portal', 'https://acme.example/privacy',
     'https://acme.example/terms', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO `organization`
    (`id`, `tenant_id`, `org_code`, `org_name`, `description`, `logo`, `parent_id`, `enabled`, `created_at`, `updated_at`)
KEY(`id`) VALUES
    ('org-acme-root', 'tenant-acme', 'acme-root', 'Acme HQ', 'Root organization for the Acme tenant.', '',
     NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('org-acme-product', 'tenant-acme', 'product', 'Product Team', 'Owns application onboarding and consent.', '',
     'org-acme-root', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('org-acme-security', 'tenant-acme', 'security', 'Security Team', 'Reviews audit events and MFA rollout.', '',
     'org-acme-root', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO `organization_member` (`id`, `org_id`, `user_id`, `org_role`, `created_at`) KEY(`id`) VALUES
    ('mock-om-admin-security', 'org-acme-security', '0d7c83d900a441c988926af0289de0b2', 'OWNER', CURRENT_TIMESTAMP),
    ('mock-om-manager-product', 'org-acme-product', 'mock-user-manager', 'MANAGER', CURRENT_TIMESTAMP),
    ('mock-om-auditor-security', 'org-acme-security', 'mock-user-auditor', 'AUDITOR', CURRENT_TIMESTAMP),
    ('mock-om-demo-product', 'org-acme-product', 'mock-user-demo', 'MEMBER', CURRENT_TIMESTAMP);

MERGE INTO `oauth2_client`
    (`id`, `client_id`, `client_secret`, `client_name`, `client_authentication_methods`,
     `authorization_grant_types`, `redirect_uris`, `post_logout_redirect_uris`,
     `client_id_issued_at`, `client_secret_expires_at`, `enabled`, `mfa_required`)
KEY(`id`) VALUES
    ('react-spa-pkce-001', 'react-spa-pkce', '', 'React SPA PKCE Sample', 'none',
     'authorization_code,refresh_token', 'http://localhost:3000/callback', 'http://localhost:3000/',
     CURRENT_TIMESTAMP, NULL, TRUE, FALSE),
    ('mock-dashboard-client', 'mock-dashboard',
     '{bcrypt}$2a$10$uHWdt9Ackncw6s5BJlYO9OOdpD3Q44aan0SjttGRCZU2qvvk3fAZO',
     'Mock Dashboard Confidential Client', 'client_secret_basic,client_secret_post',
     'authorization_code,client_credentials,refresh_token',
     'http://localhost:5173/callback,http://localhost:8080/login/oauth2/code/mock-dashboard',
     'http://localhost:5173/,http://localhost:8080/', CURRENT_TIMESTAMP, NULL, TRUE, TRUE);

MERGE INTO `oauth2_client_settings`
    (`client_id`, `jwk_set_url`, `require_authorization_consent`, `require_proof_key`, `signing_algorithm`)
KEY(`client_id`) VALUES
    ('react-spa-pkce', '', FALSE, TRUE, ''),
    ('mock-dashboard', '', TRUE, FALSE, '');

MERGE INTO `oauth2_token_settings`
    (`client_id`, `access_token_time_to_live`, `refresh_token_time_to_live`, `device_code_time_to_live`,
     `authorization_code_time_to_live`, `id_token_signature_algorithm`, `reuse_refresh_tokens`, `token_format`)
KEY(`client_id`) VALUES
    ('react-spa-pkce', 300000000000, 3600000000000, 300000000000, 300000000000, 'RS256', FALSE, 'self-contained'),
    ('mock-dashboard', 600000000000, 7200000000000, 300000000000, 300000000000, 'RS256', TRUE, 'self-contained');

MERGE INTO `oauth2_scope` (`client_id`, `scope`, `description`) KEY(`client_id`, `scope`) VALUES
    ('react-spa-pkce', 'openid', 'OpenID Connect'),
    ('react-spa-pkce', 'profile', 'Basic profile'),
    ('react-spa-pkce', 'email', 'Email address'),
    ('react-spa-pkce', 'message.read', 'Read demo messages'),
    ('mock-dashboard', 'openid', 'OpenID Connect'),
    ('mock-dashboard', 'profile', 'Basic profile'),
    ('mock-dashboard', 'email', 'Email address'),
    ('mock-dashboard', 'message.read', 'Read demo messages'),
    ('mock-dashboard', 'message.write', 'Write demo messages'),
    ('mock-dashboard', 'admin.audit', 'Read audit events');

MERGE INTO `identity_provider`
    (`id`, `provider_name`, `provider_type`, `registration_id`, `client_id`, `client_secret`,
     `authorization_uri`, `token_uri`, `userinfo_uri`, `jwk_set_uri`, `scopes`, `attribute_mapping`,
     `linking_strategy`, `display_order`, `enabled`, `created_at`, `updated_at`)
KEY(`id`) VALUES
    ('mock-idp-oidc', 'Mock OIDC Provider', 'OIDC', 'mock-oidc', 'mock-oidc-client', 'mock-oidc-secret',
     'https://mock-idp.local/oauth2/authorize', 'https://mock-idp.local/oauth2/token',
     'https://mock-idp.local/userinfo', 'https://mock-idp.local/jwks', 'openid,profile,email',
     '{"sub":"sub","email":"email","name":"name"}', 'CONFIRM', 10, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('mock-idp-gitee', 'Mock Gitee Social Login', 'GITEE', 'mock-gitee', 'mock-gitee-client', 'mock-gitee-secret',
     'https://gitee.com/oauth/authorize', 'https://gitee.com/oauth/token',
     'https://gitee.com/api/v5/user', '', 'user_info',
     '{"id":"sub","email":"email","name":"name"}', 'EMAIL_MATCH', 20, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('mock-idp-saml', 'Mock SAML Enterprise IdP', 'SAML', 'mock-saml', NULL, NULL,
     NULL, NULL, NULL, NULL, '',
     '{"nameId":"username","email":"email"}', 'CONFIRM', 30, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('mock-idp-ldap', 'Mock LDAP Directory', 'LDAP', 'mock-ldap', NULL, NULL,
     NULL, NULL, NULL, NULL, '',
     '{"uid":"username","mail":"email","cn":"displayName"}', 'NONE', 40, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO `webhook_endpoint`
    (`id`, `name`, `url`, `secret`, `event_types`, `enabled`, `created_at`, `updated_at`)
KEY(`id`) VALUES
    ('mock-webhook-audit', 'Mock Audit Webhook', 'https://webhook.site/armorauth-mock',
     'mock-webhook-secret', 'LOGIN_SUCCESS,LOGIN_FAILURE,APPLICATION_CREATED', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO `webhook_delivery`
    (`id`, `endpoint_id`, `event_type`, `payload`, `response_status`, `response_body`, `success`, `retry_count`, `created_at`)
KEY(`id`) VALUES
    ('mock-delivery-login', 'mock-webhook-audit', 'LOGIN_SUCCESS',
     '{"principal":"admin","clientId":"react-spa-pkce"}', 200, 'ok', TRUE, 0, CURRENT_TIMESTAMP),
    ('mock-delivery-failed', 'mock-webhook-audit', 'LOGIN_FAILURE',
     '{"principal":"demo.user","reason":"bad_credentials"}', 500, 'mock upstream error', FALSE, 1, CURRENT_TIMESTAMP);

MERGE INTO `audit_event`
    (`id`, `event_type`, `principal_name`, `resource_type`, `resource_id`, `detail`, `ip_address`, `user_agent`, `created_at`)
KEY(`id`) VALUES
    ('mock-audit-login-admin', 'LOGIN_SUCCESS', 'admin', 'session', 'mock-session-001',
     'Mock admin login from hosted login page.', '127.0.0.1', 'ArmorAuth Mock', CURRENT_TIMESTAMP),
    ('mock-audit-client-created', 'APPLICATION_CREATED', 'app.manager', 'oauth2_client', 'mock-dashboard',
     'Mock dashboard client created for local sample flows.', '127.0.0.1', 'ArmorAuth Mock', CURRENT_TIMESTAMP),
    ('mock-audit-idp-check', 'IDENTITY_PROVIDER_CHECKED', 'audit.viewer', 'identity_provider', 'mock-oidc',
     'Mock provider readiness check completed.', '127.0.0.1', 'ArmorAuth Mock', CURRENT_TIMESTAMP);

MERGE INTO `token_statistics`
    (`id`, `client_id`, `grant_type`, `token_type`, `count`, `last_issued_at`, `date`)
KEY(`id`) VALUES
    ('mock-token-react-code-access', 'react-spa-pkce', 'authorization_code', 'access_token', 42,
     CURRENT_TIMESTAMP, CURRENT_DATE),
    ('mock-token-dashboard-client-access', 'mock-dashboard', 'client_credentials', 'access_token', 18,
     CURRENT_TIMESTAMP, CURRENT_DATE);
