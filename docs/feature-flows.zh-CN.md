# ArmorAuth 功能链路总览

本文按“业务目标 -> 管理入口 -> 服务端 API -> 运行时链路 -> 注意事项”的方式梳理 ArmorAuth 的主要功能。它不是接口逐项参考；接口字段和请求示例详见 [API Reference](api-reference.md)。

## 功能入口地图

| 功能域 | 管理控制台入口 | 主要 API | 关键运行时模块 |
| --- | --- | --- | --- |
| 租户 | 用户与组织 / 租户管理 | `/api/admin/v1/tenants` | `armorauth-admin`、租户感知 issuer |
| 组织 | 用户与组织 / 组织管理 | `/api/admin/v1/organizations`、`/api/admin/v1/tenants/{tenantId}/organizations` | `armorauth-admin`、Token claims |
| 应用 | 应用管理 / 应用列表 | `/api/admin/v1/applications` | Spring Authorization Server registered client |
| Scope | 应用管理 / Scope 管理 | `/api/admin/v1/scopes` | OAuth2/OIDC authorization |
| 用户 | 用户与组织 / OAuth 用户 | `/api/admin/v1/users`、`/api/account/v1/me` | 本地用户、SCIM、联合登录 |
| 角色与权限 | 用户、角色、权限相关 API | `/api/admin/v1/roles`、`/api/admin/v1/permissions`、`/api/admin/v1/authorization/check` | RBAC、授权检查 Actions |
| 登录页 | 托管身份页 | `/login`、`/login/captcha`、`/login/passkey/*` | `armorauth-server-ui`、`armorauth-federation`、`armorauth-core` |
| MFA / Passkey | 账号中心、安全策略 | `/api/account/v1/factors`、`/api/account/v1/security`、`/login/mfa` | TOTP、WebAuthn、登录策略 |
| 身份源 | 身份源管理 | `/api/admin/v1/identity-providers`、`/federated/confirm` | OAuth2/OIDC、SAML、LDAP/AD |
| SCIM | 外部系统调用 | `/scim/v2/Users`、`/scim/v2/Groups` | 用户与组同步 |
| Webhook | Webhook 管理 | `/api/admin/v1/webhooks` | 事件投递、签名校验 |
| 审计与会话 | 监控与审计 | `/api/admin/v1/audit-events`、`/api/admin/v1/sessions`、`/api/admin/v1/token-statistics` | 登录、Token、管理操作追踪 |
| JWK 与 Secret | 安全 / JWK 密钥、Secret 保护 | `/api/admin/v1/jwk-keys`、`/api/admin/v1/secret-protection/rekey` | 签名密钥、敏感数据加密 |

## 1. 首次环境链路

目标：把一个空环境配置到可以登录、发 Token、审计和接入样例应用。

1. 启动服务端和管理控制台，确认 `/.well-known/openid-configuration`、`/oauth2/jwks` 和 `/api/admin/v1/me` 可访问。
2. 用管理员账号进入管理控制台，先检查默认租户、默认 Scope、角色和 JWK。
3. 创建或确认租户。租户编码会影响 `/t/{tenantCode}` 路径和租户感知 issuer，不建议频繁修改。
4. 创建应用。浏览器或移动端应用优先使用 Authorization Code + PKCE；服务间调用使用 Client Credentials。
5. 创建用户，补全手机号和邮箱；如果这些联系方式将用于登录、恢复或 MFA，应先完成验证。
6. 按需创建组织并加入成员。组织应服务于授权、报表或管理边界，不建议为了通讯录展示而过度建模。
7. 为管理员角色和敏感应用启用 MFA。
8. 使用样例工程完成登录或授权流程，确认 Token 中的 `tenant_id`、`roles`、`scope`、`permissions`、`org_ids` 等声明符合预期。
9. 查看会话、审计日志和 Token 统计，确认操作和登录事件可追踪。

输出物：稳定 issuer、租户、至少一个测试应用、至少一个测试用户、已验证联系方式、可用 MFA、可追踪审计记录。

## 2. 租户与组织链路

目标：把应用、用户、组织和路径感知能力归入清晰边界。

### 操作顺序

