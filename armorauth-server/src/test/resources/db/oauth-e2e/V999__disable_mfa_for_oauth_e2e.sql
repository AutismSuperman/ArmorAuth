-- OAuth/OIDC protocol E2E focuses on authorization server flows.
-- MFA behavior has separate coverage and would otherwise stop these flows on /login/mfa.
UPDATE `oauth2_client`
SET `mfa_required` = FALSE,
    `role_mfa_required` = ''
WHERE `client_id` IN (
    'f62ac251-36d7-42c8-9f75-c31c90111bd4',
    '8ee3a98e-89a8-438d-a314-1ef9df815279',
    'spring-pkce'
);

INSERT INTO `oauth2_client` (`id`, `client_id`, `client_secret`, `client_name`,
    `client_authentication_methods`, `authorization_grant_types`, `redirect_uris`,
    `post_logout_redirect_uris`, `client_id_issued_at`, `client_secret_expires_at`,
    `enabled`, `mfa_required`, `role_mfa_required`)
VALUES ('dcr-registrar-client', 'dcr-registrar',
    '{bcrypt}$2a$10$uHWdt9Ackncw6s5BJlYO9OOdpD3Q44aan0SjttGRCZU2qvvk3fAZO', 'DCR Registrar',
    'client_secret_basic', 'client_credentials', '', '',
    '2022-05-06 22:35:11.000000', NULL, TRUE, FALSE, '');

INSERT INTO `oauth2_scope` (`client_id`, `scope`, `description`)
VALUES ('dcr-registrar', 'client.create', 'Create dynamic clients');
INSERT INTO `oauth2_scope` (`client_id`, `scope`, `description`)
VALUES ('dcr-registrar', 'client.read', 'Read dynamic clients');

INSERT INTO `oauth2_client_settings` (`client_id`, `jwk_set_url`, `require_authorization_consent`,
    `require_proof_key`, `signing_algorithm`, `x509_certificate_subject_dn`)
VALUES ('dcr-registrar', '', FALSE, FALSE, '', NULL);

INSERT INTO `oauth2_token_settings` (`client_id`, `access_token_time_to_live`,
    `refresh_token_time_to_live`, `device_code_time_to_live`, `authorization_code_time_to_live`,
    `id_token_signature_algorithm`, `reuse_refresh_tokens`, `token_format`,
    `x509_certificate_bound_access_tokens`)
VALUES ('dcr-registrar', 300000000000, 3600000000000, 300000000000, 300000000000,
    'RS256', TRUE, 'self-contained', FALSE);

INSERT INTO `tenant` (`id`, `tenant_code`, `tenant_name`, `description`, `logo`, `primary_color`,
    `custom_domain`, `login_page_title`, `privacy_policy_url`, `terms_of_service_url`,
    `enabled`, `created_at`, `updated_at`)
VALUES ('tenant-acme', 'acme', 'Acme Tenant', 'OAuth issuer E2E tenant', '', '#1677ff',
    '', 'Acme Login', '', '', TRUE, CURRENT_TIMESTAMP, NULL);

INSERT INTO `oauth2_client` (`id`, `tenant_id`, `client_id`, `client_secret`, `client_name`,
    `client_authentication_methods`, `authorization_grant_types`, `redirect_uris`,
    `post_logout_redirect_uris`, `client_id_issued_at`, `client_secret_expires_at`,
    `enabled`, `mfa_required`, `role_mfa_required`)
VALUES ('acme-client-internal-id', 'tenant-acme', 'acme-client',
    '{bcrypt}$2a$10$uHWdt9Ackncw6s5BJlYO9OOdpD3Q44aan0SjttGRCZU2qvvk3fAZO', 'Acme Client',
    'client_secret_basic', 'client_credentials', '', '',
    '2022-05-06 22:35:11.000000', NULL, TRUE, FALSE, '');

INSERT INTO `oauth2_scope` (`client_id`, `scope`, `description`)
VALUES ('acme-client', 'message.read', 'Read messages in Acme issuer');

INSERT INTO `oauth2_client_settings` (`client_id`, `jwk_set_url`, `require_authorization_consent`,
    `require_proof_key`, `signing_algorithm`, `x509_certificate_subject_dn`)
VALUES ('acme-client', '', FALSE, FALSE, '', NULL);

INSERT INTO `oauth2_token_settings` (`client_id`, `access_token_time_to_live`,
    `refresh_token_time_to_live`, `device_code_time_to_live`, `authorization_code_time_to_live`,
    `id_token_signature_algorithm`, `reuse_refresh_tokens`, `token_format`,
    `x509_certificate_bound_access_tokens`)
VALUES ('acme-client', 300000000000, 3600000000000, 300000000000, 300000000000,
    'RS256', TRUE, 'self-contained', FALSE);
