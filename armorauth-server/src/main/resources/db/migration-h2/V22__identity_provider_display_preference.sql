-- V22: Store login-page display preference for configured identity providers

CREATE TABLE IF NOT EXISTS `identity_provider_display_preference` (
    `registration_id` varchar(100) NOT NULL,
    `display_on_login` boolean NOT NULL DEFAULT true,
    `updated_at` timestamp NOT NULL,
    PRIMARY KEY (`registration_id`)
);
