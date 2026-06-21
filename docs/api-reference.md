# ArmorAuth API Reference

## 概述

管理 API 基础路径：`/api/admin/v1`

账户自助 API 基础路径：`/api/account/v1`

管理 API 使用 HTTP Basic 认证，默认本地管理员为 `admin / admin123`。账户自助 API 使用登录后的同源 session。生产环境必须替换默认管理员密码，并按角色控制访问。本文只记录当前代码中已经实现的 API 表面；OAuth 2.0 / OIDC 标准端点由 Spring Authorization Server 提供。

统一响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

分页响应有两种形态：

```json
{
  "content": [],
  "totalElements": 0,
  "totalPages": 0,
  "page": 0,
  "size": 20
}
```

部分较新的接口直接返回 Spring `Page`，字段包含 `content`、`totalElements`、`totalPages`、`number`、`size` 等。

---

## 应用管理

### 端点

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/admin/v1/applications` | 获取应用列表 |
| GET | `/api/admin/v1/applications/{id}` | 获取应用详情 |
| POST | `/api/admin/v1/applications` | 创建应用 |
| PUT | `/api/admin/v1/applications/{id}` | 更新应用 |
| POST | `/api/admin/v1/applications/{id}/secret:rotate` | 重置客户端密钥 |
| PATCH | `/api/admin/v1/applications/{id}/status` | 启用或禁用应用 |
| DELETE | `/api/admin/v1/applications/{id}` | 删除应用 |

列表查询参数：

| 参数 | 类型 | 必填 | 默认 | 说明 |
| --- | --- | --- | --- | --- |
| page | int | 否 | `0` | 页码 |
| size | int | 否 | `20` | 每页数量 |
| sort | string | 否 | `clientIdIssuedAt` | 排序字段 |
| direction | string | 否 | `DESC` | `ASC` 或 `DESC` |

创建请求：

```json
{
  "clientName": "My App",
  "clientAuthenticationMethods": "client_secret_basic",
  "authorizationGrantTypes": "authorization_code,refresh_token",
  "redirectUris": "http://localhost:8080/callback",
  "postLogoutRedirectUris": "http://localhost:8080/",
  "scopes": ["openid", "profile", "email"],
  "mfaRequired": false,
  "clientSettings": {
    "jwkSetUrl": null,
    "requireAuthorizationConsent": true,
    "requireProofKey": false,
    "signingAlgorithm": "RS256"
  },
  "tokenSettings": {
    "accessTokenTimeToLiveSeconds": 300,
    "refreshTokenTimeToLiveSeconds": 3600,
    "deviceCodeTimeToLiveSeconds": 300,
    "authorizationCodeTimeToLiveSeconds": 300,
    "idTokenSignatureAlgorithm": "RS256",
    "reuseRefreshTokens": false,
    "tokenFormat": "self-contained"
  }
}
```

更新请求字段与创建请求相同，均为可选。创建和密钥重置响应会返回一次性明文 `clientSecret`。

状态请求：

```json
{
  "enabled": true
}
```

也兼容：

```json
{
  "status": "enabled"
}
```

---

## Scope 管理

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/admin/v1/scopes` | 获取 Scope 列表，可按 clientId 过滤 |
| POST | `/api/admin/v1/scopes` | 创建 Scope |
| PUT | `/api/admin/v1/scopes?clientId={clientId}&scope={scope}` | 更新 Scope 描述 |
| DELETE | `/api/admin/v1/scopes?clientId={clientId}&scope={scope}` | 删除 Scope |

列表查询参数：`clientId`、`page`、`size`。

创建请求：

```json
{
  "clientId": "client-id",
  "scope": "message.read",
  "description": "读取消息"
}
```

更新请求：

```json
{
  "description": "新的描述"
}
```

---

