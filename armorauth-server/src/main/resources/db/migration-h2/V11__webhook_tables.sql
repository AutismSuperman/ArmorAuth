-- V11: Add webhook tables

CREATE TABLE IF NOT EXISTS `webhook_endpoint` (
    `id` varchar(255) NOT NULL,
    `name` varchar(200) NOT NULL,
    `url` varchar(1000) NOT NULL,
    `secret` varchar(500) DEFAULT NULL,
    `event_types` varchar(1000) DEFAULT NULL,
    `enabled` boolean NOT NULL DEFAULT true,
    `created_at` datetime(6) NOT NULL,
    `updated_at` datetime(6) DEFAULT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS `webhook_delivery` (
    `id` varchar(255) NOT NULL,
    `endpoint_id` varchar(255) NOT NULL,
    `event_type` varchar(100) NOT NULL,
    `payload` clob DEFAULT NULL,
    `response_status` int DEFAULT NULL,
    `response_body` clob DEFAULT NULL,
    `success` boolean DEFAULT NULL,
    `retry_count` int DEFAULT 0,
    `created_at` datetime(6) NOT NULL,
    PRIMARY KEY (`id`)
);

CREATE INDEX IF NOT EXISTS idx_webhook_delivery_endpoint ON `webhook_delivery`(`endpoint_id`);
CREATE INDEX IF NOT EXISTS idx_webhook_delivery_created ON `webhook_delivery`(`created_at`);
