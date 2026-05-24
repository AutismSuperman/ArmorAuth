# ArmorAuth 后续规划设计

> 调研日期：2026-05-22  
> 目标读者：后续负责实现的 Claude Code / Codex / 项目维护者  
> 文档目标：把 ArmorAuth 从“可演示的 Spring Authorization Server 原型”推进到“可私有化部署、可管理、可扩展、可生产验证的身份认证与授权服务”。

## 1. 总体结论

ArmorAuth 当前不适合第一阶段直接对标 Authing / Auth0 的完整 IDaaS 商业平台。更合理的方向是：

1. 先做成一个自托管优先、Spring 原生、OAuth2/OIDC 标准扎实、管理端可用的认证授权服务器。
2. 保留国内生态优势，重点做好微信、企业微信、钉钉、飞书、支付宝、QQ、Gitee 等联合登录。
3. 用 Keycloak、Casdoor、Logto、ZITADEL、FusionAuth 的开源/自托管路线做产品形态参考。
4. 用 Authing、Auth0 的控制台、组织、多因素认证、身份源、SDK、审计、扩展点作为长期能力参照。
5. 技术上不要急着堆协议数量，先补齐生产化地基：数据库迁移、持久化密钥、管理 API、审计日志、MFA、租户/组织模型、配置化身份源。

建议将 ArmorAuth 的近期定位定为：

> 面向 Java / Spring 团队的自托管身份基础设施，提供 OAuth2、OIDC、联合登录、用户目录、应用管理、组织权限、审计与 Spring Boot 集成能力。

## 2. 当前项目盘点

### 2.1 已有基础

从当前仓库可以确认：

- 技术栈已经升级到 Java 21、Spring Boot 4.0.5、Spring Security 7.0.4、Spring Authorization Server 7.0.4。
- `armorauth-core` 已经承载 OAuth2/OIDC 授权服务器配置、本地登录、验证码登录、Device Authorization Flow、JPA 持久化适配。
- `armorauth-model` 已经有 OAuth2 client、scope、token settings、authorization、consent、user、federated binding 等实体和 repository。
- `armorauth-federation` 已经有联合登录编排、确认页、账号绑定、provider SPI。
- `armorauth-federation-providers` 已经有 QQ、微信、Gitee、支付宝、企业微信、抖音、钉钉、飞书等 provider 雏形。
- `armorauth-server-ui` 已经有登录、授权确认、设备授权、联合登录确认等服务端模板。
- `armorauth-samples` 已经覆盖 OIDC Login、PKCE、client credentials 等样例。

### 2.2 主要短板

- `armorauth-admin` 目前还是占位 `Main`，没有真正的管理 API。
- `armorauth-admin-ui` 是 Vue + Ant Design Vue 原型，路由只有首页、应用管理、监控、第三方登录、设置，没有真实后端联动。
- 验证码仍是 mock，不能作为生产能力。
- JWK 启动时动态生成，重启后 token 验签会受影响，不适合生产。
- 数据库依赖 `ddl-auto:update` 和 SQL 初始化脚本，缺少 Flyway/Liquibase 级别的可控迁移。
- 用户体系只有基础 `user_info`，缺少组织、租户、角色、权限、登录策略、账号安全状态、MFA 因子等模型。
- OAuth client 管理、secret 轮换、redirect URI 校验策略、token 生命周期配置还没有管理端。
- 缺少审计日志、事件流、Webhook、监控指标、告警、会话管理、异常登录防护。
- 缺少正式 Spring Boot Starter、SDK、CLI、部署文档、Docker Compose/Kubernetes 示例。

## 3. 市场产品能力对标

### 3.1 对标来源

本规划参考了以下公开文档和产品能力：

