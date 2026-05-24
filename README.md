<p align="center">
  <img src="./armorauth-admin-ui/public/brand/logo.svg" alt="ArmorAuth logo" width="720">
</p>

# ArmorAuth

> Shield-first identity infrastructure for modern Spring Security workflows.

ArmorAuth 是一个基于 Spring Security 和 Spring Authorization Server 的认证授权项目。当前仓库已经切到 `Java 21`、`Spring Boot 4.0.5`、`Spring Security 7.0.4`、`Spring Authorization Server 7.0.4`，并围绕“可私有化部署、可管理、可验证”的方向补齐了管理 API、管理台前端、Flyway 迁移、OAuth/OIDC 回归测试、密钥持久化、敏感字段加密、MFA/Passkey、联合身份源、SCIM、Webhook、Actions 和高级授权闭环。

项目仍在快速迭代，但现在已经不是单纯 demo：本地可以用 MySQL 跑完整 server + admin UI，可以通过管理 API 配应用、用户、角色、权限、组织、身份源、Webhook、审计、登录策略、JWK 元数据和授权检查，也可以用内置 seed client 做 OAuth/OIDC smoke。`docs/ROADMAP.md` 当前产品范围已完成到 100%，最终实现记录见 `docs/IMPLEMENTATION_STATUS.md` 的 Step 20。

## 最近更新

- ROADMAP 当前产品范围完成到 100%：Passkey/WebAuthn、SAML SP、LDAP/AD、SCIM、Webhook、Actions Java SPI、FGA 风格授权检查均已落地。
- Passkey/WebAuthn 已支持注册 `attestationObject` 验证、MFA assertion、passwordless 登录，并完成本地运行时烟测。
- 联合身份能力补齐：SAML SP 发起登录、LDAP/AD 同步、LDAP bind 实时登录、外部账号绑定和组到角色映射。
- 扩展与授权补齐：Webhook 签名/重试、Actions Java SPI、`POST /api/admin/v1/authorization/check` 授权检查接口。
- 运行基线统一到 `Java 21`、`Spring Boot 4.0.5`、`Spring Security 7.0.4`。
- `armorauth-server` 默认使用 MySQL profile，并通过 Flyway 管理 schema 和 seed data。
- `armorauth-admin` 已提供 `/api/admin/v1/**` 管理 API。
- `armorauth-admin-ui` 已接入真实管理 API，默认 Vite 端口 `1080`。
- OAuth/OIDC discovery、JWKS、authorization code + PKCE、client credentials、refresh、revocation、introspection、device flow 已有端到端测试覆盖。
- JWK 私钥持久化到数据库，并使用可逆 secret protection 加密保存。
- IdP client secret、Webhook secret、TOTP secret、JWK private key 已支持 `{enc}<keyId>:` 加密格式和 key-ring 轮换。

## 模块结构

| 模块 | 说明 |
| --- | --- |
| `armorauth-common` | 通用响应、异常、审计上下文、校验等基础能力 |
| `armorauth-model` | JPA 实体与 Repository |
| `armorauth-core` | OAuth2/OIDC 授权服务器、本地登录、验证码登录、MFA、JWK、密钥保护、JPA 持久化适配 |
| `armorauth-federation` | 联合登录编排、确认页、动态 ClientRegistration、安全处理器、provider SPI |
| `armorauth-federation-providers` | QQ / 微信 / Gitee 等 provider 实现与默认元数据 |
| `armorauth` | 对核心模块的聚合封装和自动配置 |
| `armorauth-admin` | 管理后端，提供 `/api/admin/v1/**` |
| `armorauth-admin-ui` | Vue 3 + Ant Design Vue 管理控制台 |
| `armorauth-server` | 可独立启动的认证服务端 |
| `armorauth-server-ui` | 服务端登录、授权、MFA、设备授权页面模板和静态资源 |
| `armorauth-spring-boot` | Spring Boot 聚合模块 |
| `armorauth-spring-boot/armorauth-spring-boot-starter` | 预留 starter 模块 |
| `armorauth-samples` | OIDC、PKCE、`client_credentials` 等样例 |

## 当前能力

- Spring Authorization Server / OIDC 基础能力。
- JPA 版 `RegisteredClientRepository`、`OAuth2AuthorizationService`、`OAuth2AuthorizationConsentService`。
- Discovery、JWKS、revocation、introspection、logout、device authorization 等标准端点。
- 自定义登录页 `/login`、授权确认页 `/consent`、设备授权页 `/activate`、MFA 页。
- 管理 API：应用、scope、用户、角色、权限、组织、租户、身份源、联合绑定、登录策略、审计、Webhook、token 统计、JWK 元数据。
- 管理台前端：应用、用户、组织、身份源、scope、登录策略、联合绑定、Webhook、审计、监控等页面。
- RBAC、密码策略、账号锁定、登录限流、MFA/TOTP 基础能力。
- Passkey/WebAuthn：注册 attestation 验证、MFA assertion 验证、passwordless 登录。
- SAML SP、LDAP/AD、SCIM 2.0 用户/组 provisioning。
- 联合登录自动注册 / 中间页确认双策略。
- Webhook 签名、投递记录和基础重试。
- Actions Java SPI 和 FGA 风格授权检查。
- JWK 私钥数据库持久化和敏感字段加密 at rest。
- Secret key-ring 轮换：旧 `{enc}v1:` 可读，新写入可切到 `v2` 等 active key。

## 技术栈