## 用户管理

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/admin/v1/users` | 获取用户列表 |
| GET | `/api/admin/v1/users/{id}` | 获取用户详情 |
| POST | `/api/admin/v1/users` | 创建用户 |
| PUT | `/api/admin/v1/users/{id}` | 更新用户资料 |
| PATCH | `/api/admin/v1/users/{id}/status` | 更新用户状态 |
| POST | `/api/admin/v1/users/{id}/lock` | 锁定用户 |
| POST | `/api/admin/v1/users/{id}/unlock` | 解锁用户 |
| POST | `/api/admin/v1/users/{id}/password:reset` | 重置用户密码 |
| DELETE | `/api/admin/v1/users/{id}` | 删除用户 |

列表查询参数：`page`、`size`、`sort`，默认按 `createTime DESC` 排序。

创建请求：

```json
{
  "username": "user1",
  "password": "StrongP@ss123",
  "displayName": "用户1",
  "email": "user1@example.com",
  "phone": "13800138000",
  "avatar": "",
  "profile": "{}"
}
```

更新请求：

```json
{
  "displayName": "用户1",
  "email": "user1@example.com",
  "phone": "13800138000",
  "avatar": "",
  "profile": "{}"
}
```

状态请求：

```json
{
  "status": 0
}
```

锁定请求：

```json
{
  "durationMinutes": 30
}
```

重置密码请求：

```json
{
  "newPassword": "NewStrongP@ss123"
}
```

---

## 角色和权限

### 角色

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/admin/v1/roles` | 获取角色列表 |
| POST | `/api/admin/v1/roles` | 创建角色 |
| DELETE | `/api/admin/v1/roles/{id}` | 删除角色 |
| POST | `/api/admin/v1/role-bindings` | 给用户绑定角色 |
| GET | `/api/admin/v1/role-bindings?userId={userId}` | 获取用户角色绑定 |
| DELETE | `/api/admin/v1/role-bindings/{id}` | 按绑定 ID 删除 |
| DELETE | `/api/admin/v1/role-bindings?userId={userId}&roleId={roleId}` | 按用户和角色删除 |

创建角色请求：

```json
{
  "roleCode": "APP_ADMIN",
  "roleName": "应用管理员",
  "description": "管理应用配置"
}
```

绑定角色请求：

```json
{
  "userId": "user-id",
  "roleId": "role-id"
}
```

### 权限

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/admin/v1/permissions` | 获取权限列表 |
| GET | `/api/admin/v1/permissions/{id}` | 获取权限详情 |
| POST | `/api/admin/v1/permissions` | 创建权限 |
| DELETE | `/api/admin/v1/permissions/{id}` | 删除权限 |
| GET | `/api/admin/v1/roles/{roleId}/permissions` | 获取角色权限 |
| POST | `/api/admin/v1/roles/{roleId}/permissions/{permissionId}` | 给角色分配权限 |
| DELETE | `/api/admin/v1/roles/{roleId}/permissions/{permissionId}` | 移除角色权限 |
| POST | `/api/admin/v1/authorization/check` | 执行 FGA 风格授权检查 |

创建权限请求：

```json
{
  "permissionCode": "custom:action",
  "permissionName": "自定义操作",
  "resourceType": "custom",
  "action": "action",
  "description": "自定义权限描述"
}
```

授权检查请求：

```json
{
  "username": "admin",
  "permissionCode": "custom:action",
  "resourceType": "custom",
  "action": "action",
  "context": {
    "tenantId": "tenant-id"
  }
}
```

响应字段：`allowed`、`reason`、`userId`、`username`、`roles`、`permissions`、`actionAttributes`。默认根据 `SUPER_ADMIN`、角色绑定和角色权限计算；`AUTHORIZATION_CHECK` 阶段的 Java Actions 可以对结果做终止式 allow/deny 覆盖或补充属性。

### Actions Java SPI

扩展点位于 `armorauth-core`：实现 `ArmorAuthAction` 并注册为 Spring Bean 后，`ActionExecutionService` 会按 Spring order 执行支持当前 `ActionPhase` 的 action。当前内置阶段包括 `LOGIN_VALIDATION`、`TOKEN_CUSTOMIZATION`、`USER_REGISTRATION`、`IDENTITY_PROVIDER_MAPPING`、`AUTHORIZATION_CHECK`；授权检查接口已经接入 `AUTHORIZATION_CHECK`。

---

## 租户和组织

### 租户

| 方法 | 路径 | 说明 | 角色 |
| --- | --- | --- | --- |
| GET | `/api/admin/v1/tenants` | 获取租户列表 | `SUPER_ADMIN`、`TENANT_ADMIN` |
| GET | `/api/admin/v1/tenants/{id}` | 获取租户详情 | `SUPER_ADMIN`、`TENANT_ADMIN` |
| POST | `/api/admin/v1/tenants` | 创建租户 | `SUPER_ADMIN` |
| PUT | `/api/admin/v1/tenants/{id}` | 更新租户 | `SUPER_ADMIN` |
| PATCH | `/api/admin/v1/tenants/{id}/status?enabled=true` | 启用或禁用租户 | `SUPER_ADMIN` |
| DELETE | `/api/admin/v1/tenants/{id}` | 删除租户 | `SUPER_ADMIN` |

创建请求：

```json
{
  "tenantCode": "acme",
  "tenantName": "Acme",
  "description": "Acme tenant",
  "logo": "",
  "primaryColor": "#215ae5",
  "loginPageTitle": "Acme Login",
  "privacyPolicyUrl": "https://example.com/privacy",
  "termsOfServiceUrl": "https://example.com/terms"
}
```

更新请求在创建字段基础上增加 `customDomain`，且不包含 `tenantCode`。

### 组织

组织 API 同时支持全局路径和租户路径：

| 方法 | 全局路径 | 租户路径 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/admin/v1/organizations` | `/api/admin/v1/tenants/{tenantId}/organizations` | 获取组织列表 |
| GET | `/api/admin/v1/organizations/{id}` | `/api/admin/v1/tenants/{tenantId}/organizations/{id}` | 获取组织详情 |
| POST | `/api/admin/v1/organizations` | `/api/admin/v1/tenants/{tenantId}/organizations` | 创建组织 |
| PUT | `/api/admin/v1/organizations/{id}` | `/api/admin/v1/tenants/{tenantId}/organizations/{id}` | 更新组织 |
| DELETE | `/api/admin/v1/organizations/{id}` | `/api/admin/v1/tenants/{tenantId}/organizations/{id}` | 删除组织 |
| GET | `/api/admin/v1/organizations/{id}/members` | `/api/admin/v1/tenants/{tenantId}/organizations/{id}/members` | 获取成员 |
| POST | `/api/admin/v1/organizations/{id}/members` | `/api/admin/v1/tenants/{tenantId}/organizations/{id}/members` | 添加成员 |
| DELETE | `/api/admin/v1/organizations/{id}/members/{userId}` | `/api/admin/v1/tenants/{tenantId}/organizations/{id}/members/{userId}` | 移除成员 |

