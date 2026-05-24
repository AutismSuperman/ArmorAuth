# 联合登录配置指南

## 概述

ArmorAuth 支持两种方式配置身份源：
1. **配置文件方式**：在 `application.yml` 中静态配置
2. **数据库方式**：通过管理 API 动态配置（推荐）

## 1. 数据库配置方式（推荐）

### 1.1 通过管理 API 创建身份源

```bash
# 创建标准 OIDC 身份源
curl -X POST http://localhost:9000/api/admin/v1/identity-providers \
  -H "Content-Type: application/json" \
  -u admin:admin123 \
  -d '{
    "providerName": "企业 SSO",
    "providerType": "OIDC",
    "registrationId": "enterprise-oidc",
    "clientId": "your-client-id",
    "clientSecret": "your-client-secret",
    "authorizationUri": "https://sso.example.com/authorize",
    "tokenUri": "https://sso.example.com/token",
    "userinfoUri": "https://sso.example.com/userinfo",
    "scopes": "openid,profile,email",
    "linkingStrategy": "AUTO_REGISTER",
    "displayOrder": 1
  }'
```

### 1.2 通过 Java SDK 创建

```java
ArmorAuthAdminClient client = new ArmorAuthAdminClient(
    "http://localhost:9000", "admin", "admin123");

client.createIdentityProvider(Map.of(
    "providerName", "企业 SSO",
    "providerType", "OIDC",
    "registrationId", "enterprise-oidc",
    "clientId", "your-client-id",
    "clientSecret", "your-client-secret",
    "authorizationUri", "https://sso.example.com/authorize",
    "tokenUri", "https://sso.example.com/token",
    "userinfoUri", "https://sso.example.com/userinfo",
    "scopes", "openid,profile,email",
    "linkingStrategy", "AUTO_REGISTER",
    "displayOrder", 1
));
```

## 2. 支持的身份源类型

| 类型 | 说明 | 默认 scopes | userNameAttribute |
|------|------|-------------|-------------------|
| `OIDC` | 标准 OpenID Connect | openid | sub |
| `WECHAT` | 微信开放平台 | snsapi_login | openid |
| `WECOM` | 企业微信 | snsapi_userinfo | userid |
| `DINGTALK` | 钉钉 | openid | openid |
| `FEISHU` | 飞书 | contact:user.id:readonly | user_id |
| `ALIPAY` | 支付宝 | - | - |
| `QQ` | QQ 互联 | get_user_info | openid |
| `GITEE` | Gitee | user_info | id |
| `SAML` | SAML 2.0 | - | - |
| `LDAP` | LDAP/AD | - | - |

## 3. 账号链接策略

| 策略 | 说明 |
|------|------|
| `AUTO_REGISTER` | 首次登录自动创建本地账号 |
| `CONFIRM` | 首次登录跳转确认页，用户选择创建新账号或绑定已有账号 |
| `EMAIL_MATCH` | 通过邮箱自动匹配已有账号 |
| `NONE` | 不允许自动注册，只允许已有绑定用户登录 |

## 4. 属性映射

配置 `attributeMapping` JSON 字段可以将外部身份源的属性映射到本地用户的角色和组织：

```json
{
  "organization": {
    "fromAttribute": "department",
    "matchBy": "orgCode"
  },
  "roles": {
    "value": "USER"
  }
}
```

支持的映射类型：
- `fromAttribute`：从外部身份源返回的属性中提取值
- `value`：使用固定值

## 5. 配置文件方式

在 `application.yml` 中配置：

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          gitee:
            client-id: your-gitee-client-id
            client-secret: your-gitee-client-secret
            scope: user_info
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            client-name: Gitee
          wechat:
            client-id: your-wechat-appid
            client-secret: your-wechat-secret
            scope: snsapi_login
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            client-name: 微信
            provider: wechat
        provider:
          wechat:
            authorization-uri: https://open.weixin.qq.com/connect/qrconnect
            token-uri: https://api.weixin.qq.com/sns/oauth2/access_token
            user-info-uri: https://api.weixin.qq.com/sns/userinfo
            user-name-attribute: openid
```

## 6. 登录页显示

配置完成后，登录页会自动显示已启用的身份源按钮。可以通过 `displayOrder` 控制显示顺序。

## 7. 启用/禁用身份源

```bash
# 禁用
curl -X PATCH http://localhost:9000/api/admin/v1/identity-providers/{id}/status \
  -H "Content-Type: application/json" \
  -u admin:admin123 \
  -d '{"enabled": false}'

# 启用
curl -X PATCH http://localhost:9000/api/admin/v1/identity-providers/{id}/status \
  -H "Content-Type: application/json" \
  -u admin:admin123 \
  -d '{"enabled": true}'
```
