ALTER TABLE `oauth2_client_settings`
    ADD COLUMN `x509_certificate_subject_dn` varchar(1000) DEFAULT NULL;

ALTER TABLE `oauth2_token_settings`
    ADD COLUMN `x509_certificate_bound_access_tokens` boolean NOT NULL DEFAULT false;

ALTER TABLE `oauth2_client`
    ADD COLUMN `tenant_id` varchar(255) NOT NULL DEFAULT 'tenant-default';

ALTER TABLE `oauth2_client`
    ADD COLUMN `registration_source` varchar(50) NOT NULL DEFAULT 'ADMIN';

CREATE INDEX IF NOT EXISTS `idx_oauth2_client_tenant_id` ON `oauth2_client` (`tenant_id`);

ALTER TABLE `authorization`
    ADD COLUMN `tenant_id` varchar(255) NOT NULL DEFAULT 'tenant-default';

CREATE INDEX IF NOT EXISTS `idx_authorization_tenant_id` ON `authorization` (`tenant_id`);

ALTER TABLE `authorization_consent`
    ADD COLUMN `tenant_id` varchar(255) NOT NULL DEFAULT 'tenant-default';

CREATE INDEX IF NOT EXISTS `idx_authorization_consent_tenant_id` ON `authorization_consent` (`tenant_id`);

ALTER TABLE `jwk_key`
    ADD COLUMN `tenant_id` varchar(255) NOT NULL DEFAULT 'tenant-default';

CREATE INDEX IF NOT EXISTS `idx_jwk_key_tenant_id` ON `jwk_key` (`tenant_id`);

ALTER TABLE `oauth2_client_settings`
    ADD COLUMN `dpop_enabled` boolean NOT NULL DEFAULT false;

ALTER TABLE `oauth2_client_settings`
    ADD COLUMN `dpop_required` boolean NOT NULL DEFAULT false;

ALTER TABLE `oauth2_client_settings`
    ADD COLUMN `dpop_allowed_algorithms` varchar(500) DEFAULT NULL;