1. 在租户管理中创建租户，确定 `tenantCode`、名称、状态、品牌色、Logo、隐私协议和服务条款。
2. 在应用创建时选择租户。应用的 endpoint 详情会根据租户配置展示 discovery、authorization、token、JWKS、userinfo、logout 等 URL。
3. 如果业务权限、报表或客户侧层级需要组织，再创建组织和子组织。
4. 将用户加入组织，并设置成员角色，例如 `MEMBER`、`OWNER`、`ADMIN`。
5. 在业务服务中读取 Token 或当前用户上下文中的租户和组织声明，做数据隔离或授权判断。

### 数据与声明

| 对象 | 典型字段 | 影响 |
| --- | --- | --- |
| 租户 | `tenantCode`、`tenantName`、`customDomain`、`enabled` | issuer、应用归属、登录品牌、管理过滤 |
| 组织 | `tenantId`、`orgCode`、`parentId` | 成员层级、组织过滤、组织角色 |
| 组织成员 | `userId`、`orgRole` | Token 组织声明、业务授权 |

### 注意事项

- 租户编码进入 URL 后应保持稳定。
- 组织不是租户的替代品；租户用于隔离边界，组织用于层级和成员关系。
- 删除租户或组织前先检查应用、用户、组织成员和审计要求。

## 3. 应用接入链路

目标：把业务系统注册为 OAuth2/OIDC Client，并在管理控制台中获得可调用端点。

### 操作顺序

1. 创建应用，选择租户。
2. 选择授权类型：
   - Web 后端：Authorization Code + Client Secret。
   - SPA / 移动端：Authorization Code + PKCE，公共客户端不保存 secret。
   - 服务间调用：Client Credentials。
   - 设备输入场景：Device Authorization。
3. 配置回调地址和登出回调地址，只允许可信域名。
4. 选择认证方式：`client_secret_basic`、`client_secret_post`、`private_key_jwt` 或公共客户端。
5. 配置 Scope，最小化授权范围。
6. 对敏感应用开启应用级 MFA。
7. 通过端点详情复制 discovery、authorization、token、JWKS、introspection、revocation、userinfo、logout URL。
8. 接入应用完成登录后，查看 Token 统计和审计记录。

### 运行时链路

```text
用户访问业务应用
  -> 业务应用跳转 /oauth2/authorize
  -> ArmorAuth 登录页完成认证和 MFA
  -> 如需同意，进入 /consent
  -> 回调 redirect_uri 返回 code
  -> 业务应用调用 /oauth2/token
  -> 业务应用验证 ID Token / Access Token 或访问 UserInfo
```

### 注意事项

- Client Secret 只在创建或轮换时返回明文，应该立即交给应用负责人保存。
- PKCE 不是“可选安全增强”，浏览器和移动端默认应开启。
- 需要用户授权确认时，确保 Scope 文案可读。
- 禁用应用比删除应用更适合应急止血和事后恢复。

## 4. 用户、角色与权限链路

目标：建立本地账号、管理授权和业务授权基础。

### 用户来源

| 来源 | 入口 | 说明 |
| --- | --- | --- |
| 管理员手动创建 | `/api/admin/v1/users` | 适合少量测试用户或内部账号 |
| 用户自助更新 | `/api/account/v1/me` | 登录后的账号中心维护资料和联系方式 |
| 联合登录 | `/federated/confirm` | 由外部身份源创建或绑定本地账号 |
| SCIM | `/scim/v2/Users` | 外部身份系统同步用户生命周期 |
| LDAP/AD 同步 | `/api/admin/v1/identity-providers/{id}:sync-users` | 预演或执行目录同步 |

### 授权模型

1. 创建角色，如 `APPLICATION_ADMIN`、`USER_ADMIN`、`AUDIT_VIEWER`。
2. 创建权限，如 `application:write`、`user:read`。
3. 将权限绑定到角色。
4. 将角色绑定到用户，尽量避免直接给用户绑定大量权限。
5. 使用 `/api/admin/v1/authorization/check` 做集中授权检查，也可以通过 Java Action 在 `AUTHORIZATION_CHECK` 阶段覆盖或补充判定。

