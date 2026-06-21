# ArmorAuth Operation Manual

This manual describes common administration workflows for a running ArmorAuth installation. It focuses on operator routines after the system is available. For first-time product usage, start with [Basic Usage](basic-usage.md).

## Operator Roles

| Role | Responsibility |
| --- | --- |
| Platform administrator | Deployment, database, secrets, backups, and monitoring |
| Identity administrator | Users, organizations, identity providers, and login policies |
| Application administrator | OAuth/OIDC clients, scopes, redirect URIs, and application MFA settings |
| Security reviewer | Audit review, token activity, webhook activity, and policy verification |

## Initial System Review

After the first deployment:

1. Confirm the public issuer URL is stable and uses HTTPS.
2. Replace development administrator credentials.
3. Configure crypto keys and backup storage.
4. Verify the JWK table is populated and backed up.
5. Review default applications, scopes, roles, permissions, and login policies.
6. Confirm audit logging is enabled for administrative actions.

## Common Day-One Workflow

For a new environment, use this sequence:

1. Confirm or create the default tenant.
2. Create organizations only when they affect authorization, reporting, or administration.
3. Create the first OAuth2/OIDC application and verify redirect URIs.
4. Create test users, verify email and phone when those contact methods are used, and assign roles.
5. Test hosted login and token issuance with a sample or relying application.
6. Enable MFA for administrators and sensitive applications.
7. Review sessions, audit logs, and token statistics after testing.

Keep local development seed data separate from shared environments.

## Application Management

Applications represent OAuth2/OIDC clients.

When creating an application:

- Choose the correct grant types for the client type.
- Register only trusted redirect URIs.
- Enable PKCE for browser and mobile clients.
- Use confidential client authentication only when the client can protect a secret.
- Limit scopes to the smallest required set.
- Decide whether user consent is required.
- Decide whether the application requires MFA.

Operational notes:

- Client secrets are returned only when created or rotated.
- Rotating a client secret should be coordinated with the relying application.
- Disabling an application should be preferred over deleting it when investigation or rollback may be needed.

## Users, Roles, And Permissions

Use users, roles, and permissions to express administrative and business authorization.

Recommended workflow:

1. Create or import users.
2. Assign roles based on responsibility.
3. Bind permissions to roles rather than directly to users when possible.
4. Use organizations or tenants to group users when needed.
5. Review high-privilege accounts regularly.

For incident response, disable a user before deleting it so audit history and bindings remain available.

## Login Policies And MFA

Login policies define additional authentication requirements.

Recommended baseline:

- Require MFA for administrator roles.
- Enable application-level MFA for sensitive clients.
- Keep recovery procedures documented before enforcing MFA globally.
- Review failed MFA attempts alongside login failure events.

ArmorAuth supports TOTP and Passkey/WebAuthn flows. Passkeys are suitable for passwordless sign-in or phishing-resistant MFA where browser and device support is available.

## Identity Providers

ArmorAuth can connect to external identity providers through OAuth2/OIDC, SAML, and LDAP/AD.

Before enabling a provider:

1. Configure metadata, issuer, endpoints, or directory connection settings.
2. Map external attributes to local user fields.
3. Decide the linking strategy for existing local users.
4. Test the provider configuration.
5. Decide whether users are auto-registered or must confirm linking.
6. Enable the provider on the hosted login page.

Operational notes:

- Store provider secrets through ArmorAuth configuration or admin APIs; do not commit secrets.
- For LDAP/AD, verify bind credentials, search base, user filter, group mapping, and TLS settings.
- For SAML, keep certificate rotation dates visible to operators.

## Federated Account Linking

Federated bindings connect a local user to an external identity.

Use binding review when:

- A user changes email address.
- An external account is compromised.
- A provider changes its subject identifier format.
- A user needs to unlink and relink an account.

Deleting a binding should not delete the local user unless that is a deliberate account lifecycle action.

## SCIM Provisioning

SCIM endpoints allow external systems to provision users and groups.

Operational guidance:

- Use dedicated credentials for provisioning clients.
- Limit network access where possible.
- Monitor provisioning errors.
- Reconcile deprovisioned users with local sessions and grants.
- Keep group-to-role mapping simple and documented.

## Webhooks

Webhooks deliver selected events to external systems.

Recommended practice:

- Use HTTPS webhook targets.
- Configure a unique secret per endpoint.
- Verify signatures before processing events.
- Monitor delivery failures and retries.
- Rotate webhook secrets when receivers change ownership.

## Actions

Actions are extension points for custom Java behavior.

Before deploying a custom action:

1. Review the implementation for security and side effects.
2. Confirm it handles retries or repeated invocation safely.
3. Validate failure behavior.
4. Record ownership and rollback steps.

## Key And Secret Operations

ArmorAuth encrypts sensitive data at rest and stores signing keys in the database.

Operator responsibilities:

- Configure stable crypto keys before accepting real users.
- Back up crypto key material outside the database.
- Back up the `jwk_key` table with the rest of the database.
- Rotate crypto keys through a staged re-encryption workflow.
- Rotate OAuth client secrets and webhook secrets with the relying party owner.

Do not remove old crypto keys until all ciphertext encrypted by those keys has been migrated.

## Audit And Monitoring

Review the following signals regularly:

- Login successes and failures.
- MFA challenges and failures.
- Token issuance and refresh patterns.
- Admin API changes.
- Identity provider changes.
- Webhook delivery failures.
- Authorization check denials.
- Database and migration errors.

High-risk changes should be tied to a ticket, operator, and timestamp.

## Backup And Restore Drills

Run restore drills before a production incident.

Minimum drill:

1. Restore a recent database backup into an isolated environment.
2. Provide the matching crypto key configuration.
3. Start ArmorAuth with the restored data.
4. Confirm users, clients, JWK keys, and encrypted provider secrets can be read.
5. Validate a login and token issuance flow.

## Incident Playbooks

For suspected admin account compromise:

1. Disable or rotate credentials for the account.
2. Review recent audit events.
3. Rotate affected application, provider, and webhook secrets.
4. Revoke suspicious grants where appropriate.
5. Capture evidence before deleting records.

For signing key exposure:

1. Stop accepting traffic if exposure is active.
2. Rotate JWK keys.
3. Revoke affected tokens if needed.
4. Confirm relying parties refresh JWKS.
5. Review database and crypto key access.

For provider secret exposure:

1. Rotate the secret at the external provider.
2. Update ArmorAuth configuration.
3. Test login with the provider.
4. Review federated login events during the exposure window.
