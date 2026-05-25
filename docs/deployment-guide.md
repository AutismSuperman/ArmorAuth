# ArmorAuth Deployment Guide

This guide covers production-oriented deployment concerns for ArmorAuth. It assumes the server is built from source or from an internal release pipeline.

## Deployment Model

A typical deployment includes:

- `armorauth-server` running as a Spring Boot service.
- MySQL as the primary relational database.
- A reverse proxy or ingress terminating HTTPS.
- A secret management system for database credentials and ArmorAuth crypto keys.
- Centralized logs, metrics, backups, and alerting.

The admin console can run behind the same domain as the server or as a separately deployed frontend that calls the admin API.

## Required Configuration

| Area | Required Setting |
| --- | --- |
| Issuer | Set a stable external issuer URL |
| Database | Use a managed MySQL database and a least-privilege database user |
| HTTPS | Terminate TLS at the proxy or ingress |
| Cookies | Enable secure and HTTP-only session cookies |
| Crypto | Configure stable encryption keys and key rotation policy |
| Admin | Replace development credentials and restrict admin access |
| Backup | Include JWK keys, application data, users, grants, and Flyway metadata |

Example environment variables:

```bash
SPRING_DATASOURCE_URL=jdbc:mysql://db.example.internal:3306/identity_server
SPRING_DATASOURCE_USERNAME=armorauth
SPRING_DATASOURCE_PASSWORD=<strong-password>

SPRING_SECURITY_OAUTH2_AUTHORIZATIONSERVER_ISSUER=https://auth.example.com

SERVER_SERVLET_SESSION_COOKIE_SECURE=true
SERVER_SERVLET_SESSION_COOKIE_HTTPONLY=true

ARMORAUTH_CRYPTO_SECRET=<v1-secret>
ARMORAUTH_CRYPTO_KEYS=v2=<v2-secret>
ARMORAUTH_CRYPTO_ACTIVE_KEY_ID=v2
```

## Reverse Proxy

Forward the original host and protocol headers so generated URLs, redirects, and cookies match the public issuer.

```nginx
server {
    listen 443 ssl http2;
    server_name auth.example.com;

    ssl_certificate /etc/nginx/ssl/auth.example.com.crt;
    ssl_certificate_key /etc/nginx/ssl/auth.example.com.key;

    location / {
        proxy_pass http://127.0.0.1:9000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $host;
        proxy_set_header X-Forwarded-Port $server_port;
    }
}
```

Enable forwarded header processing:

```yaml
server:
  forward-headers-strategy: native
```

## Database Migration

ArmorAuth uses Flyway migrations. Deployment pipelines should:

1. Back up the database before upgrading.
2. Run the new server version in a controlled rollout.
3. Let Flyway apply pending migrations at startup, or run migrations as a dedicated release step.
4. Confirm application health before routing full traffic.

Never enable Hibernate schema mutation for production databases.

## Secret Encryption And Rotation

ArmorAuth stores sensitive fields as encrypted payloads:

```text
{enc}<keyId>:<payload>
```

Protected data includes identity provider secrets, webhook secrets, TOTP data, and JWK private keys.

Rotation workflow:

1. Keep the existing key configured.
2. Add a new key in `ARMORAUTH_CRYPTO_KEYS`.
3. Set `ARMORAUTH_CRYPTO_ACTIVE_KEY_ID` to the new key id.
4. Re-encrypt existing records through the admin rekey operation.
5. Validate all nodes can read existing encrypted values.
6. Remove the old key only after all old ciphertext has been re-encrypted and backed up.

## Backup And Restore

Back up the full database and the external secret configuration together. The following data is especially important:

| Data | Why It Matters |
| --- | --- |
| `jwk_key` | Existing access tokens and ID tokens depend on these signing keys |
| OAuth2 clients | Applications and redirect configuration |
| Users, roles, permissions, organizations | Identity and authorization data |
| Identity providers and federated bindings | External identity integration state |
| OAuth2 authorizations and consents | Active grants and user consent |
| Flyway history | Schema migration state |

Losing the database or crypto keys can invalidate existing tokens and make encrypted secrets unreadable.

## Observability

Use the health endpoint for liveness and readiness checks:

```text
/actuator/health
```

Restrict operational endpoints such as metrics and info to trusted operators. Collect application logs centrally and alert on repeated login failures, token issuance anomalies, webhook delivery failures, and database connectivity problems.

## Security Checklist

- Use HTTPS for every external OAuth/OIDC endpoint.
- Configure a stable issuer that matches the public URL.
- Use secure, HTTP-only cookies.
- Replace all development credentials.
- Store secrets in a secret manager, not in source control.
- Restrict admin API access by network and role.
- Back up the database and crypto keys together.
- Keep old crypto keys during rotation until all encrypted rows are migrated.
- Review audit logs regularly.
- Test restore procedures before depending on backups.

## Upgrade Checklist

1. Read release notes and migration notes.
2. Back up database and secret configuration.
3. Build or pull the new server artifact.
4. Deploy to a staging environment first.
5. Run health, login, token, admin API, and federation checks.
6. Roll out gradually.
7. Keep rollback artifacts and previous secrets available until the rollout is complete.