- Java 21
- Spring Boot 4.0.5
- Spring Security 7.0.4
- Spring Authorization Server 7.0.4
- Spring Data JPA
- Flyway
- MySQL 8.0+
- H2（仅本地 `local` profile 调试）
- FreeMarker
- Vue 3 / Vite / Ant Design Vue

## 快速开始

详细步骤见 [docs/quick-start.md](docs/quick-start.md)。

本地默认路径：

1. 准备 MySQL 数据库 `identity_server`。
2. 打包 server。
3. 启动 `armorauth-server`，默认端口 `9000`。
4. 启动 `armorauth-admin-ui`，默认端口 `1080`。
5. 使用 `admin / admin123` 登录管理台。

默认本地 MySQL 配置在 [application-mysql.yml](armorauth-server/src/main/resources/application-mysql.yml)：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/identity_server?zeroDateTimeBehavior=convertToNull
    username: root
    password: wangle
```

生产或共享环境必须覆盖数据库密码、issuer、cookie secure 和 crypto key 配置。

## 常用命令

根目录打包：

```bash
mvn -DskipTests -Dspotless.apply.skip=true -Dspotless.check.skip=true -pl armorauth-server -am package
```

启动 server：

```bash
java -jar armorauth-server/target/armorauth-server-0.0.1.jar
```

启动 admin UI：

```bash
cd armorauth-admin-ui
npm install
npm run dev
```

健康检查：

```bash
curl http://localhost:9000/actuator/health
curl http://localhost:9000/.well-known/openid-configuration
curl http://localhost:9000/oauth2/jwks
curl -u admin:admin123 "http://localhost:9000/api/admin/v1/applications?page=0&size=1"
curl -u admin:admin123 -H "Content-Type: application/json" \
  -d '{"username":"admin","permissionCode":"roadmap:any"}' \
  http://localhost:9000/api/admin/v1/authorization/check
```

## Secret 加密与 key-ring

以下数据会以 `{enc}<keyId>:` 格式加密保存：

- Identity Provider `clientSecret`
- Webhook `secret`
- TOTP secret / recovery data 中的可逆 secret
- `jwk_key.private_key`

本地开发未配置时会使用默认开发 key。生产环境必须显式配置：

```bash
ARMORAUTH_CRYPTO_SECRET=old-or-v1-production-secret
ARMORAUTH_CRYPTO_KEYS=v2=new-production-secret
ARMORAUTH_CRYPTO_ACTIVE_KEY_ID=v2
```

说明：

- `ARMORAUTH_CRYPTO_SECRET` 会作为 `v1` fallback，用来读取已有 `{enc}v1:` 数据。
- `ARMORAUTH_CRYPTO_KEYS` 用逗号分隔 `keyId=secret`，当前 parser 不支持 secret 内含逗号。
- `ARMORAUTH_CRYPTO_ACTIVE_KEY_ID` 控制新写入值使用哪个 key id。
- 轮换期间要保留旧 key，先用 `POST /api/admin/v1/secret-protection/rekey` dry-run，再执行存量重加密，确认不再有旧 key 数据后再移除旧 key。

## 样例工程

默认 seed client：

| 用途 | Client ID | Secret / Auth |
| --- | --- | --- |
| Confidential OIDC / client credentials | `f62ac251-36d7-42c8-9f75-c31c90111bd4` | `secret` |
| React SPA PKCE | `react-spa-pkce` | `none` |
| Device flow | `8ee3a98e-89a8-438d-a314-1ef9df815279` | `none` |

样例 host 建议绑定：

```text
127.0.0.1 armorauth-demo
127.0.0.1 armorauth-server
```

不要混用 `armorauth-demo` 和 `127.0.0.1`，否则 OAuth2 `state` 对应的 session cookie 可能丢失。

## 当前注意事项

- `armorauth-spring-boot-starter` 已保留接入入口，后续仍可继续打磨 starter DX、示例和发布流程。
- `local` profile 使用 H2，只适合本地调试；默认生产式路径应使用 MySQL。
- 默认管理员 `admin / admin123` 只适合本地开发，生产必须修改。
- 内置第三方 OAuth provider 默认值仅用于开发示例，真实环境应使用环境变量覆盖。
- JWK 私钥已持久化且加密保存，数据库备份必须包含 `jwk_key` 表。
- SAML IdP 模式、SLO 编排、脚本沙箱 Actions、外部 OpenFGA adapter 和企业级 attestation trust store 已作为后续增强项记录，不阻塞当前 100% 产品范围。

## 推荐阅读顺序

1. [docs/ROADMAP.md](docs/ROADMAP.md)
2. [docs/IMPLEMENTATION_STATUS.md](docs/IMPLEMENTATION_STATUS.md)
3. [docs/quick-start.md](docs/quick-start.md)
4. [docs/api-reference.md](docs/api-reference.md)
5. [docs/deployment-guide.md](docs/deployment-guide.md)
6. [armorauth-core/src/main/java/com/armorauth/config/AuthorizationServerConfig.java](armorauth-core/src/main/java/com/armorauth/config/AuthorizationServerConfig.java)
7. [armorauth-core/src/main/java/com/armorauth/config/DefaultSecurityConfig.java](armorauth-core/src/main/java/com/armorauth/config/DefaultSecurityConfig.java)
8. [armorauth-admin/src/main/java/com/armorauth/admin/config/AdminSecurityConfig.java](armorauth-admin/src/main/java/com/armorauth/admin/config/AdminSecurityConfig.java)
9. [armorauth-server/src/main/resources/application.yml](armorauth-server/src/main/resources/application.yml)
10. [armorauth-server/src/main/resources/db](armorauth-server/src/main/resources/db)

## License

Apache License 2.0
