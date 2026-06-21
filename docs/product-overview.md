# ArmorAuth Product Overview

ArmorAuth is a self-hosted identity, authentication, and authorization platform for teams that need private deployment, standard OAuth2/OIDC compatibility, and direct control over user, tenant, application, and security data.

It is built on Spring Security and Spring Authorization Server, and packages the authorization server, hosted identity pages, admin API, admin console, Spring Boot starter, and runnable samples in one repository.

## Who It Is For

ArmorAuth fits these scenarios:

- Internal platforms that need a shared OAuth2/OIDC authorization server.
- SaaS products that need tenant-aware identity and application management.
- Enterprise systems that need local users plus OAuth2/OIDC, SAML, LDAP/AD, and social or enterprise federation.
- Teams that want Spring-native code they can inspect, extend, and deploy in their own environment.

## Core Capabilities

| Area | What ArmorAuth Provides |
| --- | --- |
| Authorization server | Authorization Code, Client Credentials, Refresh Token, Device Authorization, discovery, JWKS, introspection, revocation, and OIDC logout |
| Hosted identity pages | Sign-in, consent, MFA challenge, device activation, activation result, account center, and federated account confirmation |
| Account security | Password login, captcha, SMS code login, TOTP MFA, Passkey/WebAuthn, login step-up, contact verification, and session visibility |
| Administration | Applications, scopes, users, roles, permissions, tenants, organizations, identity providers, sessions, audit logs, webhooks, token statistics, and JWK metadata |
| Federation | OAuth2/OIDC, SAML SP login, LDAP/AD bind/search login, built-in provider metadata, and account linking |
| Provisioning and integration | SCIM 2.0 users/groups, authorization-check API, webhooks, Java action SPI, and Spring Boot starter integration |
| Operations | Persistent JWK storage, encrypted secrets, Flyway migrations, MySQL production profile, H2 local profile, and deployment guidance |

## System Surfaces

ArmorAuth has three main user-facing surfaces:

- Hosted identity pages on the server port, usually `9000`, for login, consent, account security, and authorization flows.
- Admin REST API under `/api/admin/v1` for platform operations and automation.
- Vue admin console for operators who manage applications, users, tenants, identity providers, and security policies.

Business applications integrate with ArmorAuth as OAuth2/OIDC clients or resource servers. Spring Boot applications can use `armorauth-spring-boot-starter` for resource-server setup, OIDC login, current-user context, Admin API calls, and token relay.

## Data Model At A Glance

- A tenant groups applications, users, organizations, and issuer-facing behavior.
- An organization represents a hierarchy or business unit inside a tenant.
- An application is an OAuth2/OIDC registered client.
- Users can have local credentials, verified contact methods, roles, organization membership, MFA factors, Passkeys, and federated account bindings.
- Roles and permissions provide management and application-side authorization data.
- Identity providers describe external OAuth2/OIDC, SAML, LDAP, or built-in provider integrations.

## Recommended Evaluation Path

1. Run the system locally with [Quick Start](quick-start.md).
2. Review [Basic Usage](basic-usage.md) to create a tenant, application, and test user.
3. Configure a Spring Boot relying service with [Spring Boot Starter](spring-boot-starter.md).
4. Review [Security Best Practices](security-best-practices.md) before using shared or production data.
5. Use [Deployment Guide](deployment-guide.md) for production deployment planning.
