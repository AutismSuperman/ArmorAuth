# ArmorAuth Basic Usage

This guide describes the first operational workflows after ArmorAuth is running. Use it with [Quick Start](quick-start.md) for local evaluation or with [Deployment Guide](deployment-guide.md) for shared environments.

## 1. Sign In To The Admin Console

Open the admin console and sign in with an administrator account.

For local development, seed accounts are available only when the development seed profile is enabled. See [Development Seed Profile](mock-system.md).

Before using a shared environment:

- Replace development credentials.
- Confirm the public issuer URL.
- Configure stable crypto keys.
- Restrict administrator access by role and network.

## 2. Create Or Review A Tenant

Tenants group applications, users, organizations, and tenant-aware issuer paths.

For a new tenant:

1. Create a tenant code that is stable and URL-safe.
2. Set the display name and status.
3. Configure branding and domain metadata when needed.
4. Use tenant-aware paths such as `/t/{tenantCode}` for tenant-specific flows.

Use a single default tenant when you do not need tenant isolation.

## 3. Create Organizations And Roles

Organizations model departments, business units, or customer-side hierarchy.

Recommended workflow:

1. Create top-level organizations first.
2. Add child organizations only when hierarchy affects authorization or reporting.
3. Assign users to organizations.
4. Bind roles through roles and permissions instead of one-off direct user grants when possible.

Keep administrator roles small and review them regularly.

## 4. Create An OAuth2/OIDC Application

Applications represent OAuth2/OIDC clients.

When creating an application:

- Choose the tenant.
- Select the client type.
- Register only trusted redirect URIs.
- Use Authorization Code with PKCE for browser and mobile clients.
- Use Client Credentials only for service-to-service clients.
- Store client secrets only in confidential backends.
- Limit scopes to the minimum required set.
- Enable MFA policy for sensitive applications when needed.

After creation, use application endpoint details in the admin console to copy discovery, authorization, token, JWKS, introspection, revocation, and logout URLs.

## 5. Create Users And Verify Contact Methods

Create local users or provision them through SCIM or federation.

For local users:

1. Set username and display name.
2. Add email and phone number when available.
3. Verify email and phone number before depending on them for account recovery or login.
4. Assign roles and organization membership.
5. Disable rather than delete users when audit history matters.

Users can manage their own account security from the hosted account center after sign-in. Contact verification flows show mock verification codes in local development so evaluators can complete the flow without external SMS or email providers.

## 6. Configure Login And MFA

Use login policies and application settings to decide when additional verification is required.

Recommended baseline:

- Require MFA for administrators.
- Require application-level MFA for sensitive clients.
- Let users bind an Authenticator app from the account center.
- Keep recovery and support procedures ready before enforcing MFA broadly.
- Review failed login and MFA attempts in audit views.

See [MFA Configuration](mfa-config.md) for TOTP, Passkey, and policy details.

## 7. Test A Relying Application

For Spring Boot services:

1. Add `armorauth-spring-boot-starter`.
2. Configure either Resource Server or OIDC Login.
3. Point the service to the ArmorAuth issuer.
4. Read user, tenant, organization, role, scope, and permission claims through the starter current-user resolver.
5. Use token relay only for trusted downstream services.

See [Spring Boot Starter](spring-boot-starter.md) for configuration examples and [Spring Boot Starter Extension Spec](spring-boot-starter-extension-spec.md) for extension points.

## 8. Review Activity

After testing login and token flows, review:

- Active sessions.
- Audit logs.
- Token statistics.
- Webhook delivery status.
- Failed login and MFA events.
- Authorization check denials.

These views help confirm that applications are issuing the expected grants and that user-facing security controls are working.
