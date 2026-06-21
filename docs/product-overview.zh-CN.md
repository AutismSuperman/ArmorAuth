# ArmorAuth 产品概览

ArmorAuth 是一个自托管身份认证与授权平台，适合需要私有化部署、标准 OAuth2/OIDC 协议兼容，以及自主掌控用户、租户、应用和安全数据的团队。

项目基于 Spring Security 与 Spring Authorization Server 构建，把授权服务器、托管身份页面、管理 API、管理控制台、Spring Boot Starter 和可运行样例放在同一个代码库中。

## 适用场景

ArmorAuth 适合这些场景：

- 内部平台需要统一 OAuth2/OIDC 授权服务器。
- SaaS 产品需要租户感知的身份与应用管理。
- 企业系统需要同时支持本地用户、OAuth2/OIDC、SAML、LDAP/AD 以及社交或企业身份源。
- 团队希望使用可审查、可扩展、可私有化部署的 Spring 技术栈。

## 核心能力

| 范围 | ArmorAuth 提供的能力 |
| --- | --- |
| 授权服务器 | Authorization Code、Client Credentials、Refresh Token、Device Authorization、discovery、JWKS、introspection、revocation、OIDC logout |
| 托管身份页面 | 登录、授权确认、MFA 挑战、设备激活、激活结果、账号中心、联合身份确认 |
| 账号安全 | 密码登录、图形验证码、短信验证码登录、TOTP MFA、Passkey/WebAuthn、恢复码相关流程、联系方式验证、会话可见性 |
| 管理能力 | 应用、Scope、用户、角色、权限、租户、组织、身份源、会话、审计、Webhook、Token 统计、JWK 元数据 |
| 联合身份 | OAuth2/OIDC、SAML SP 登录、LDAP/AD bind/search 登录、内置 Provider 元数据和账号绑定 |
| 接入集成 | SCIM 2.0 用户/组、授权检查 API、Webhook、Java Action SPI、Spring Boot Starter |
| 运维能力 | JWK 持久化、敏感密钥加密、Flyway 迁移、MySQL 生产配置、H2 本地配置和部署指南 |

## 系统入口

ArmorAuth 主要有三个使用入口：

- 服务端端口上的托管身份页面，通常是 `9000`，用于登录、授权确认、账号安全和授权流程。
- `/api/admin/v1` 下的管理 REST API，用于平台运营和自动化。
- Vue 管理控制台，用于操作员管理应用、用户、租户、身份源和安全策略。

业务应用以 OAuth2/OIDC Client 或 Resource Server 的方式接入 ArmorAuth。Spring Boot 应用可以使用 `armorauth-spring-boot-starter` 完成资源服务器、OIDC Login、当前用户上下文、Admin API 调用和 Token Relay。

## 数据模型概览

- 租户用于归集应用、用户、组织和 issuer 相关行为。
- 组织表示租户内的层级结构或业务单元。
- 应用对应 OAuth2/OIDC Registered Client。
- 用户可以拥有本地凭据、已验证联系方式、角色、组织成员关系、MFA 因子、Passkey 和联合账号绑定。
- 角色与权限用于表达管理授权和业务授权。
- 身份源用于描述外部 OAuth2/OIDC、SAML、LDAP 或内置 Provider 集成。

## 推荐评估路径

1. 按 [快速开始](quick-start.zh-CN.md) 在本地启动系统。
2. 阅读 [基础使用](basic-usage.zh-CN.md)，创建租户、应用和测试用户。
3. 按 [Spring Boot Starter](spring-boot-starter.md) 接入一个 Spring Boot 业务服务。
4. 进入共享或生产环境前阅读 [安全最佳实践](security-best-practices.md)。
5. 按 [部署指南](deployment-guide.zh-CN.md) 规划生产部署。