组织创建请求：

```json
{
  "tenantId": "tenant-id",
  "orgCode": "engineering",
  "orgName": "Engineering",
  "description": "",
  "logo": "",
  "parentId": null
}
```

成员请求：

```json
{
  "userId": "user-id",
  "orgRole": "MEMBER"
}
```

---

## 身份源和外部账号绑定

### 身份源

| 方法 | 路径 | 说明 | 角色 |
| --- | --- | --- | --- |
| GET | `/api/admin/v1/identity-providers` | 获取身份源列表 | `SUPER_ADMIN`、`APPLICATION_ADMIN` |
| GET | `/api/admin/v1/identity-providers/{id}` | 获取身份源详情 | `SUPER_ADMIN`、`APPLICATION_ADMIN` |
| POST | `/api/admin/v1/identity-providers` | 创建身份源 | `SUPER_ADMIN` |
| PUT | `/api/admin/v1/identity-providers/{id}` | 更新身份源 | `SUPER_ADMIN` |
| PATCH | `/api/admin/v1/identity-providers/{id}/status?enabled=true` | 启用或禁用身份源 | `SUPER_ADMIN` |
| POST | `/api/admin/v1/identity-providers/{id}:test?probeRemote=false` | 测试身份源配置 | `SUPER_ADMIN`、`APPLICATION_ADMIN` |
| POST | `/api/admin/v1/identity-providers/{id}:sync-users` | LDAP/AD 用户同步或同步预演 | `SUPER_ADMIN`、`USER_ADMIN` |
| DELETE | `/api/admin/v1/identity-providers/{id}` | 删除身份源 | `SUPER_ADMIN` |

创建请求：

