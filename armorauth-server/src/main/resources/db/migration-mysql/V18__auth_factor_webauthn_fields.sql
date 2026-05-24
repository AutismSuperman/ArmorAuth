-- V18: Add WebAuthn/Passkey metadata fields to auth_factor

ALTER TABLE `auth_factor`
    ADD COLUMN `webauthn_challenge` varchar(500) DEFAULT NULL,
    ADD COLUMN `credential_id` varchar(500) DEFAULT NULL,
    ADD COLUMN `credential_public_key` text DEFAULT NULL,
    ADD COLUMN `sign_count` bigint DEFAULT NULL,
    ADD COLUMN `transports` varchar(200) DEFAULT NULL,
    ADD COLUMN `aaguid` varchar(100) DEFAULT NULL,
    ADD COLUMN `webauthn_user_handle` varchar(500) DEFAULT NULL,
    ADD COLUMN `backup_eligible` tinyint(1) DEFAULT NULL,
    ADD COLUMN `backup_state` tinyint(1) DEFAULT NULL;

CREATE INDEX `idx_auth_factor_credential_id` ON `auth_factor` (`credential_id`);
