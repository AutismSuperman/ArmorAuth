# ArmorAuth Development Seed Profile

ArmorAuth includes a development-oriented seed profile for local UI and API exploration. It is not part of the production deployment model.

## Purpose

The profile creates representative data for:

- Users and roles.
- Tenants and organizations.
- OAuth2/OIDC applications.
- Identity providers.
- Webhooks.
- Audit events.
- Token statistics.

This helps contributors work on management screens and integration flows without preparing every record manually.

## Usage

Run the server with the `mock` profile from a local development environment:

```bash
mvn -pl armorauth-server -am spring-boot:run -Dspring-boot.run.profiles=mock
```

The profile uses an H2 file database under the local runtime directory. Do not commit generated runtime files.

## Seed Accounts

Seed accounts are intended for local development only. Passwords and test users must not be reused in shared environments.

| Username | Role |
| --- | --- |
| `admin` | Super administrator |
| `app.manager` | Application administrator |
| `audit.viewer` | Audit reviewer |
| `demo.user` | Regular user |

## Notes For Contributors

- Keep seed data realistic enough to exercise the admin console.
- Keep local machine paths, proxy settings, PIDs, logs, and transient command output out of this document.
- Prefer adding reusable seed records over documenting one-off manual setup.
