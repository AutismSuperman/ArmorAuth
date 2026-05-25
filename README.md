<p align="center">
  <img src="./armorauth-admin-ui/public/brand/logo.svg" alt="ArmorAuth logo" width="720">
</p>

# ArmorAuth

ArmorAuth is an open source identity and authorization platform built on Spring Security and Spring Authorization Server. It provides a self-hosted authorization server, an admin API, an admin console, and hosted authentication pages for teams that need private deployment, OAuth 2.0 / OpenID Connect compatibility, and extensible identity workflows.

The project is designed for product teams and platform engineers who want a clear Java/Spring codebase rather than a black-box identity service.

## Highlights

- OAuth 2.0 and OpenID Connect authorization server based on Spring Authorization Server.
- Hosted login, consent, MFA, device activation, and federated identity confirmation pages.
- Admin API and Vue admin console for applications, users, roles, permissions, organizations, identity providers, login policies, audit data, webhooks, and token statistics.
- Persistent JWK storage and encrypted-at-rest secrets for identity providers, webhooks, TOTP data, and signing keys.
- Local authentication, captcha login, MFA, TOTP, Passkey/WebAuthn, and passwordless sign-in flows.
- Federated identity support for OAuth2/OIDC providers, SAML SP flows, LDAP/AD bind login, and account linking.
- SCIM 2.0 provisioning, webhook delivery, Java SPI actions, and authorization check APIs.
- Flyway-managed schema migrations for repeatable deployment.

## Architecture

| Module | Purpose |
| --- | --- |
| `armorauth-common` | Shared response, exception, validation, and audit context utilities |
| `armorauth-model` | JPA entities and repositories |
| `armorauth-core` | Authorization server, authentication, MFA, JWK, secret protection, and persistence adapters |
| `armorauth-federation` | Federated login orchestration, account confirmation, and provider SPI |
| `armorauth-federation-providers` | Built-in provider integrations and metadata |
| `armorauth-admin` | Admin REST API |
| `armorauth-admin-ui` | Vue 3 admin console |
| `armorauth-server-ui` | Hosted identity pages and static assets |
| `armorauth-server` | Runnable Spring Boot server |
| `armorauth-samples` | Integration samples for OAuth/OIDC clients |

## User-Facing Surfaces

- Hosted identity pages: login, authorization consent, MFA, device activation, activation result, federated account confirmation.
- Admin console: application management, user management, identity providers, login policies, federated bindings, monitoring, and operational views.
- Admin API: stable management surface under `/api/admin/v1`.
- Account API: self-service factor management under `/api/account/v1`.
- Standard protocol endpoints: OAuth 2.0, OpenID Connect discovery, JWKS, token, introspection, revocation, logout, and device authorization.

## Documentation

| Document | Purpose |
| --- | --- |
| [Quick Start](docs/quick-start.md) | Build and run ArmorAuth for local development |
| [Operation Manual](docs/operation-manual.md) | Day-to-day administration and operational workflows |
| [Deployment Guide](docs/deployment-guide.md) | Production deployment, proxy, database, backup, and security guidance |
| [API Reference](docs/api-reference.md) | Admin API and account API reference |
| [Federation Configuration](docs/federation-config.md) | OAuth2/OIDC, SAML, and LDAP identity provider setup |
| [MFA Configuration](docs/mfa-config.md) | MFA, TOTP, Passkey, and application policy setup |
| [Security Best Practices](docs/security-best-practices.md) | Security checklist and hardening notes |
| [Roadmap](docs/ROADMAP.md) | Product direction and implementation phases |
| [Project Status](docs/IMPLEMENTATION_STATUS.md) | Current capability status and follow-up areas |

## Requirements

- JDK 21+
- Maven 3.9+
- MySQL 8.0+ for shared or production-like environments
- Node.js 18+ when working on the admin console

## Build From Source

```bash
mvn -pl armorauth-server -am package -DskipTests
```

The runnable server artifact is produced at:

```text
armorauth-server/target/armorauth-server-0.0.1.jar
```

See [Quick Start](docs/quick-start.md) for the local development flow and [Deployment Guide](docs/deployment-guide.md) for production configuration.

## Security Notes

ArmorAuth stores signing keys and sensitive integration secrets in the database. Production environments must provide stable cryptographic keys, a stable issuer URL, HTTPS-only cookies, controlled admin access, and database backup procedures that include the JWK table and crypto key material.

Default development credentials and built-in seed data are for local development only.

## Project Status

ArmorAuth is under active development. The current codebase includes the main identity, administration, federation, MFA, webhook, SCIM, and authorization-check capabilities needed for private deployment evaluation. Remaining work focuses on hardening, packaging, SDKs, starter ergonomics, enterprise federation depth, and broader deployment automation.

## License

Apache License 2.0
