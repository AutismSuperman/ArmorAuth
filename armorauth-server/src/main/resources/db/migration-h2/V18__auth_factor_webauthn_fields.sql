-- V18: Add WebAuthn/Passkey metadata fields to auth_factor

ALTER TABLE `auth_factor` ADD COLUMN IF NOT EXISTS `webauthn_challenge` varchar(500) DEFAULT NULL;
ALTER TABLE `auth_factor` ADD COLUMN IF NOT EXISTS `credential_id` varchar(500) DEFAULT NULL;
ALTER TABLE `auth_factor` ADD COLUMN IF NOT EXISTS `credential_public_key` clob DEFAULT NULL;
ALTER TABLE `auth_factor` ADD COLUMN IF NOT EXISTS `sign_count` bigint DEFAULT NULL;
ALTER TABLE `auth_factor` ADD COLUMN IF NOT EXISTS `transports` varchar(200) DEFAULT NULL;
ALTER TABLE `auth_factor` ADD COLUMN IF NOT EXISTS `aaguid` varchar(100) DEFAULT NULL;
ALTER TABLE `auth_factor` ADD COLUMN IF NOT EXISTS `webauthn_user_handle` varchar(500) DEFAULT NULL;
ALTER TABLE `auth_factor` ADD COLUMN IF NOT EXISTS `backup_eligible` boolean DEFAULT NULL;
ALTER TABLE `auth_factor` ADD COLUMN IF NOT EXISTS `backup_state` boolean DEFAULT NULL;

CREATE INDEX IF NOT EXISTS idx_auth_factor_credential_id ON `auth_factor` (`credential_id`);
