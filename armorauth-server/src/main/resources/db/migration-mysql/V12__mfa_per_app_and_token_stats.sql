-- V12: Add MFA per-app requirement and token statistics

ALTER TABLE `oauth2_client` ADD COLUMN `mfa_required` tinyint(1) DEFAULT 0;

CREATE TABLE IF NOT EXISTS `token_statistics` (
    `id` varchar(255) NOT NULL,
    `client_id` varchar(255) NOT NULL,
    `grant_type` varchar(100) NOT NULL,
    `token_type` varchar(50) NOT NULL,
    `count` bigint NOT NULL DEFAULT 0,
    `last_issued_at` datetime(6) DEFAULT NULL,
    `date` date NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_token_stats` (`client_id`, `grant_type`, `token_type`, `date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
