SET @schema_name = DATABASE();

SET @sql = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `oauth2_client_settings` ADD COLUMN `x509_certificate_subject_dn` varchar(1000) DEFAULT NULL',
        'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'oauth2_client_settings'
      AND COLUMN_NAME = 'x509_certificate_subject_dn'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `oauth2_token_settings` ADD COLUMN `x509_certificate_bound_access_tokens` tinyint(1) NOT NULL DEFAULT 0',
        'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'oauth2_token_settings'
      AND COLUMN_NAME = 'x509_certificate_bound_access_tokens'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `oauth2_client` ADD COLUMN `tenant_id` varchar(255) NOT NULL DEFAULT ''tenant-default''',
        'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'oauth2_client'
      AND COLUMN_NAME = 'tenant_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `oauth2_client` ADD COLUMN `registration_source` varchar(50) NOT NULL DEFAULT ''ADMIN''',
        'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'oauth2_client'
      AND COLUMN_NAME = 'registration_source'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
        'CREATE INDEX `idx_oauth2_client_tenant_id` ON `oauth2_client` (`tenant_id`)',
        'DO 0')
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'oauth2_client'
      AND INDEX_NAME = 'idx_oauth2_client_tenant_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `authorization` ADD COLUMN `tenant_id` varchar(255) NOT NULL DEFAULT ''tenant-default''',
        'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'authorization'
      AND COLUMN_NAME = 'tenant_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
        'CREATE INDEX `idx_authorization_tenant_id` ON `authorization` (`tenant_id`)',
        'DO 0')
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'authorization'
      AND INDEX_NAME = 'idx_authorization_tenant_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `authorization_consent` ADD COLUMN `tenant_id` varchar(255) NOT NULL DEFAULT ''tenant-default''',
        'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'authorization_consent'
      AND COLUMN_NAME = 'tenant_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
        'CREATE INDEX `idx_authorization_consent_tenant_id` ON `authorization_consent` (`tenant_id`)',
        'DO 0')
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'authorization_consent'
      AND INDEX_NAME = 'idx_authorization_consent_tenant_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `jwk_key` ADD COLUMN `tenant_id` varchar(255) NOT NULL DEFAULT ''tenant-default''',
        'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'jwk_key'
      AND COLUMN_NAME = 'tenant_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
        'CREATE INDEX `idx_jwk_key_tenant_id` ON `jwk_key` (`tenant_id`)',
        'DO 0')
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'jwk_key'
      AND INDEX_NAME = 'idx_jwk_key_tenant_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `oauth2_client_settings` ADD COLUMN `dpop_enabled` tinyint(1) NOT NULL DEFAULT 0',
        'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'oauth2_client_settings'
      AND COLUMN_NAME = 'dpop_enabled'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `oauth2_client_settings` ADD COLUMN `dpop_required` tinyint(1) NOT NULL DEFAULT 0',
        'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'oauth2_client_settings'
      AND COLUMN_NAME = 'dpop_required'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `oauth2_client_settings` ADD COLUMN `dpop_allowed_algorithms` varchar(500) DEFAULT NULL',
        'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'oauth2_client_settings'
      AND COLUMN_NAME = 'dpop_allowed_algorithms'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