### 注意事项

- 需要保留审计历史时，优先禁用或锁定用户，而不是删除。
- 角色应围绕职责设计，不应围绕个人设计。
- 高权限用户必须启用 MFA，并纳入定期复核。

## 5. 登录、MFA 与账号中心链路

目标：让用户安全登录，并能在登录后维护账号安全。

### 登录方式

| 登录方式 | 入口 | 说明 |
| --- | --- | --- |
| 密码登录 | `GET/POST /login` | 标准表单登录 |
| 图形验证码 | `/login/captcha/image`、`/login/captcha` | 本地和演示环境可展示 mock code |
| 短信验证码 | `/login/captcha/send`、`/login/captcha` | 需要实际短信服务时替换发送实现 |
| Passkey | `/login/passkey/options`、`/login/passkey/finish` | 无密码登录 |
| 联合登录 | `/oauth2/authorization/{registrationId}` 或 `/saml2/authorization/{registrationId}` | 跳转外部身份源 |

### MFA 触发条件

ArmorAuth 会在密码、联合登录或 Passkey 初步认证后检查 MFA 策略：

- 用户主动启用登录 MFA。
- 用户拥有要求 MFA 的角色，例如管理员角色。
- 当前访问的应用要求 MFA。
- 登录策略要求额外验证。

需要 MFA 时进入 `/login/mfa`。用户可以使用已激活的 TOTP 因子或 Passkey 断言完成挑战。

### 账号中心链路

1. 登录成功后进入账号中心。
2. 更新显示名称、邮箱、手机号和备注。
3. 对邮箱或手机号发送验证码并完成验证。开发/演示环境会返回 mock code，便于无短信/邮件网关时测试。
4. 初始化 TOTP，使用二维码或 URI 加入 Authenticator app，再输入 6 位验证码激活。
5. 初始化 Passkey 注册，浏览器完成 WebAuthn attestation 后保存凭据。
6. 根据需要开启或关闭用户自己的登录 MFA 偏好。
7. 删除不再使用的 MFA 因子或 Passkey。

### 注意事项

- 联系方式未验证前，不应作为找回、OTP 登录或强安全策略依据。
- 强制 MFA 前必须准备恢复流程，否则用户设备丢失会造成无法登录。
- Passkey 依赖浏览器、RP ID、Origin 和 HTTPS。生产环境必须使用稳定域名。

## 6. 身份源与联合登录链路

目标：接入企业 SSO、SAML、LDAP/AD 或社交账号，并安全地映射到本地用户。

### OIDC / OAuth2 身份源

1. 创建身份源，填写 `registrationId`、`clientId`、`clientSecret`、authorization/token/userinfo/JWK 端点和 Scope。
2. 配置属性映射，将 `sub`、`email`、`name`、组织或角色映射到本地用户字段。
3. 选择绑定策略：
   - `AUTO_REGISTER`：首次登录自动创建用户。
   - `CONFIRM`：跳转确认页，用户选择创建或绑定。
   - `EMAIL_MATCH`：按邮箱匹配已有账号。
   - `NONE`：只允许已有绑定用户登录。
4. 测试配置。
5. 启用身份源，并按需打开登录页展示。
6. 用户从登录页选择身份源，完成外部登录后进入绑定或自动注册流程。

### SAML 身份源

1. 配置 IdP metadata URL，或手动填写 entityId、SSO URL、证书、NameID 格式。
2. 确认 SP entityId、ACS URL 和 metadata URL。
3. 通过 `/saml2/authorization/{registrationId}` 发起 SP-initiated 登录。
4. IdP 回调 `/login/saml2/sso/{registrationId}` 后复用联合账号绑定流程。

### LDAP / AD 身份源

1. 配置 LDAP URL、Base DN、Bind DN、Bind Password、用户搜索 base 和过滤器。
2. 配置用户名、邮箱、手机号、显示名和组属性。
3. 使用 `:test` 探测配置。
4. 使用 `:sync-users` 先 dry-run，再执行同步。
5. 登录时 ArmorAuth 通过服务账号或匿名 bind 搜索用户 DN，再使用用户密码 bind 验证。

### 注意事项