```json
{
  "providerName": "企业 OIDC",
  "providerType": "OIDC",
  "registrationId": "enterprise-oidc",
  "clientId": "client-id",
  "clientSecret": "client-secret",
  "authorizationUri": "https://sso.example.com/authorize",
  "tokenUri": "https://sso.example.com/token",
  "userinfoUri": "https://sso.example.com/userinfo",
  "jwkSetUri": "https://sso.example.com/jwks",
  "samlEntityId": null,
  "samlSsoUrl": null,
  "samlSloUrl": null,
  "samlX509Certificate": null,
  "samlMetadataUrl": null,
  "samlSpEntityId": null,
  "samlAcsUrl": null,
  "samlNameIdFormat": null,
  "ldapUrl": null,
  "ldapBaseDn": null,
  "ldapBindDn": null,
  "ldapBindPassword": null,
  "ldapUserSearchBase": null,
  "ldapUserSearchFilter": "(objectClass=person)",
  "ldapUsernameAttribute": "uid",
  "ldapEmailAttribute": "mail",
  "ldapPhoneAttribute": "telephoneNumber",
  "ldapDisplayNameAttribute": "displayName",
  "ldapGroupAttribute": "memberOf",
  "ldapUseSsl": false,
  "ldapStartTls": false,
  "ldapPageSize": 200,
  "scopes": "openid,profile,email",
  "attributeMapping": "{\"username\":\"sub\",\"email\":\"email\"}",
  "linkingStrategy": "AUTO_REGISTER",
  "displayOrder": 1
}
```

更新请求不包含 `providerType` 和 `registrationId`。更新时 `clientSecret` 留空会保留原密钥。

SAML 身份源支持配置模型、管理 API、UI 录入、字段校验和 SP-initiated 登录。SAML 可以使用 `samlMetadataUrl` 方式配置，或手动填写 `samlEntityId`、`samlSsoUrl`、`samlX509Certificate`；`samlSpEntityId`、`samlAcsUrl`、`samlNameIdFormat` 会进入运行时 Relying Party 配置。运行时默认使用 `/saml2/authorization/{registrationId}` 发起 AuthnRequest，使用 `/login/saml2/sso/{registrationId}` 接收断言并复用联合账号绑定/自动注册流程；`runtimeSupport=sp_redirect_post_assertion` 表示该身份源可用于 SAML 登录。

LDAP/AD 身份源当前支持配置模型、管理 API、UI 录入、连接探测、bind/search 用户同步，以及表单登录链路中的实时 LDAP bind 登录。登录时会用服务账号或匿名 bind 搜索用户 DN，再用用户密码 bind；成功后会创建或更新本地用户，并可通过 `attributeMapping.roles` 将 LDAP 组映射到已存在的 ArmorAuth 角色。`ldapBindPassword` 是写入字段，详情响应不会回显明文，只返回 `ldapBindPasswordConfigured`。`providerType` 和 `linkingStrategy` 接受大小写不敏感输入；`linkingStrategy=email` 会映射为 `EMAIL_MATCH`。`runtimeSupport=bind_search_user_sync_login` 表示该 LDAP 身份源已具备同步和登录运行时支持。

LDAP/AD 组到角色映射示例：

```json
{
  "roles": {
    "fromAttribute": "memberOf",
    "mappings": {
      "CN=Armor Admins,OU=Groups,DC=example,DC=com": "SUPER_ADMIN",
      "Armor Users": "USER_ADMIN"
    }
  }
}
```

LDAP/AD 同步请求默认是预演模式：

```json
{
  "dryRun": true,
  "maxResults": 200
}
```

同步响应：

```json
{
  "providerId": "idp-id",
  "providerName": "Corp AD",
  "dryRun": true,
  "scanned": 10,
  "wouldCreate": 3,
  "wouldUpdate": 7,
  "created": 0,
  "updated": 0,
  "skipped": 0,
  "failed": 0,
  "message": "LDAP/AD 用户同步预演完成",
  "samples": {
    "create": ["alice"],
    "update": ["bob"]
  }
}
```

测试响应：

```json
{
  "success": true,
  "message": "配置检查通过",
  "checks": {
    "runtimeSupport": "bind_search_user_sync_login"
  }
}
```

### 外部账号绑定

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/admin/v1/federated-bindings` | 获取外部账号绑定列表 |
| DELETE | `/api/admin/v1/federated-bindings/{id}` | 删除绑定 |

查询参数：`userId`、`registrationId`、`page`、`size`。

---

## 登录策略

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/admin/v1/login-policies` | 获取应用登录策略列表 |
| GET | `/api/admin/v1/login-policies/{id}` | 获取应用登录策略 |
| PUT | `/api/admin/v1/login-policies/{id}` | 更新应用登录策略 |

