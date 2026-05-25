# ArmorAuth Project Status

This document summarizes the current project capability status. Historical local run logs and one-off verification notes are intentionally excluded from the public project documentation.

## Current Scope

ArmorAuth currently provides:

- OAuth 2.0 and OpenID Connect authorization server flows.
- Hosted identity pages for login, consent, MFA, device activation, activation result, and federated confirmation.
- Admin API and admin console for applications, scopes, users, roles, permissions, organizations, identity providers, login policies, audit, webhooks, token statistics, and JWK metadata.
- Persistent JWK storage and encrypted-at-rest secrets.
- Password, captcha, TOTP, MFA, Passkey/WebAuthn, and passwordless login capabilities.
- Federated identity through OAuth2/OIDC providers, SAML SP login, LDAP/AD bind login, and account linking.
- SCIM 2.0 provisioning endpoints.
- Webhook delivery and Java SPI actions.
- Authorization check API for application-side permission decisions.
- Flyway migrations for MySQL and local H2 development profiles.

## Capability Matrix

| Area | Status | Notes |
| --- | --- | --- |
| OAuth2/OIDC core | Available | Authorization code, refresh, client credentials, device flow, discovery, JWKS, revocation, introspection, logout |
| Hosted login UI | Available | Unified server-side identity pages with Chinese/English UI switching |
| Admin API | Available | Management surface under `/api/admin/v1` |
| Admin console | Available | Vue 3 console for core management workflows |
| User and access model | Available | Users, roles, permissions, organizations, tenants, login policy hooks |
| MFA and Passkey | Available | TOTP, MFA challenge flow, WebAuthn assertion, passwordless entry |
| Federation | Available | OAuth2/OIDC, SAML SP, LDAP/AD bind/search, account linking |
| SCIM | Available | User and group provisioning surface |
| Webhooks | Available | Signed delivery records and retry-oriented model |
| Actions | Available | Java SPI extension points |
| Secret protection | Available | Versioned encrypted value format and key-ring rotation workflow |
| Deployment docs | Available | Production configuration, reverse proxy, backup, and security checklist |

## Recently Completed

- Unified hosted identity page styling across login, consent, MFA, device activation, activation result, home, and federated confirmation pages.
- Added Chinese/English switching for server-side UI pages.
- Added closeable and auto-dismissable notification behavior.
- Fixed federated confirmation cancellation so users can return to login and clear the pending federated session.
- Added distinct functional illustrations for each hosted identity page.
- Added identity provider display preferences and icon metadata support.
- Extended login policy and MFA-related management behavior.

## Known Follow-Up Areas

- Publish polished Docker/Kubernetes examples and release packaging.
- Improve Spring Boot starter ergonomics for relying services.
- Add SDKs or client helpers for common integration stacks.
- Deepen enterprise federation features such as SAML IdP mode, SLO orchestration, and certificate lifecycle tooling.
- Add stronger WebAuthn attestation trust-store management.
- Expand operational dashboards and alert templates.
- Continue hardening test coverage around multi-tenant and high-availability deployment paths.

## Verification Policy

For code changes, contributors should run the narrowest useful checks first and broaden validation when a change touches shared behavior. Typical checks include:

- Module compilation or package build.
- Focused unit or integration tests for changed modules.
- OAuth/OIDC protocol smoke tests when authorization behavior changes.
- Hosted UI rendering checks when templates, static assets, or i18n behavior changes.
- Migration checks when database schema changes.

Record durable product behavior in documentation. Avoid committing local PIDs, machine-specific paths, transient logs, or demo run transcripts.
