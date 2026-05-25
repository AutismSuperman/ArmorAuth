<p align="center">
  <img src="./armorauth-admin-ui/public/brand/logo.svg" alt="ArmorAuth logo" width="720">
</p>

# ArmorAuth

[English](README.md)

ArmorAuth 是一个基于 Spring Security 与 Spring Authorization Server 构建的开源身份认证与授权平台。它提供可私有化部署的授权服务器、管理 API、管理控制台，以及登录、授权确认、MFA、设备激活、联合身份确认等托管身份页面。

项目面向希望掌控身份基础设施的产品团队和平台工程团队：保留 OAuth 2.0 / OpenID Connect 兼容性，同时提供清晰的 Java/Spring 代码结构，便于审计、扩展和二次开发。

## 核心特性

- 基于 Spring Authorization Server 的 OAuth 2.0 与 OpenID Connect 授权服务器。
- 托管身份页面：登录、授权确认、MFA、设备激活、激活结果、联合身份确认。
- 管理 API 与 Vue 管理控制台：应用、用户、角色、权限、组织、身份源、登录策略、审计、Webhook、Token 统计、JWK 元数据。
- JWK 持久化存储，身份源密钥、Webhook 密钥、TOTP 数据和签名私钥支持加密保存。
- 本地账号、验证码登录、MFA、TOTP、Passkey/WebAuthn、无密码登录。
- 支持 OAuth2/OIDC、SAML SP、LDAP/AD bind 登录和外部账号绑定。
- SCIM 2.0 用户/组同步、Webhook 投递、Java SPI Actions、授权检查 API。
- 基于 Flyway 的数据库迁移，便于持续部署和版本演进。

## 模块结构

| 模块 | 说明 |
| --- | --- |
| `armorauth-common` | 通用响应、异常、校验、审计上下文等基础能力 |
| `armorauth-model` | JPA 实体与 Repository |
| `armorauth-core` | 授权服务器、认证、MFA、JWK、密钥保护和持久化适配 |
| `armorauth-federation` | 联合登录编排、账号确认、Provider SPI |
| `armorauth-federation-providers` | 内置第三方 Provider 集成与元数据 |
| `armorauth-admin` | 管理 REST API |
| `armorauth-admin-ui` | Vue 3 管理控制台 |
| `armorauth-server-ui` | 托管身份页面模板与静态资源 |
| `armorauth-server` | 可独立运行的 Spring Boot 服务端 |
| `armorauth-spring-boot-autoconfigure` | 面向接入方 Spring Boot 服务的轻量自动配置 |
| `armorauth-spring-boot-starter` | Resource Server 与 OIDC Login 接入 Starter |
| `armorauth-samples` | OAuth/OIDC 客户端接入样例 |

## 对外能力

- 托管身份页面：登录、授权确认、MFA、设备激活、激活结果、联合身份确认。
- 管理控制台：应用管理、用户管理、身份源、登录策略、联合绑定、监控与运营视图。
- 管理 API：`/api/admin/v1` 下的管理接口。
- 账户 API：`/api/account/v1` 下的自助因子管理接口。
- 标准协议端点：OAuth 2.0、OpenID Connect discovery、JWKS、token、introspection、revocation、logout、device authorization。

## 文档

| 文档 | 说明 |
| --- | --- |
| [快速开始](docs/quick-start.zh-CN.md) | 本地构建和运行 ArmorAuth |
| [操作手册](docs/operation-manual.zh-CN.md) | 日常管理与运营流程 |
| [部署指南](docs/deployment-guide.zh-CN.md) | 生产部署、反向代理、数据库、备份与安全配置 |
| [API Reference](docs/api-reference.md) | 管理 API 与账户 API |
| [Spring Boot Starter](docs/spring-boot-starter.md) | Spring Boot 资源服务器与 OIDC Login 接入 |
| [联合登录配置](docs/federation-config.md) | OAuth2/OIDC、SAML、LDAP 身份源配置 |
| [MFA 配置](docs/mfa-config.md) | MFA、TOTP、Passkey 与应用策略 |
| [安全最佳实践](docs/security-best-practices.md) | 安全加固与检查清单 |
| [路线图](docs/ROADMAP.md) | 产品方向与阶段规划 |
| [项目状态](docs/IMPLEMENTATION_STATUS.zh-CN.md) | 当前能力状态与后续方向 |

## 环境要求

- JDK 21+
- Maven 3.9+
- MySQL 8.0+，用于共享环境或接近生产的部署
- Node.js 18+，用于开发管理控制台

## 从源码构建

```bash
mvn -pl armorauth-server -am package -DskipTests
```

服务端产物位于：

```text
armorauth-server/target/armorauth-server-0.0.1.jar
```

本地启动流程见 [快速开始](docs/quick-start.zh-CN.md)，生产部署见 [部署指南](docs/deployment-guide.zh-CN.md)。

## 安全说明

ArmorAuth 会在数据库中保存签名密钥和敏感集成密钥。生产环境必须配置稳定的加密密钥、稳定的 issuer URL、HTTPS Cookie、受控的管理员访问，以及包含 JWK 表和密钥材料的备份方案。

默认开发凭据和种子数据只适合本地开发，不应进入共享或生产环境。

## 项目状态

ArmorAuth 正在持续迭代。当前代码库已经包含私有化部署评估所需的身份认证、管理、联合登录、MFA、Webhook、SCIM 和授权检查能力。后续重点是安全加固、发布打包、SDK、Starter 体验、企业级联合身份能力和部署自动化。

## License

Apache License 2.0