更新请求：

```json
{
  "mfaRequired": true
}
```

响应字段包含 `roleMfaRequired`，当前内置角色策略包含 `SUPER_ADMIN`、`TENANT_ADMIN`。

---

## JWK 和 Secret 保护

### JWK 密钥

| 方法 | 路径 | 说明 | 角色 |
| --- | --- | --- | --- |
| GET | `/api/admin/v1/jwk-keys` | 获取 JWK 元数据列表 | 管理员 |
| POST | `/api/admin/v1/jwk-keys/rotate` | 轮换 JWK，生成新 active key | `SUPER_ADMIN` |
| POST | `/api/admin/v1/jwk-keys/{kid}/retire` | 废弃 standby key | `SUPER_ADMIN` |

JWK 响应字段：`id`、`kid`、`keyType`、`algorithm`、`status`、`createdAt`、`expiresAt`。

轮换响应：

```json
{
  "kid": "new-kid",
  "message": "密钥轮换成功"
}
```

### Secret 重加密

| 方法 | 路径 | 说明 | 角色 |
| --- | --- | --- | --- |
| POST | `/api/admin/v1/secret-protection/rekey` | 对存量 protected secret 做 dry-run 或执行重加密 | `SUPER_ADMIN` |

默认 dry-run；只有请求明确传 `false` 才会写库。

```json
{
  "dryRun": true
}
```

响应字段：

| 字段 | 说明 |
| --- | --- |
| activeKeyId | 当前写入使用的 key id |
| configuredKeyIds | 当前进程可读取的 key id 列表 |
| dryRun | 是否只统计 |
| identityProviders | 身份源 client secret 统计 |
| webhookEndpoints | Webhook secret 统计 |
| authFactors | MFA secret 统计 |
| jwkKeys | JWK private key 统计 |
| total | 汇总统计 |

统计字段：

| 字段 | 说明 |
| --- | --- |
| scanned | 扫描数量 |
| blank | 空值数量 |
| alreadyActive | 已使用 active key 的数量 |
| plaintext | 仍是明文的数量 |
| differentKey | 使用非 active key 的数量 |
| wouldRekey | dry-run 判断需要重写的数量 |
| rekeyed | 已实际重写的数量 |
| failed | 失败数量 |

---

## 会话、审计和统计

### 会话

| 方法 | 路径 | 说明 | 角色 |
| --- | --- | --- | --- |
| GET | `/api/admin/v1/sessions` | 获取所有活跃会话 | `SUPER_ADMIN`、`AUDIT_VIEWER` |
| GET | `/api/admin/v1/sessions/{username}` | 获取指定用户会话 | `SUPER_ADMIN`、`USER_ADMIN`、`AUDIT_VIEWER` |
| DELETE | `/api/admin/v1/sessions/{sessionId}` | 强制下线会话 | `SUPER_ADMIN` |

会话响应字段：`sessionId`、`username`、`lastRequest`、`active`。

### 审计日志

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/admin/v1/audit-events` | 获取审计事件 |

查询参数：`page`、`size`、`eventType`、`principalName`。

### Token 统计

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/admin/v1/token-statistics?clientId={clientId}&from=2026-05-01&to=2026-05-24` | 获取指定 client 的 Token 统计 |
| GET | `/api/admin/v1/token-statistics/summary?from=2026-05-01&to=2026-05-24` | 获取全局 Token 统计汇总 |

---

## Webhook 管理

| 方法 | 路径 | 说明 | 角色 |
| --- | --- | --- | --- |
| GET | `/api/admin/v1/webhooks` | 获取 Webhook endpoint 列表 | `SUPER_ADMIN` |
| GET | `/api/admin/v1/webhooks/{id}` | 获取 Webhook endpoint 详情 | `SUPER_ADMIN` |
| POST | `/api/admin/v1/webhooks` | 创建 Webhook endpoint | `SUPER_ADMIN` |
| PUT | `/api/admin/v1/webhooks/{id}` | 更新 Webhook endpoint | `SUPER_ADMIN` |
| PATCH | `/api/admin/v1/webhooks/{id}/status?enabled=true` | 启用或禁用 Webhook endpoint | `SUPER_ADMIN` |
| DELETE | `/api/admin/v1/webhooks/{id}` | 删除 Webhook endpoint | `SUPER_ADMIN` |
| GET | `/api/admin/v1/webhooks/{id}/deliveries` | 获取投递记录 | `SUPER_ADMIN` |