- 身份源密钥和 LDAP bind password 是受保护 secret，不应明文出现在源码或前端。
- 外部账号解绑不应自动删除本地用户。
- 如果外部身份源的 subject 规则变化，需要复核绑定关系。

## 7. SCIM 同步链路

目标：让外部身份系统同步 ArmorAuth 用户和组。

1. 为 SCIM 客户端准备专用管理员凭据，限制为 `SUPER_ADMIN` 或 `USER_ADMIN`。
2. 外部系统读取 `/scim/v2/ServiceProviderConfig`、`/Schemas`、`/ResourceTypes`。
3. 创建、查询、替换或 Patch `/scim/v2/Users`。
4. 创建、查询、替换或 Patch `/scim/v2/Groups`。组映射到系统角色或 SCIM 管理组。
5. 停用用户时同步处理本地 session、授权和审计。

注意：SCIM 响应不使用 `ApiResponse` 包装，媒体类型为 `application/scim+json`。

## 8. Webhook 与事件链路

目标：把关键身份事件投递到外部审计、通知或自动化系统。

1. 创建 Webhook endpoint，填写名称、HTTPS URL、secret 和事件类型。
2. 服务端在事件发生后写入投递记录并调用目标 URL。
3. 接收方使用 secret 校验签名，确认事件来源。
4. 管理员查看投递记录、响应状态和失败次数。
5. 接收方变更负责人或 secret 泄露时轮换 secret。

建议只订阅需要的事件，避免把高敏身份数据发送到无关系统。

## 9. JWK、Secret 与安全运维链路

目标：保证 Token 签名和敏感配置可轮换、可备份、可审计。

### JWK

1. 服务启动后生成或读取 active JWK。
2. `/oauth2/jwks` 对外发布公钥。
3. 管理员通过 JWK 页面查看 key metadata。
4. 轮换时生成新 active key，旧 key 保留到依赖方刷新 JWKS 和旧 Token 过期。
5. 只在确认不再需要时 retire 或删除旧 key。

### Secret 保护

受保护对象包括身份源 client secret、Webhook secret、MFA secret 和 JWK private key。

1. 生产环境设置稳定 crypto secret。
2. 轮换加密 key 时先配置旧 key 和新 key。
3. 调用 `/api/admin/v1/secret-protection/rekey` 先 dry-run。
4. 确认统计后执行 rekey。
5. 所有密文迁移完成前不要移除旧 key。

## 10. 审计、会话与监控链路

目标：让登录、Token、配置变更和安全事件可追踪。

| 视图 | 说明 | 常见用途 |
| --- | --- | --- |
| 会话管理 | 当前活跃 session | 强制下线、排查异常登录 |
| 审计日志 | 管理操作和安全事件 | 追踪高风险变更 |
| Token 统计 | Token 签发与刷新统计 | 识别异常 client 或流量 |
| Webhook 投递 | 外部事件投递记录 | 排查集成失败 |

建议为共享和生产环境建立固定巡检：

- 每日查看登录失败、MFA 失败和异常 Token 峰值。
- 每周复核高权限账号、应用 secret、身份源配置和 Webhook 状态。
- 每次发布后验证 discovery、JWKS、登录、Token、管理 API、SCIM 和样例应用。

## 11. Spring Boot 接入链路

目标：业务系统不直接依赖 ArmorAuth 内部模块，而是通过标准协议或 starter 接入。

1. 在 ArmorAuth 中创建应用并取得 issuer、client id、secret、Scope 和回调地址。
2. 业务服务引入 `armorauth-spring-boot-starter`。
3. API 服务启用 Resource Server，指向 `issuer-uri` 或 `jwk-set-uri`。
4. Web/BFF 服务启用 OIDC Login，并配置 Spring Security OAuth2 Client。
5. 使用 `ArmorAuthCurrentUserResolver` 读取当前用户、租户、组织、角色、权限和 Scope。
6. 只有调用可信下游服务时才挂载 `ArmorAuthTokenRelayInterceptor`。
7. 需要管理自动化时启用 Admin Client，并使用专用凭据或 bearer token。

详细配置见 [Spring Boot Starter](spring-boot-starter.md)。
