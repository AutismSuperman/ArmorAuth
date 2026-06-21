# 联合登录与身份源配置指南

本文说明 ArmorAuth 的外部身份源接入链路，包括 OIDC/OAuth2、SAML、LDAP/AD、联合账号绑定和登录页展示。接口逐项参考见 [API Reference](api-reference.md)。

## 1. 身份源模型

ArmorAuth 支持两类配置来源：

| 来源 | 说明 | 适用场景 |
| --- | --- | --- |
| 配置文件 | 通过 Spring Security OAuth2 Client 配置静态注册 | 本地固定身份源、不可由控制台修改的基础集成 |
| 数据库 | 通过管理控制台或 `/api/admin/v1/identity-providers` 动态维护 | 推荐方式，支持启停、排序、登录页展示、测试和同步 |

数据库身份源可以受 Secret 保护能力加密保存 client secret、LDAP bind password 等敏感字段。

## 2. OIDC / OAuth2 身份源

创建 OIDC 身份源：

```bash
curl -X POST http://localhost:9000/api/admin/v1/identity-providers \
  -H "Content-Type: application/json" \
  -u admin:admin123 \
  -d '{
    "providerName":"企业 SSO",
    "providerType":"OIDC",
    "registrationId":"enterprise-oidc",
    "clientId":"your-client-id",
    "clientSecret":"your-client-secret",
    "authorizationUri":"https://sso.example.com/authorize",
    "tokenUri":"https://sso.example.com/token",
    "userinfoUri":"https://sso.example.com/userinfo",
    "jwkSetUri":"https://sso.example.com/jwks",
    "scopes":"openid,profile,email",
    "attributeMapping":"{\"username\":\"sub\",\"email\":\"email\"}",
    "linkingStrategy":"AUTO_REGISTER",
    "displayOrder":10
  }'
```

登录链路：

```text
登录页展示身份源
  -> 用户点击按钮
  -> /oauth2/authorization/{registrationId}
  -> 外部 IdP 完成登录
  -> /login/oauth2/code/{registrationId}
  -> 查找或创建本地用户绑定
  -> 继续原 OAuth2/OIDC 授权流程
```

## 3. SAML 身份源

SAML 可以使用 metadata URL，也可以手工填写 entityId、SSO URL、证书和 NameID 格式。

```json
{
  "providerName": "企业 SAML",
  "providerType": "SAML",
  "registrationId": "enterprise-saml",
  "samlMetadataUrl": "https://idp.example.com/metadata",
  "samlSpEntityId": "https://auth.example.com/saml2/service-provider-metadata/enterprise-saml",
  "samlAcsUrl": "https://auth.example.com/login/saml2/sso/enterprise-saml",
  "linkingStrategy": "CONFIRM",
  "displayOrder": 20
}
```

SAML 运行时入口：

| 路径 | 说明 |
| --- | --- |
| `/saml2/authorization/{registrationId}` | 发起 SP-initiated 登录 |
| `/login/saml2/sso/{registrationId}` | 接收 SAML Assertion |
| `/saml2/service-provider-metadata/{registrationId}` | 输出 SP metadata |

SAML 登录成功后复用联合账号绑定流程。首次登录是否自动创建用户，由 `linkingStrategy` 决定。

## 4. LDAP / AD 身份源

LDAP/AD 主要用于目录认证和用户同步。它不会像 OAuth/SAML 那样天然作为第三方按钮跳转；登录时服务端通过 bind/search 验证用户密码。

创建 LDAP 身份源时需要确认：

- `ldapUrl`、`ldapBaseDn`、`ldapBindDn`、`ldapBindPassword`。
- 用户搜索 base 和过滤器。
- 用户名、邮箱、手机号、显示名和组属性。
- 是否启用 SSL 或 StartTLS。
- 组到 ArmorAuth 角色的映射关系。

同步预演：

```bash
curl -X POST 'http://localhost:9000/api/admin/v1/identity-providers/{id}:sync-users' \
  -H "Content-Type: application/json" \
  -u admin:admin123 \
  -d '{"dryRun":true,"maxResults":200}'
```

确认后执行同步：

```bash
curl -X POST 'http://localhost:9000/api/admin/v1/identity-providers/{id}:sync-users' \
  -H "Content-Type: application/json" \
  -u admin:admin123 \
  -d '{"dryRun":false,"maxResults":200}'
```

## 5. 账号链接策略

| 策略 | 说明 | 适用场景 |
| --- | --- | --- |
| `AUTO_REGISTER` | 首次登录自动创建本地用户 | 内部 SSO 或受信身份源 |
| `CONFIRM` | 首次登录进入确认页，用户选择创建或绑定 | 需要降低误绑定风险 |
| `EMAIL_MATCH` | 按邮箱匹配已有本地用户 | 邮箱可信且唯一 |
| `NONE` | 只允许已有绑定用户登录 | 高安全或灰度阶段 |

确认页入口为 `/federated/confirm`。创建本地账号和绑定已有账号分别提交到 `/federated/confirm/create`、`/federated/confirm/bind`。

## 6. 登录页展示

身份源是否出现在登录页由状态、登录展示开关和排序共同决定。

启用或禁用身份源：

```bash
curl -X PATCH 'http://localhost:9000/api/admin/v1/identity-providers/{id}/status?enabled=true' \
  -u admin:admin123
```

设置是否展示到登录页：

```bash
curl -X PATCH http://localhost:9000/api/admin/v1/identity-providers/{id}/login-display \
  -H "Content-Type: application/json" \
  -u admin:admin123 \
  -d '{"displayOnLogin":true}'
```

`displayOrder` 越小越靠前。建议只展示用户能识别且确实可用的身份源。

## 7. 属性映射

`attributeMapping` 用于把外部属性映射到本地用户、角色或组织。

固定角色示例：

```json
{
  "roles": {
    "value": "USER"
  }
}
```

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

## 8. 运维建议

- 身份源 secret、LDAP bind password 和 SAML 证书轮换要纳入运维日历。
- 修改 `registrationId` 会影响回调路径和绑定关系，应避免上线后变更。
- 外部身份源 subject 规则变化时，先导出并复核已有绑定。
- 删除绑定不应自动删除本地用户，除非这是明确的账号生命周期动作。
- 生产环境优先使用 HTTPS metadata、HTTPS redirect 和稳定 issuer。
- LDAP/AD 同步先 dry-run，确认 create/update 样本后再执行。