创建请求：

```json
{
  "name": "登录事件通知",
  "url": "https://example.com/webhook",
  "secret": "hmac-secret",
  "eventTypes": "login.success,login.failure"
}
```

更新请求字段相同。`secret` 留空时由服务端保留原密钥。

投递响应字段：`id`、`endpointId`、`eventType`、`payload`、`responseStatus`、`success`、`retryCount`、`createdAt`。

---

## 账户自助 API

账户自助 API 面向当前认证用户。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/account/v1/me` | 获取当前用户资料 |
| PUT | `/api/account/v1/me` | 更新当前用户资料 |
| POST | `/api/account/v1/password:change` | 修改当前用户密码 |
| GET | `/api/account/v1/security` | 获取当前用户账号安全状态 |
| PATCH | `/api/account/v1/security/mfa` | 更新当前用户登录 MFA 偏好 |
| GET | `/api/account/v1/factors` | 获取当前用户 MFA 因子 |
| POST | `/api/account/v1/factors/totp` | 初始化 TOTP 因子 |
| POST | `/api/account/v1/factors/passkey:begin-registration` | 初始化 Passkey/WebAuthn 注册 |
| POST | `/api/account/v1/factors/passkey/{id}:finish-registration` | 验证并完成 Passkey/WebAuthn 注册 |
| POST | `/api/account/v1/factors/{id}:verify` | 验证并激活 MFA 因子 |
| DELETE | `/api/account/v1/factors/{id}` | 删除 MFA 因子 |

更新资料请求：

```json
{
  "displayName": "用户1",
  "email": "user1@example.com",
  "phone": "13800138000",
  "avatar": "",
  "profile": "{}"
}
```

修改密码请求：

```json
{
  "oldPassword": "OldStrongP@ss123",
  "newPassword": "NewStrongP@ss123"
}
```

验证因子请求：

```json
{
  "code": "123456"
}
```

TOTP 初始化响应字段：`factorId`、`secret`、`uri`、`recoveryCodes`。

Passkey/WebAuthn 支持账号自助注册参数生成、challenge 持久化、attestationObject 验证、credential 元数据保存、列表、删除、登录 MFA 阶段断言验证，以及 passwordless 登录。完成注册优先提交浏览器返回的 `clientDataJSON` 和 `attestationObject`，服务端校验 `webauthn.create`、challenge、origin/RP ID、RP ID hash、user-present、attested credential data、COSE public key，并支持 `fmt=none` 和 `fmt=packed` attestation。兼容模式仍可提交 `publicKey`，其值支持 base64url DER/SPKI、公钥 PEM、JWK JSON 或 COSE_Key CBOR；返回 `runtimeSupport=passkey_assertion_ready` 时，该凭据可用于登录页 Passkey 验证。

Passkey 开始注册请求：

```json
{
  "name": "MacBook Touch ID",
  "rpId": "auth.example.com",
  "rpName": "ArmorAuth"
}
```

Passkey 开始注册响应字段：`factorId`、`challenge`、`rpId`、`rpName`、`timeoutMillis`、`userHandle`、`username`、`displayName`、`excludeCredentialIds`、`pubKeyCredParams`、`attestation`、`verificationMode`。

Passkey 完成注册请求：

```json
{
  "challenge": "challenge-from-begin",
  "credentialId": "base64url-credential-id",
  "clientDataJSON": "base64url-client-data-json",
  "attestationObject": "base64url-attestation-object",
  "rpId": "auth.example.com",
  "name": "MacBook Touch ID",
  "transports": "internal",
  "userHandle": "base64url-user-handle"
}
```

兼容模式可额外使用 `publicKey`、`signCount`、`aaguid`、`backupEligible`、`backupState` 字段；生产环境建议使用浏览器原生 attestationObject 路径。

Passwordless 登录端点：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/login/passkey/options` | 生成 passwordless Passkey 断言参数，可按 username 限定 allowCredentials |
| POST | `/login/passkey/finish` | 验证 passwordless Passkey 签名并建立登录态 |

