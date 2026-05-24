-- V15 used the oauth2_client row id as the foreign key for the React SPA
-- settings/scopes. Runtime mappings join these tables by the public client_id.

INSERT INTO `oauth2_client_settings` (`client_id`, `jwk_set_url`, `require_authorization_consent`, `require_proof_key`, `signing_algorithm`)
SELECT 'react-spa-pkce', `jwk_set_url`, `require_authorization_consent`, `require_proof_key`, `signing_algorithm`
FROM `oauth2_client_settings` source
WHERE source.`client_id` = 'react-spa-pkce-001'
  AND NOT EXISTS (
      SELECT 1 FROM `oauth2_client_settings` target
      WHERE target.`client_id` = 'react-spa-pkce'
  );

DELETE FROM `oauth2_client_settings`
WHERE `client_id` = 'react-spa-pkce-001';

INSERT INTO `oauth2_token_settings` (`client_id`, `access_token_time_to_live`, `refresh_token_time_to_live`,
    `device_code_time_to_live`, `authorization_code_time_to_live`,
    `id_token_signature_algorithm`, `reuse_refresh_tokens`, `token_format`)
SELECT 'react-spa-pkce', `access_token_time_to_live`, `refresh_token_time_to_live`,
    `device_code_time_to_live`, `authorization_code_time_to_live`,
    `id_token_signature_algorithm`, `reuse_refresh_tokens`, `token_format`
FROM `oauth2_token_settings` source
WHERE source.`client_id` = 'react-spa-pkce-001'
  AND NOT EXISTS (
      SELECT 1 FROM `oauth2_token_settings` target
      WHERE target.`client_id` = 'react-spa-pkce'
  );

DELETE FROM `oauth2_token_settings`
WHERE `client_id` = 'react-spa-pkce-001';

INSERT INTO `oauth2_scope` (`client_id`, `scope`, `description`)
SELECT 'react-spa-pkce', source.`scope`, source.`description`
FROM `oauth2_scope` source
WHERE source.`client_id` = 'react-spa-pkce-001'
  AND NOT EXISTS (
      SELECT 1 FROM `oauth2_scope` target
      WHERE target.`client_id` = 'react-spa-pkce'
        AND target.`scope` = source.`scope`
  );

DELETE FROM `oauth2_scope`
WHERE `client_id` = 'react-spa-pkce-001';