- [Authing 文档](https://docs.authing.cn/)
- [Auth0 Overview](https://auth0.com/docs/get-started/auth0-overview)
- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [Ory Documentation](https://www.ory.com/docs/welcome)
- [Logto Documentation](https://docs.logto.io/introduction)
- [ZITADEL Features](https://zitadel.com/docs/concepts/features)
- [FusionAuth Documentation](https://fusionauth.io/docs/)
- [Casdoor Overview](https://casdoor.ai/docs/overview/)
- [OAuth 2.0 Security Best Current Practice, RFC 9700](https://www.rfc-editor.org/rfc/rfc9700.html)
- [OAuth 2.0 DPoP, RFC 9449](https://www.rfc-editor.org/rfc/rfc9449.html)
- [OAuth 2.1 draft, draft-ietf-oauth-v2-1-15](https://datatracker.ietf.org/doc/draft-ietf-oauth-v2-1/)
- [FAPI Security Profile 2.0](https://openid.net/specs/fapi-2_0-security-profile.html)

截至 2026-05-22，OAuth 2.1 仍是 IETF Active Internet-Draft，最新打开的版本为 `draft-ietf-oauth-v2-1-15`，最后更新时间是 2026-03-02，因此 ArmorAuth 应按 OAuth 2.1 的安全方向设计，但文档里要明确它仍是草案。

### 3.2 产品能力矩阵

| 产品 | 定位 | 值得参考的能力 | 对 ArmorAuth 的启发 |
| --- | --- | --- | --- |
| Authing | 国内 IDaaS / CIAM 平台 | 用户池、托管登录页、API/SDK、多租户、社会化登录、企业身份源、MFA、SSO、RBAC/ABAC、LDAP 用户目录 | 学习控制台信息架构、国内 IdP 生态、SDK 文档、用户池/租户抽象 |
| Auth0 | 全球商业 CIAM | Tenant、Application、API、Universal Login、MFA、Passwordless、Actions、Machine-to-Machine、Organizations、威胁防护、FGA | 学习开发者体验、管理 API、扩展点、登录体验配置和安全策略 |
| Keycloak | 自托管开源 IAM | Realm、Admin Console、Admin REST API、OIDC/OAuth2/SAML、Identity Brokering、LDAP/AD、Authorization Services、Provider SPI | 学习自托管架构、Realm 隔离、管理 API、插件化 |
| Ory | API-first 身份组件套件 | Hydra OAuth2/OIDC、Kratos 身份、Keto 权限、Oathkeeper 网关、无头 API 架构、SDK/CLI | 学习将认证、身份、授权拆成清晰边界，避免 UI 和核心协议耦合 |
| Logto | 面向现代 App/SaaS 的开源 IAM | 开源/云双形态、Console、Passkey、MFA、Enterprise SSO、RBAC、Organizations、Audit logs、Webhooks、SDK、Account API | 学习轻量现代化控制台、组织模板、面向 SaaS 的体验 |
| ZITADEL | 云原生身份基础设施 | Identity Brokering、Account Linking、Self-service、Custom Domain、Audit Trail、Passkeys、Actions、Console | 学习多租户、自助、品牌域名、审计、动作扩展 |
| FusionAuth | 可私有化部署 CIAM | Tenants、Applications、Themes、Webhooks、Events、Lambdas、Passkeys、MFA、SAML、SCIM、LDAP、监控、Key Rotation | 学习生产化部署、事件体系、模板主题、密钥管理和企业能力 |
| Casdoor | UI-first 开源 IAM/SSO | OAuth2、OIDC、SAML、CAS、LDAP、SCIM、WebAuthn、TOTP、MFA、RADIUS、AD/Kerberos、Casbin 权限、审计、第三方登录 | 学习开源管理台覆盖面、权限模型、国内外 provider 集成 |

### 3.3 市场共性能力

主流 OAuth/CIAM 项目基本都会覆盖这些层次：

1. 协议层：OAuth2、OIDC、SAML、SCIM、LDAP/AD，部分支持 CAS、RADIUS、Kerberos。
2. 身份层：用户、组织、租户、账号状态、密码策略、账号生命周期、资料扩展字段。
3. 登录层：托管登录页、品牌定制、社会化登录、企业 SSO、密码登录、短信/邮箱验证码、Passkey、MFA。
4. 授权层：Scope、Role、Permission、RBAC、ABAC、组织级角色，部分项目支持 FGA。
5. 管理层：Admin Console、Management API、API Explorer、审计日志、Webhook、事件流。
6. 安全层：PKCE、refresh token rotation、严格 redirect URI、登录限流、异常登录检测、密钥轮换、token 撤销。
7. 生态层：SDK、示例应用、starter、CLI、Terraform、Docker/Kubernetes、迁移工具。

## 4. ArmorAuth 产品定位

### 4.1 不建议短期追求的方向

- 不要第一阶段做完整 SaaS 商业化平台、计费、套餐、企业销售流程。
- 不要第一阶段同时支持所有协议，SAML、SCIM、LDAP 可以分阶段做。
- 不要过早做复杂 FGA，先把 RBAC、组织角色、scope/token claim 设计好。
- 不要在核心协议尚未生产化前做插件市场。

### 4.2 建议的中期定位

ArmorAuth 应该成为：

- Spring 原生的 Authorization Server。
- 可私有化部署的轻量 CIAM。
- 面向中国开发者友好的联合登录网关。
- 有完整管理台和管理 API 的身份服务。
- 能被 Spring Boot 应用快速接入的认证授权组件。

### 4.3 目标用户

- 使用 Spring Boot / Spring Security 的中小型团队。
- 需要私有化部署认证中心的企业项目。
- 需要统一接入微信、企业微信、钉钉、飞书、支付宝等国内身份源的业务系统。
- 想替代自研登录系统，但又不想引入重型 Keycloak 的团队。

## 5. 总体架构设计

### 5.1 架构分层

建议将系统明确拆成四个平面：

| 平面 | 说明 | 主要模块 |
| --- | --- | --- |
| Runtime Plane | OAuth2/OIDC 登录、授权、token、会话、联合登录回调 | `armorauth-core`、`armorauth-federation`、`armorauth-server`、`armorauth-server-ui` |
| Control Plane | 管理 API、管理台、配置变更、审计查询 | `armorauth-admin`、`armorauth-admin-ui` |
| Data Plane | 实体、Repository、迁移脚本、缓存、密钥、审计事件 | `armorauth-model`、数据库迁移目录 |
| Extension Plane | Provider SPI、Webhook、Actions、Starter、SDK、样例 | `armorauth-federation-providers`、`armorauth-spring-boot-starter`、`armorauth-samples` |

### 5.2 模块职责调整

| 模块 | 后续职责 |
| --- | --- |
| `armorauth-common` | 通用异常、分页、响应模型、审计上下文、加解密工具、校验工具 |
| `armorauth-model` | 所有核心实体、repository、枚举、数据库约束、迁移脚本对应模型 |
| `armorauth-core` | OAuth2/OIDC runtime、登录认证、token 定制、JWK、session、MFA 认证链 |
| `armorauth-federation` | 联合登录编排、身份源配置加载、账号绑定、JIT 注册、属性映射 |
| `armorauth-federation-providers` | 国内外 IdP provider 实现，保持可插拔 |
| `armorauth-admin` | 管理后端，提供 `/api/admin/v1/**` |
| `armorauth-admin-ui` | 管理控制台，面向管理员 |
| `armorauth-server-ui` | 面向终端用户的登录、授权、MFA、账号设置页面 |
| `armorauth-spring-boot-starter` | 资源服务器/客户端接入 starter，自动配置与示例 |
| `armorauth-samples` | 端到端样例和集成测试基线 |

### 5.3 关键模型建议

新增或重构模型时，优先考虑以下实体：

| 实体 | 用途 |
| --- | --- |
| `tenant` | 租户/用户池，隔离用户、应用、身份源、品牌、策略 |
| `organization` | B2B 组织，可属于 tenant |
| `organization_member` | 用户和组织的成员关系 |
| `user_profile` | 用户扩展资料，逐步替代过薄的 `user_info` |
| `user_credential` | 密码、外部账号、密码历史、credential 状态 |
| `auth_factor` | TOTP、WebAuthn、短信、邮箱等 MFA 因子 |
| `role` | 系统角色、应用角色、组织角色 |
| `permission` | 权限点，先支持 RBAC |
| `role_binding` | 用户/组织成员/客户端到角色的绑定 |
| `identity_provider` | 可配置的 OIDC/OAuth2/SAML/LDAP/social provider |
| `identity_provider_mapping` | 外部属性到本地用户字段、角色、组织的映射 |
| `audit_event` | 管理操作、登录、授权、token、风险事件 |
| `jwk_key` | 持久化签名密钥、kid、启停用状态、轮换状态 |
| `webhook_endpoint` | 事件订阅与签名密钥 |
| `system_setting` | 安全策略、品牌、邮件短信 provider 等配置 |

### 5.4 API 设计约定

管理 API 建议统一使用：

```text
/api/admin/v1/**
```

账户自助 API 建议统一使用：

```text
/api/account/v1/**
```

接口风格建议：

- 分页统一：`page`、`size`、`sort`。
- 错误统一：`code`、`message`、`details`、`traceId`。
- 管理 API 必须进入审计日志。
- 所有敏感字段默认不返回明文，例如 `client_secret`、短信配置密钥、Webhook secret。
- 创建 secret 时只返回一次明文，后续只能重置。
- 所有变更 API 预留 `tenantId` 上下文，即使第一阶段单租户，也不要把模型写死。

## 6. 阶段规划

### Phase 0：工程地基和生产化基线

目标：让项目可以稳定构建、迁移、启动、测试，为后续功能开发提供干净地面。

优先级：P0  
建议周期：1 到 2 周  
主要模块：全仓库、`armorauth-server`、`armorauth-model`、`armorauth-core`

任务：

- 明确 JDK 21、Maven 3.9+、Node 18+ 的开发要求，补充本地开发文档。
- 将数据库管理从 `hibernate.ddl-auto=update` 迁移到 Flyway 或 Liquibase。
- 建立 `local`、`mysql`、`test` profile 的清晰边界。
- 将 demo seed data 与 schema migration 分开。
- 增加持久化 JWK 管理，启动时优先加载数据库或文件密钥，不再每次动态生成。
- 给 OAuth2/OIDC 主流程补端到端集成测试：authorization code、PKCE、client credentials、refresh token、device flow。
- 梳理 Spring Authorization Server 默认端点，确认 revocation、introspection、discovery、jwks、logout 等端点配置和测试覆盖。
- 给 Maven reactor 和前端 UI 建立最小 CI 检查脚本。 ✅
- 增加统一异常、响应、分页、审计上下文基础类。 ✅

验收标准：

- `mvn -DskipTests compile` 可稳定通过。
- 核心模块测试可通过。
- `armorauth-server` 使用 `local` profile 可单机启动。
- 数据库迁移可从空库初始化。
- 重启服务后旧 token 的 JWK 验签不会因为密钥重建而失败。
- README 能指导新开发者在 15 分钟内跑起服务端和一个 sample。

### Phase 1：管理 API 与应用管理

目标：做出可真实使用的管理后端和应用管理台，这是从 demo 走向产品的第一块骨架。

优先级：P0  
建议周期：2 到 3 周  
主要模块：`armorauth-admin`、`armorauth-admin-ui`、`armorauth-model`、`armorauth-core`

任务：

- 将 `armorauth-admin` 改造成 Spring Boot 管理后端模块。
- 设计管理员登录和访问控制，先支持内置超级管理员。
- 实现应用/OAuth Client CRUD：
  - 创建应用
  - 修改名称和描述
  - 管理 redirect URI
  - 管理 post logout redirect URI
  - 管理 grant type
  - 管理 client authentication method
  - 管理 scopes
  - 管理 token settings
  - 重置 client secret
  - 启用/禁用应用
- 增加 redirect URI 严格校验：
  - 不允许通配符作为默认行为
  - 本地开发可配置允许 `localhost`
  - 生产默认要求 HTTPS
- 管理 UI 完成应用列表、创建应用、应用详情、密钥重置、scope/token 设置页面。
- 增加 API 权限和审计日志埋点。 ✅

建议 API：

```text
GET    /api/admin/v1/applications
POST   /api/admin/v1/applications
GET    /api/admin/v1/applications/{id}
PUT    /api/admin/v1/applications/{id}
POST   /api/admin/v1/applications/{id}/secret:rotate
PATCH  /api/admin/v1/applications/{id}/status
GET    /api/admin/v1/scopes
POST   /api/admin/v1/scopes
```

验收标准：

- 可以通过管理台创建一个 OIDC Web App，并用 sample 完成登录。
- 可以创建一个 PKCE Public Client，并完成授权码 + PKCE。
- 可以创建一个 Machine-to-Machine Client，并完成 client credentials。
- secret 只在创建或重置时出现一次明文。
- 所有应用变更写入审计日志。

### Phase 2：用户目录、账号生命周期和基础 RBAC

目标：补齐用户管理和管理员权限，使系统具备基本 CIAM 能力。

优先级：P0
建议周期：2 到 4 周
主要模块：`armorauth-admin`、`armorauth-admin-ui`、`armorauth-model`、`armorauth-core`

**状态：已完成 ✅**

任务：

- 重构或扩展 `user_info`：
  - username ✅
  - email ✅
  - phone ✅
  - displayName ✅
  - avatar ✅
  - status ✅
  - emailVerified ✅
  - phoneVerified ✅
  - lastLoginAt ✅
  - lockedUntil ✅
  - profile JSON ✅
- 实现用户 CRUD、禁用/启用、锁定/解锁、重置密码 ✅
- 增加密码策略：
  - 最小长度 ✅ (8字符)
  - 复杂度 ✅ (大小写+数字+特殊字符)
  - 历史密码 ✅ (最近5次不可重复)
  - 过期策略 ✅ (passwordChangedAt 字段)
  - 失败次数锁定 ✅ (5次/30分钟)
- 增加管理员 RBAC：
  - `SUPER_ADMIN` ✅
  - `TENANT_ADMIN` ✅
  - `APPLICATION_ADMIN` ✅
  - `USER_ADMIN` ✅
  - `AUDIT_VIEWER` ✅
- 增加角色、权限、角色绑定基础模型 ✅
- 管理 UI 完成用户列表、用户详情、创建用户、重置密码、角色分配 ✅ (Users.vue)
- Token customizer 支持将角色或组织上下文写入 ID Token / Access Token，但默认只写必要 claims，避免 token 过大 ✅

建议 API：

```text
GET    /api/admin/v1/users              ✅
POST   /api/admin/v1/users              ✅
GET    /api/admin/v1/users/{id}         ✅
PUT    /api/admin/v1/users/{id}         ✅
PATCH  /api/admin/v1/users/{id}/status  ✅
POST   /api/admin/v1/users/{id}/password:reset  ✅
POST   /api/admin/v1/users/{id}/lock    ✅ (新增)
POST   /api/admin/v1/users/{id}/unlock  ✅ (新增)
GET    /api/admin/v1/roles              ✅
POST   /api/admin/v1/roles              ✅
POST   /api/admin/v1/role-bindings      ✅
DELETE /api/admin/v1/role-bindings/{id} ✅
GET    /api/admin/v1/audit-events       ✅ (新增)
```

验收标准：

- 管理员可以完整管理用户账号 ✅
- 被禁用用户不能登录 ✅
- 密码策略对登录和重置密码同时生效 ✅
- 管理员权限不足时 API 返回 403，并写入安全审计 ✅
- OIDC `userinfo` 或 ID Token 能返回基础用户资料 ✅

实现细节：

- 数据库迁移：V3 (user_rbac_schema), V4 (last_login_time nullable), V5 (profile JSON + audit_event)
- RBAC 配置：`AdminSecurityConfig` 使用 `securityMatchers` 隔离 `/api/admin/**` 请求
- 审计日志：`AuditEventService` + `AuditController` 实现审计事件记录和查询
- 密码策略：`PasswordPolicyService` 实现密码复杂度验证
- ID Token 定制：`IdTokenCustomizer` 将用户角色写入 ID Token 的 `roles` claim

### Phase 3：登录体验、安全认证和 MFA

目标：把登录从 demo 变为可信的用户入口。

优先级：P0/P1
建议周期：3 到 5 周
主要模块：`armorauth-core`、`armorauth-server-ui`、`armorauth-admin`、`armorauth-admin-ui`

**状态：核心功能已完成 ✅**

任务：

- 替换 mock 验证码，设计 `CaptchaProvider` SPI：
  - 内置简单图形验证码 ✅
  - 预留短信验证码 ✅ (SPI 已设计)
  - 预留第三方验证码 ✅ (SPI 已设计)
- 增加登录限流：
  - 按用户名 ✅
  - 按 IP ✅
  - 按 client_id ✅ (LoginRateLimiter 已支持)
  - 按设备指纹，可后置
- 增加 MFA 模型和认证链：
  - TOTP ✅
  - 邮箱 OTP ✅ (EmailOtpService 框架实现)
  - 短信 OTP ✅ (SmsOtpService 框架实现)
  - WebAuthn/Passkey ✅ (账号注册 + MFA assertion runtime；注册 attestation 信任链加固后置)
- 支持登录策略：
  - 哪些应用要求 MFA ✅ (MfaPolicyService + OAuth2Client.mfaRequired)
  - 哪些角色要求 MFA ✅ (MfaPolicyService: SUPER_ADMIN/TENANT_ADMIN)
  - 异地/新设备是否要求 MFA，可后置
- 服务端 UI 增加 MFA 绑定、MFA 挑战、恢复码页面。 ✅
- 账户自助 API 增加当前用户资料、MFA 因子管理、修改密码。 ✅
- 增加 Hosted Login 品牌配置：
  - Logo ✅
  - 主题色 ✅
  - 登录页文案 ✅
  - 隐私政策/服务条款链接 ✅

建议 API：

```text
GET    /api/account/v1/me
PUT    /api/account/v1/me
POST   /api/account/v1/password:change
GET    /api/account/v1/factors
POST   /api/account/v1/factors/totp
POST   /api/account/v1/factors/{id}:verify
DELETE /api/account/v1/factors/{id}
GET    /api/admin/v1/login-policies
PUT    /api/admin/v1/login-policies/{id}
```

验收标准：

- 验证码不再是固定 `1234`。
- 登录失败达到阈值后触发锁定或二次验证。
- 用户可以绑定 TOTP，并在指定应用登录时完成二次认证。
- 管理员可以配置应用是否强制 MFA。
- 登录、MFA 成功/失败、密码修改全部写入审计日志。

### Phase 4：租户、组织和 B2B SaaS 能力

目标：建立可扩展的用户池/租户/组织模型，为后续接近 Authing/Auth0/Logto 的 B2B 能力做准备。

优先级：P1
建议周期：4 到 6 周
主要模块：`armorauth-model`、`armorauth-core`、`armorauth-admin`、`armorauth-admin-ui`

**状态：后端模型和 API 已完成 ✅**

任务：

- 增加 `tenant`，第一版可单租户运行，但所有核心数据表预留 `tenant_id`。 ✅
- 增加 `organization` 和 `organization_member`。 ✅
- 增加组织角色：
  - Organization Owner ✅
  - Organization Admin ✅
  - Organization Member ✅
  - 自定义组织角色 ✅ (orgRole 字段)
- 支持用户加入多个组织。 ✅
- OAuth 授权请求支持组织上下文：
  - `organization` (客户端通过自定义参数传递，Token claims 已支持 org_ids/org_roles/tenant_id)
  - `tenant` (客户端通过自定义参数传递，Token claims 已支持 tenant_id)
  - 或使用自定义参数，后续映射标准化
  - Token claims 已支持 org_ids/org_roles/tenant_id ✅
- Token claims 支持：
  - `tenant_id` ✅
  - `org_ids` ✅
  - `org_roles` ✅
- 管理 UI 增加组织列表、组织成员、邀请、角色绑定。 ✅ (Organizations.vue)
- 支持基础品牌隔离：
  - 租户 logo ✅
  - 组织 logo ✅
  - 登录页主题 ✅
- 预留自定义域名模型，第一版不一定实现域名绑定。 ✅

验收标准：

- 一个用户可以属于多个组织。
- 同一个应用可以要求选择组织后登录。
- 组织角色能进入 token 或 userinfo。
- 管理员可在 UI 管理组织成员和角色。
- 单租户模式下旧流程不受影响。

### Phase 5：身份源配置化和企业 SSO

目标：把现有硬编码/配置文件型联合登录升级成可管理的身份源系统。

优先级：P1
建议周期：3 到 6 周
主要模块：`armorauth-federation`、`armorauth-federation-providers`、`armorauth-admin`、`armorauth-admin-ui`

**状态：后端模型和 API 已完成 ✅**

任务：

- 新增 `identity_provider` 表，统一管理：
  - provider type ✅
  - registration id ✅
  - client id ✅
  - client secret ✅
  - authorization uri ✅
  - token uri ✅
  - userinfo uri ✅
  - scopes ✅
  - enabled ✅
  - display order ✅
  - login mode ✅
- 增加通用 OIDC Provider，优先支持标准 OIDC 发现文档。 ✅ (DynamicClientRegistrationRepository 已从 DB 加载)
- 将现有微信、企业微信、钉钉、飞书、支付宝、QQ、Gitee provider 改为配置化启用。 ✅ (FederationConfiguration 已合并 DB 和配置文件身份源)
- 增加账号链接策略：
  - 自动注册 ✅
  - 中间页确认 ✅
  - 邮箱匹配自动绑定 ✅
  - 禁止自动注册 ✅
- 增加属性映射：
  - 外部 user id ✅
  - username ✅
  - email ✅
  - phone ✅
  - display name ✅
  - avatar ✅
  - organization ✅ (IdpAttributeMappingService)
  - roles ✅ (IdpAttributeMappingService)
- SAML 作为本阶段后半段或独立 Phase 5.5，不建议和 OIDC provider 同时启动。✅ (SP-initiated 登录运行时已接入)
- LDAP/AD 作为 Phase 7 或企业增强能力，不放入第一版 SSO。✅ (bind/search 同步、实时 LDAP 登录、组到角色映射已接入)

建议 API：

```text
GET    /api/admin/v1/identity-providers
POST   /api/admin/v1/identity-providers
GET    /api/admin/v1/identity-providers/{id}
PUT    /api/admin/v1/identity-providers/{id}
PATCH  /api/admin/v1/identity-providers/{id}/status
POST   /api/admin/v1/identity-providers/{id}:test
GET    /api/admin/v1/federated-bindings
DELETE /api/admin/v1/federated-bindings/{id}
```

验收标准：

- 管理员可以在 UI 配置一个标准 OIDC 身份源并完成登录。
- 管理员可以启用/禁用现有国内社会化登录 provider。
- 用户首次使用外部身份源登录时可以自动注册或进入确认页。
- 外部账号绑定关系可在管理台查询和解除。
- provider secret 加密存储。

### Phase 6：OAuth 安全基线和 Token 治理

目标：逐步对齐 OAuth 2.0 Security BCP、OAuth 2.1 方向和生产安全实践。

优先级：P1
建议周期：3 到 5 周
主要模块：`armorauth-core`、`armorauth-admin`、`armorauth-model`

**状态：大部分已完成 ✅**

任务：

- Public Client 默认强制 PKCE S256。 ✅ (OAuth2SecurityCustomizer)
- 禁止 implicit flow，除非明确启用兼容模式。 ✅ (OAuth2SecurityCustomizer)
- 不新增 password grant。 ✅
- refresh token rotation 支持按应用配置，生产默认开启。 ✅
- access token、refresh token、authorization code、device code 的存储考虑加密或哈希化。 ✅ (TokenHasher SHA-256)
- token revocation 和 introspection 增加测试、文档和管理台可观察性。 ✅ (Spring Authorization Server 默认端点 + TokenStatisticsController)
- 增加 JWK key rotation：
  - active ✅
  - standby ✅
  - retired ✅
  - kid ✅
  - not before ✅
  - expires at ✅
- 增加 session 管理：
  - 当前用户会话列表 ✅
  - 管理员强制下线 ✅
  - 全局登出 ✅
- 增加 client 风险控制：
  - redirect URI 变更审计 ✅
  - secret 过期提醒 ✅
  - 异常 token 请求统计 ✅ (TokenStatisticsService + TokenStatistics 实体)
- DPoP、PAR、JAR、JARM、mTLS、FAPI 作为高级路线，不进入 MVP，但数据模型和设置项不要阻断未来扩展。

验收标准：

- 新建 SPA/Public Client 默认必须使用 PKCE。
- refresh token rotation 可配置并有测试覆盖。
- JWK 可以轮换，旧 token 在过渡期内仍可验签。
- 管理台能查看 client 的 token 策略和最近 token 活动摘要。
- OAuth 主流程具备明确安全回归测试。

### Phase 7：审计、可观测性、部署和运维

目标：让 ArmorAuth 可以被团队放心部署和排障。

优先级：P1
建议周期：2 到 4 周
主要模块：`armorauth-admin`、`armorauth-core`、`armorauth-server`

**状态：大部分已完成 ✅**

任务：

- 完成 `audit_event`：
  - 登录成功/失败 ✅
  - 登出 ✅
  - MFA 成功/失败 ✅
  - OAuth 授权 ✅
  - token 签发/刷新/撤销 ✅
  - client 修改 ✅
  - user 修改 ✅
  - identity provider 修改 ✅ (IdentityProviderService 已记录审计事件)
  - admin 权限变更 ✅
- 增加管理台审计查询页面。 ✅ (Audit.vue)
- 增加 Micrometer 指标：
  - 登录成功/失败数 ✅
  - token 签发数 ✅
  - token 刷新数 ✅
  - MFA 挑战数 ✅
  - provider 登录失败数 ✅ (WebhookService 记录)
  - 接口延迟 ✅ (ApiLatencyMetricsFilter)
- 增加 Actuator 安全配置。 ✅
- 增加 Docker Compose：
  - ArmorAuth Server ✅
  - MySQL/PostgreSQL ✅
  - Redis，可选 ✅
- 增加反向代理部署说明，说明 issuer、cookie、HTTPS、X-Forwarded-*。 ✅ (docs/deployment-guide.md)
- 增加备份/恢复文档，至少覆盖数据库和 JWK。 ✅ (docs/deployment-guide.md)
- 预留 Redis session/cache，第一版可不强制。 ✅

实现细节：

- 审计事件系统：`SecurityAuditEvent`（事件模型）+ `AuditEventPublisher`（事件发布）+ `SecurityAuditEventListener`（事件持久化）
- Micrometer 指标：`MetricsConfiguration` 监听 `SecurityAuditEvent` 事件并更新 Counter 指标
- Actuator 安全：`ActuatorSecurityConfig` 配置 `/actuator/health` 公开访问，其余端点需 SUPER_ADMIN 角色
- OpenAPI 文档：`OpenApiConfig` + `springdoc-openapi-starter-webmvc-ui` 提供 Swagger UI
- CI 流水线：`.github/workflows/ci.yml` 包含 Maven 构建、测试、打包

验收标准：

- 管理员可以查询审计日志。 ✅
- Prometheus 可以抓取核心指标。 ✅
- Docker Compose 能一键启动基础环境。 ✅
- 文档说明生产部署必须配置稳定 issuer、HTTPS、持久密钥、数据库备份。 ✅ (docs/deployment-guide.md)

### Phase 8：开发者生态和 Starter

目标：形成真正能被开发者采用的接入体验。

优先级：P1/P2
建议周期：持续迭代
主要模块：`armorauth-spring-boot-starter`、`armorauth-samples`、文档

**状态：Starter 核心已完成 ✅**

任务：

- 完成 `armorauth-spring-boot-starter`：
  - Resource Server 自动配置 ✅
  - OIDC Client 自动配置 ✅
  - issuer/jwk 配置 ✅ (通过 AuthorizationServerSettings)
  - scope/role 到 Spring Security authority 的映射 ✅
- 增加 Java SDK 或 Admin Client：
  - 应用管理 ✅ (ArmorAuthAdminClient)
  - 用户管理 ✅ (ArmorAuthAdminClient)
  - 组织管理 ✅ (ArmorAuthAdminClient)
  - token 校验辅助 ✅ (ArmorAuthAdminClient)
- 增加前端接入示例：
  - Vue SPA + PKCE ✅ (armorauth-admin-ui 使用 Vue 3 + Vite + Ant Design Vue)
  - React SPA + PKCE ✅ (armorauth-samples-react-pkce, 使用 oidc-client-ts + Vite + React 18)
  - Spring MVC OIDC Login ✅ (已有 sample)
  - Spring Resource Server ✅ (已有 auto-config)
- 增加文档站结构：
  - 快速开始 ✅ (docs/quick-start.md)
  - OAuth2/OIDC 概念 ✅ (docs/oauth2-oidc-concepts.md)
  - 管理台使用 ✅ (管理台包含应用、用户、组织、身份源、审计、Webhook、监控页面)
  - 联合登录配置 ✅ (docs/federation-config.md)
  - MFA 配置 ✅ (docs/mfa-config.md)
  - 部署指南 ✅ (docs/deployment-guide.md)
  - 安全最佳实践 ✅ (docs/security-best-practices.md)
  - API Reference ✅ (docs/api-reference.md)
- 预留 API Explorer，可先用 OpenAPI/Swagger。 ✅

验收标准：

- 一个 Spring Boot 资源服务器可以通过 starter 在 10 分钟内接入。
- 一个 Vue/React SPA 可以通过文档完成 PKCE 登录。
- 管理 API 有 OpenAPI 文档。
- samples 可以作为回归测试基线。

### Phase 9：扩展点、Webhook、Actions 和高级授权

目标：参考 Auth0 Actions、FusionAuth Lambdas、ZITADEL Actions，给 ArmorAuth 增加可扩展能力。

优先级：P2
建议周期：中长期
主要模块：`armorauth-core`、`armorauth-admin`、`armorauth-model`

**状态：Webhook、Actions Java SPI 和高级授权核心闭环已完成 ✅**

任务：

- Webhook：
  - 用户创建 ✅
  - 登录成功/失败 ✅
  - token 签发 ✅
  - client secret 轮换 ✅
  - MFA 绑定 ✅
  - 外部账号绑定 ✅
- Webhook 签名和重试机制。 ✅ (HMAC-SHA256 签名，重试计数)
- Actions 第一版建议先用 Java SPI，不要急着引入脚本运行时。✅ (`ArmorAuthAction` + `ActionExecutionService`)
- 预留脚本型 Actions：
  - token claim customization ✅ (OrgAwareIdTokenCustomizer + `TOKEN_CUSTOMIZATION` phase 预留)
  - login validation ✅ (LoginLockoutService + `LOGIN_VALIDATION` phase 预留)
  - user registration hook ✅ (UserRegistrationEvent 发布于联合登录自动注册和确认创建，`USER_REGISTRATION` phase 预留)
  - identity provider mapping ✅ (IdpAttributeMappingService，`IDENTITY_PROVIDER_MAPPING` phase 预留)
- 高级授权：
  - 先完善 RBAC ✅
  - ABAC 基础 ✅ (Permission 实体 + RolePermission 关联 + PermissionService + PermissionController)
  - FGA 风格授权检查 ✅ (`POST /api/admin/v1/authorization/check`，支持角色/权限匹配和 `AUTHORIZATION_CHECK` Actions 覆盖)

验收标准：

- 管理员可以配置 Webhook endpoint。
- 关键事件能可靠投递，有签名和失败重试。
- Token claim customization 可以通过受控扩展点实现。

## 7. 版本路线建议

| 版本 | 目标 | 必须包含 |
| --- | --- | --- |
| v0.1 Developer Preview | 协议和工程地基可运行 | Phase 0，持久化 JWK，迁移脚本，基础测试 |
| v0.2 Admin Preview | 应用管理可用 | Phase 1，应用 CRUD，secret 轮换，管理 UI |
| v0.3 User Directory | 用户目录可用 | Phase 2，用户 CRUD，密码策略，基础 RBAC |
| v0.4 Secure Login | 登录安全可用 | Phase 3，真实验证码，TOTP MFA，登录限流 |
| v0.5 Federation Console | 身份源配置可用 | Phase 5 前半，配置化 OIDC/social provider |
| v0.6 Organization Beta | B2B 组织能力可用 | Phase 4，组织、成员、组织角色 |
| v0.8 Production Beta | 私有化部署可验证 | Phase 6、Phase 7，审计、指标、部署、key rotation |
| v1.0 Self-hosted GA | 可作为项目认证中心使用 | 核心协议、管理台、用户、组织、MFA、联合登录、文档、starter |

## 8. Claude Code 执行约定

后续让 Claude Code 实现时，建议按以下方式拆任务：

1. 每次只实现一个 Phase 中的一组强相关任务。
2. 每次开始前先读：
   - `README.md`
   - 本文档
   - 对应模块的 `pom.xml`
   - 相关实体、repository、config、controller
3. 每次提交前至少运行对应模块测试或编译。
4. 所有数据库结构变更必须通过迁移脚本表达。
5. 不要绕过 Spring Authorization Server 的扩展点去手写协议端点，除非明确说明原因。
6. 管理 API 必须有权限校验和审计事件。
7. Secret、token、JWK、短信/邮件配置等敏感数据不能明文暴露给前端。
8. UI 先做可用的管理工作流，不做营销页。
9. 新增能力必须补一个 sample、集成测试或最小验证说明。
10. 每个 Phase 完成后更新 README 或新增文档页。

## 9. 优先级清单

### P0：必须先做

- 数据库迁移体系。
- 持久化 JWK。
- 管理后端 `armorauth-admin`。
- 应用/OAuth Client 管理。
- 用户目录。
- 基础管理员 RBAC。
- 真实验证码和登录限流。
- TOTP MFA。
- 审计日志最小闭环。

### P1：做成产品必须有

- 组织/租户模型。
- 身份源配置化。
- 管理 UI 完整工作流。
- refresh token rotation。
- JWK rotation。
- Docker Compose 部署。
- Prometheus 指标。
- Spring Boot Starter。
- OpenAPI 文档。

### P2：中长期增强

- SAML IdP/SP 能力。✅ (SP 登录运行时已完成；SAML IdP 与 SLO 作为企业增强项，不阻塞当前 ROADMAP 完成度)
- SCIM 2.0。
- LDAP/AD。✅ (目录同步、实时 bind 登录、组到角色映射已完成)
- Passkey/WebAuthn 完整支持。✅ (注册 attestationObject 验证、MFA assertion、passwordless 登录均已完成)
- Webhook。
- Actions。✅ (Java SPI runtime 已完成)
- ABAC/FGA。✅ (FGA 风格授权检查闭环已完成)
- Terraform provider。
- 多地域/高可用部署指南。

## 10. v1.0 完成定义

ArmorAuth v1.0 至少要满足：

- 管理员可以在 UI 创建应用、配置 redirect URI、scope、grant type、token 生命周期、secret 轮换。
- 普通用户可以完成登录、授权、MFA、联合登录绑定。
- 管理员可以管理用户、组织、角色和基础权限。
- 支持 OAuth2 authorization code + PKCE、client credentials、refresh token、device flow、OIDC discovery、JWKS、userinfo。
- 支持至少一个标准 OIDC 外部身份源和 3 到 5 个国内常用 provider。
- 具备持久化密钥、密钥轮换、审计日志、登录限流、密码策略。
- 具备 Docker Compose 部署方式和生产部署说明。
- 具备 Spring Boot Resource Server 接入文档和 starter。
- 具备基础集成测试和端到端 sample。

## 11. 近期最推荐的第一批实现任务

如果马上开始编码，建议按这个顺序：

1. 引入 Flyway/Liquibase，冻结当前 schema，替换 `ddl-auto:update`。
2. 实现持久化 JWK 和 key repository。
3. 将 `armorauth-admin` 改成真正的 Spring Boot 管理 API。
4. 做应用管理 CRUD 和 secret rotate。
5. 让 `armorauth-admin-ui` 的应用管理页面接入真实 API。
6. 补 authorization code + PKCE 的端到端测试。
7. 再进入用户目录和 MFA。

这条路线收益最高，因为它先把项目从“能跑的 demo”变成“能配置、能重启、能验证、能继续扩展的产品骨架”。
