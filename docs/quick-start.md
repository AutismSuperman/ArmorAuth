# ArmorAuth 快速开始

这条路径以当前默认 MySQL profile 为准，目标是在本地跑起：

- ArmorAuth Server: `http://localhost:9000`
- Admin UI: `http://localhost:1080`
- 默认管理员: `admin / admin123`

## 1. 前置条件

| 依赖 | 建议版本 | 说明 |
| --- | --- | --- |
| JDK | 21+ | 项目编译目标为 Java 21 |
| Maven | 3.9+ | 用于构建 Spring Boot server |
| MySQL | 8.0+ | 默认 profile 使用 MySQL |
| Node.js | 18+ | 仅启动 `armorauth-admin-ui` 时需要 |

## 2. 准备数据库

创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS identity_server
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

默认本地连接在 `armorauth-server/src/main/resources/application-mysql.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/identity_server?zeroDateTimeBehavior=convertToNull
    username: root
    password: wangle
```

启动后 Flyway 会自动执行：

```text
armorauth-server/src/main/resources/db/migration-mysql
armorauth-server/src/main/resources/db/migration
```

如果你的本地密码不同，可以用环境变量或启动参数覆盖：

```bash
SPRING_DATASOURCE_PASSWORD=your_password
```

## 3. 配置本地 secret key

本地可以直接使用默认开发 key；如果要模拟生产配置，建议显式设置：

```bash
ARMORAUTH_CRYPTO_SECRET=local-v1-secret
```

需要验证 key rotation 时：

```bash
ARMORAUTH_CRYPTO_SECRET=local-v1-secret
ARMORAUTH_CRYPTO_KEYS=v2=local-v2-secret
ARMORAUTH_CRYPTO_ACTIVE_KEY_ID=v2
```

说明：

- `ARMORAUTH_CRYPTO_SECRET` 作为 `v1` key，用来读取已有 `{enc}v1:` 数据。
- `ARMORAUTH_CRYPTO_KEYS` 是额外 key-ring，格式为 `keyId=secret`，多个条目用逗号分隔。
- `ARMORAUTH_CRYPTO_ACTIVE_KEY_ID` 决定新写入 secret 使用哪个 key id。
- 当前 parser 用逗号分隔 key-ring，所以 secret 内不要包含逗号。

## 4. 构建 server

在仓库根目录执行：

```bash
mvn -DskipTests -Dspotless.apply.skip=true -Dspotless.check.skip=true -pl armorauth-server -am package
```

构建产物：

```text
armorauth-server/target/armorauth-server-0.0.1.jar
```

## 5. 启动 server

默认 `spring.profiles.active=mysql`，所以可以直接启动：

```bash
java -jar armorauth-server/target/armorauth-server-0.0.1.jar
```

如果要显式指定 MySQL profile：

```bash
java -jar armorauth-server/target/armorauth-server-0.0.1.jar --spring.profiles.active=mysql
```

H2 调试 profile：

```bash
java -jar armorauth-server/target/armorauth-server-0.0.1.jar --spring.profiles.active=local
```

`local` profile 会使用 H2 文件库，并关闭联合登录编排，适合无 MySQL 时做基础页面和接口调试。

## 6. 启动 Admin UI

新开一个终端：

```bash
cd armorauth-admin-ui
npm install
npm run dev
```

Vite 默认端口为 `1080`，并将 `/api` 代理到 `http://localhost:9000`。

访问：

```text
http://localhost:1080
```

登录：

```text
admin / admin123
```

## 7. 本地 smoke check

服务健康：

```bash
curl http://localhost:9000/actuator/health
```

OIDC discovery：

```bash
curl http://localhost:9000/.well-known/openid-configuration
```

JWKS：

```bash
curl http://localhost:9000/oauth2/jwks
```

管理 API：

```bash
curl -u admin:admin123 "http://localhost:9000/api/admin/v1/applications?page=0&size=1"
curl -u admin:admin123 "http://localhost:9000/api/admin/v1/users?page=0&size=1"
curl -u admin:admin123 "http://localhost:9000/api/admin/v1/jwk-keys"
```

Client credentials token：

```bash
curl -u f62ac251-36d7-42c8-9f75-c31c90111bd4:secret \
  -d "grant_type=client_credentials" \
  -d "scope=message.read" \
  http://localhost:9000/oauth2/token
```

## 8. 创建第一个应用

使用管理 API 创建 OIDC Web App：

```bash
curl -X POST http://localhost:9000/api/admin/v1/applications \
  -H "Content-Type: application/json" \
  -u admin:admin123 \
  -d '{
    "clientName": "My Web App",
    "clientAuthenticationMethods": "client_secret_basic",
    "authorizationGrantTypes": "authorization_code,refresh_token",
    "redirectUris": "http://localhost:8080/login/oauth2/code/armorauth",
    "scopes": ["openid", "profile", "email"],
    "clientSettings": {
      "requireAuthorizationConsent": true,
      "requireProofKey": false
    }
  }'
```

返回结果中的 `clientSecret` 只在创建时返回；后续需要通过 rotate 接口重新生成。

## 9. 常用默认数据

| 用途 | 值 |
| --- | --- |
| 管理员 | `admin / admin123` |
| Server | `http://localhost:9000` |
| Admin UI | `http://localhost:1080` |
| Confidential client id | `f62ac251-36d7-42c8-9f75-c31c90111bd4` |
| Confidential client secret | `secret` |
| React SPA PKCE client id | `react-spa-pkce` |
| Device flow client id | `8ee3a98e-89a8-438d-a314-1ef9df815279` |

OAuth sample host 建议：

```text
127.0.0.1 armorauth-demo
127.0.0.1 armorauth-server
```

访问 sample 时不要混用 `armorauth-demo` 和 `127.0.0.1`，否则浏览器 session cookie 可能不一致。

## 10. 下一步

- [部署指南](deployment-guide.md)
- [API Reference](api-reference.md)
- [联合登录配置](federation-config.md)
- [MFA 配置](mfa-config.md)
- [OAuth2 / OIDC 概念](oauth2-oidc-concepts.md)
