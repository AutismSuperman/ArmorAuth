# ArmorAuth Quick Start

This guide gets a local ArmorAuth environment running from source. It is intended for contributors and evaluators; production setup belongs in [Deployment Guide](deployment-guide.md).

## Prerequisites

| Dependency | Version |
| --- | --- |
| JDK | 21+ |
| Maven | 3.9+ |
| MySQL | 8.0+ |
| Node.js | 18+ |

## Prepare The Database

Create an empty database for ArmorAuth:

```sql
CREATE DATABASE IF NOT EXISTS identity_server
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

Configure the datasource with environment variables or Spring Boot arguments:

```bash
SPRING_DATASOURCE_URL=jdbc:mysql://127.0.0.1:3306/identity_server
SPRING_DATASOURCE_USERNAME=armorauth
SPRING_DATASOURCE_PASSWORD=<password>
```

Flyway applies schema migrations automatically when the server starts.

## Configure Development Secrets

For local development you can provide a single crypto secret:

```bash
ARMORAUTH_CRYPTO_SECRET=<local-development-secret>
```

For key rotation testing:

```bash
ARMORAUTH_CRYPTO_SECRET=<old-secret>
ARMORAUTH_CRYPTO_KEYS=v2=<new-secret>
ARMORAUTH_CRYPTO_ACTIVE_KEY_ID=v2
```

Do not reuse development secrets in shared or production environments.

## Build The Server

Run from the repository root:

```bash
mvn -pl armorauth-server -am package -DskipTests
```

The server jar is created at:

```text
armorauth-server/target/armorauth-server-1.0.0.jar
```

## Run The Server

Start the packaged server:

```bash
java -jar armorauth-server/target/armorauth-server-1.0.0.jar
```

The server exposes hosted identity pages, OAuth/OIDC protocol endpoints, and the admin API.

For local H2-based development you can use the `local` profile:

```bash
java -jar armorauth-server/target/armorauth-server-1.0.0.jar --spring.profiles.active=local
```

## Run The Admin Console

```bash
cd armorauth-admin-ui
npm install
npm run dev
```

The Vite dev server proxies API calls to the ArmorAuth server.

## Verify The Environment

Check the server health and OpenID Connect discovery document:

```bash
curl http://localhost:9000/actuator/health
curl http://localhost:9000/.well-known/openid-configuration
```

Default development seed data includes an administrator account for local evaluation. Replace it before using any shared environment.

## Next Steps

- Configure the first OAuth/OIDC application in the admin console.
- Review [Operation Manual](operation-manual.md) for administration workflows.
- Review [Deployment Guide](deployment-guide.md) before exposing the service outside a local machine.
- Review [Security Best Practices](security-best-practices.md) for hardening guidance.
