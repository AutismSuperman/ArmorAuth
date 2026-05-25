-- V21: configurable role MFA policy per OAuth2 client (MySQL)

ALTER TABLE `oauth2_client`
    ADD COLUMN `role_mfa_required` varchar(500) DEFAULT 'SUPER_ADMIN,TENANT_ADMIN' AFTER `mfa_required`;

UPDATE `oauth2_client`
SET `role_mfa_required` = 'SUPER_ADMIN,TENANT_ADMIN'
WHERE `role_mfa_required` IS NULL;
