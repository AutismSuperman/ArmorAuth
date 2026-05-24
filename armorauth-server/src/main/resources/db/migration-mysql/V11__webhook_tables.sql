-- V11: Add webhook tables

CREATE TABLE IF NOT EXISTS `webhook_endpoint` (
    `id` varchar(255) NOT NULL,
    `name` varchar(200) NOT NULL,
    `url` varchar(1000) NOT NULL,
    `secret` varchar(500) DEFAULT NULL,
    `event_types` varchar(1000) DEFAULT NULL,
    `enabled` tinyint(1) NOT NULL DEFAULT 1,
    `created_at` datetime(6) NOT NULL,
    `updated_at` datetime(6) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `webhook_delivery` (
    `id` varchar(255) NOT NULL,
    `endpoint_id` varchar(255) NOT NULL,
    `event_type` varchar(100) NOT NULL,
    `payload` text DEFAULT NULL,
    `response_status` int DEFAULT NULL,
    `response_body` text DEFAULT NULL,
    `success` tinyint(1) DEFAULT NULL,
    `retry_count` int DEFAULT 0,
    `created_at` datetime(6) NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_webhook_delivery_endpoint` (`endpoint_id`),
    KEY `idx_webhook_delivery_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
