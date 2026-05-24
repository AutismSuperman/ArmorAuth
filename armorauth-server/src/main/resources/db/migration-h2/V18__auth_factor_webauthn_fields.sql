-- V18: Add WebAuthn/Passkey metadata fields to auth_factor

ALTER TABLE `auth_factor`
    ADD COLUMN `webauthn_challenge` varchar(500) DEFAULT NULL,
    ADD COLUMN `credential_id` varchar(500) DEFAULT NULL,
    ADD COLUMN `credential_public_key` clob DEFAULT NULL,
    ADD COLUMN `sign_count` bigint DEFAULT NULL,
    ADD COLUMN `transports` varchar(200) DEFAULT NULL,
    ADD COLUMN `aaguid` varchar(100) DEFAULT NULL,
    ADD COLUMN `webauthn_user_handle` varchar(500) DEFAULT NULL,
    ADD COLUMN `backup_eligible` boolean DEFAULT NULL,
    ADD COLUMN `backup_state` boolean DEFAULT NULL;

CREATE INDEX IF NOT EXISTS idx_auth_factor_credential_id ON `auth_factor` (`credential_id`);
