CREATE TABLE IF NOT EXISTS `identity_provider_display_preference` (
    `registration_id` varchar(100) NOT NULL,
    `display_on_login` tinyint(1) NOT NULL DEFAULT 1,
    `updated_at` timestamp NOT NULL,
    PRIMARY KEY (`registration_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