Passkey 登录断言开始响应字段：`challenge`、`rpId`、`timeoutMillis`、`userVerification`、`allowCredentials`。该接口要求当前会话已经进入 MFA 挑战态。

Passkey 登录断言完成请求：

```json
{
  "credentialId": "base64url-credential-id",
  "clientDataJSON": "base64url-client-data-json",
  "authenticatorData": "base64url-authenticator-data",
  "signature": "base64url-signature",
  "userHandle": "base64url-user-handle"
}
```

服务端会校验 pending MFA 会话、challenge、origin、RP ID hash、user-present 标记、签名、userHandle 和 signCount 单调递增；验证成功后恢复原登录态并返回 `success`、`redirectUrl`、`factorId`、`runtimeSupport`。

Passwordless 完成登录复用同一请求结构，服务端根据 `credentialId` 反查已验证且启用的 Passkey 因子，并返回 `runtimeSupport=passkey_passwordless_ready`。

---

## SCIM 2.0

SCIM API 基础路径：`/scim/v2`

SCIM API 使用 HTTP Basic 认证，复用管理员账号和 RBAC 权限；当前要求 `SUPER_ADMIN` 或 `USER_ADMIN`。响应不使用 `ApiResponse` 包装，默认媒体类型为 `application/scim+json`。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/scim/v2/ServiceProviderConfig` | 获取 SCIM 服务能力 |
| GET | `/scim/v2/Schemas` | 获取当前支持的 SCIM schema |
| GET | `/scim/v2/ResourceTypes` | 获取当前支持的资源类型 |
| GET | `/scim/v2/Users` | 查询用户列表 |
| POST | `/scim/v2/Users` | 创建用户 |
| GET | `/scim/v2/Users/{id}` | 获取用户 |
| PUT | `/scim/v2/Users/{id}` | 替换用户字段 |
| PATCH | `/scim/v2/Users/{id}` | 按 SCIM PatchOp 更新用户 |
| DELETE | `/scim/v2/Users/{id}` | 删除用户 |
| GET | `/scim/v2/Groups` | 查询组列表，当前映射到系统角色 |
| POST | `/scim/v2/Groups` | 创建 SCIM 管理组 |
| GET | `/scim/v2/Groups/{id}` | 获取组 |
| PUT | `/scim/v2/Groups/{id}` | 替换组名和成员 |
| PATCH | `/scim/v2/Groups/{id}` | 按 SCIM PatchOp 更新组 |
| DELETE | `/scim/v2/Groups/{id}` | 删除 SCIM 管理组 |

用户列表查询参数：

| 参数 | 类型 | 必填 | 默认 | 说明 |
| --- | --- | --- | --- | --- |
| startIndex | int | 否 | `1` | SCIM 1-based 起始位置 |
| count | int | 否 | `20` | 每页数量，最大 `100` |
| filter | string | 否 |  | 当前支持 `userName eq "name"`、`userName co "part"`、`emails.value eq "a@b.com"` |

组列表查询参数同用户列表；`filter` 当前支持 `displayName eq "ROLE_CODE"`、`displayName co "PART"`。

创建用户请求：

```json
{
  "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"],
  "userName": "scim.user@example.com",
  "displayName": "SCIM User",
  "name": {
    "givenName": "SCIM",
    "familyName": "User"
  },
  "active": true,
  "emails": [
    {
      "value": "scim.user@example.com",
      "type": "work",
      "primary": true
    }
  ],
  "phoneNumbers": [
    {
      "value": "13800000000",
      "type": "mobile",
      "primary": true
    }
  ],
  "password": "StrongP@ss123"
}
```

`password` 可省略；服务端会生成符合当前密码策略的随机密码并仅保存哈希，不会在 SCIM 响应中返回。

创建组请求：

```json
{
  "schemas": ["urn:ietf:params:scim:schemas:core:2.0:Group"],
  "displayName": "SCIM_SYNCED_GROUP",
  "members": [
    {
      "value": "user-id",
      "display": "admin"
    }
  ]
}
```

内置角色组通过 SCIM 只读；SCIM 创建的组会落到非内置 `sys_role`，成员关系落到 `user_role`。组成员 PATCH 支持 `add`/`replace`/`remove` 的 `members` 路径，以及 `remove` 的 `members[value eq "user-id"]` 路径。

PATCH 请求示例：

```json
{
  "schemas": ["urn:ietf:params:scim:api:messages:2.0:PatchOp"],
  "Operations": [
    {
      "op": "replace",
      "path": "active",
      "value": false
    },
    {
      "op": "replace",
      "path": "displayName",
      "value": "Disabled SCIM User"
    }
  ]
}
```

用户响应示例：

```json
{
  "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"],
  "id": "user-id",
  "userName": "scim.user@example.com",
  "displayName": "SCIM User",
  "active": true,
  "name": {
    "formatted": "SCIM User",
    "givenName": "SCIM",
    "familyName": "User"
  },
  "emails": [
    {
      "value": "scim.user@example.com",
      "type": "work",
      "primary": true
    }
  ],
  "meta": {
    "resourceType": "User",
    "created": "2026-05-24T03:00:00Z",
    "lastModified": "2026-05-24T03:00:00Z",
    "location": "http://localhost:9000/scim/v2/Users/user-id"
  }
}
```

SCIM 错误响应示例：

```json
{
  "schemas": ["urn:ietf:params:scim:api:messages:2.0:Error"],
  "status": "409",
  "scimType": "uniqueness",
  "detail": "userName already exists: scim.user@example.com"
}
```

---

## OAuth 2.0 / OIDC 标准端点

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/.well-known/openid-configuration` | OIDC Discovery |
| GET | `/oauth2/jwks` | JWKS 公钥集 |
| GET | `/oauth2/authorize` | Authorization Endpoint |
| POST | `/oauth2/token` | Token Endpoint |
| POST | `/oauth2/introspect` | Token Introspection |
| POST | `/oauth2/revoke` | Token Revocation |
| GET | `/userinfo` | OIDC UserInfo |
| POST | `/oauth2/device_authorization` | Device Authorization |
| GET/POST | `/oauth2/device_verification` | Device Verification |

辅助页面：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/activate` | Device flow 用户激活入口，可带 `user_code` |
| GET | `/activated` | Device flow 激活完成页 |
| GET | `/consent` | 授权确认页 |

---

## 登录和联合登录页面

这些端点返回 HTML 或用于登录页交互；Passkey assertion 接口使用 `ApiResponse` 包装。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/` | 当前登录用户首页，匿名用户重定向到 `/login` |
| GET | `/login` | 登录页，支持 `mode`、`error`、`logout` 查询参数 |
| POST | `/login` | Spring Security 表单登录 |
| GET | `/login/mfa` | MFA 挑战页 |
| POST | `/login/passkey/assertion/options` | MFA 阶段初始化 Passkey/WebAuthn 断言 |
| POST | `/login/passkey/assertion/finish` | MFA 阶段验证 Passkey/WebAuthn 断言并恢复登录态 |
| GET | `/saml2/authorization/{registrationId}` | 记录联合登录模式并发起 SAML SP 登录 |
| GET | `/saml2/authenticate/{registrationId}` | SAML AuthnRequest 生成和跳转入口 |
| POST | `/login/saml2/sso/{registrationId}` | SAML Assertion Consumer Service 回调 |
| GET | `/saml2/service-provider-metadata/{registrationId}` | SAML SP metadata |
| POST | `/login/captcha/send` | 演示短信/图形验证码提示 |
| GET | `/login/captcha` | 重定向到登录页 |
| POST | `/login/captcha` | 验证码登录，表单字段为 `account`、`captcha`、`captchaId` |
| GET | `/login/captcha/image` | 获取 PNG 图形验证码，响应头含 `X-Captcha-Id` |
| GET | `/login/captcha/info` | 获取图形验证码元信息 |
| GET | `/federated/confirm` | 联合登录确认页 |
| POST | `/federated/confirm/create` | 联合登录后创建本地账号 |
| POST | `/federated/confirm/bind` | 联合登录后绑定已有本地账号 |

---

## 错误响应

API 错误响应格式：

```json
{
  "code": 400,
  "message": "错误信息",
  "data": null
}
```

常见状态：

| HTTP 状态 | 说明 |
| --- | --- |
| 400 | 请求参数错误 |
| 401 | 未认证 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 409 | 资源冲突 |
| 500 | 服务器内部错误 |
