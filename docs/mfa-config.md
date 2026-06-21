# MFA 与账号安全配置指南

本文说明 ArmorAuth 当前已经实现的账号安全能力：联系方式验证、TOTP、Passkey/WebAuthn、登录 MFA 偏好以及应用/角色触发的 MFA。接口逐项参考见 [API Reference](api-reference.md)。

## 能力边界

| 能力 | 当前状态 | 主要入口 |
| --- | --- | --- |
| 邮箱验证 | 已实现 | `/api/account/v1/me/email:send-verification-code`、`/api/account/v1/me/email:verify` |
| 手机号验证 | 已实现 | `/api/account/v1/me/phone:send-verification-code`、`/api/account/v1/me/phone:verify` |
| TOTP | 已实现 | `/api/account/v1/factors/totp`、`/api/account/v1/factors/{id}:verify` |
| Passkey/WebAuthn 注册 | 已实现 | `/api/account/v1/factors/passkey:begin-registration`、`/api/account/v1/factors/passkey/{id}:finish-registration` |
| Passwordless Passkey 登录 | 已实现 | `/login/passkey/options`、`/login/passkey/finish` |
| 登录 MFA 挑战 | 已实现 | `/login/mfa`、`/login/passkey/assertion/*` |
| 恢复码独立管理 | 尚未提供独立查询接口 | 不要在客户端依赖独立恢复码查询端点 |

## 1. 联系方式验证

邮箱和手机号可以作为后续登录、找回或安全提醒的基础。用户更新邮箱或手机号后，服务端会清除对应 verified 标记，需要重新验证。

发送邮箱验证码：

```bash
curl -X POST http://localhost:9000/api/account/v1/me/email:send-verification-code \
  -H "Content-Type: application/json" \
  -b cookie.txt
```

验证邮箱：

```bash
curl -X POST http://localhost:9000/api/account/v1/me/email:verify \
  -H "Content-Type: application/json" \
  -b cookie.txt \
  -d '{"code":"123456"}'
```

手机号把路径中的 `email` 换成 `phone`。

开发和演示环境可以通过配置暴露 mock code，便于没有短信或邮件网关时完成流程。共享和生产环境必须关闭 mock code，并替换真实发送实现。

## 2. 用户绑定 TOTP

### Web 流程

1. 用户登录后进入账号中心。
2. 打开“我的 MFA”或“安全性与登录”。
3. 点击添加 Authenticator。
4. 页面展示二维码和 otpauth URI。
5. 用户用 Authenticator app 扫描二维码。
6. 输入 6 位验证码完成激活。
7. 按需开启“登录二次验证”偏好。

### API 流程

初始化 TOTP：

```bash
curl -X POST http://localhost:9000/api/account/v1/factors/totp \
  -H "Content-Type: application/json" \
  -b cookie.txt
```

响应包含 `factorId`、`secret`、`uri`。客户端应优先展示二维码，并提供 URI 复制入口。

验证并激活：

```bash
curl -X POST http://localhost:9000/api/account/v1/factors/{factorId}:verify \
  -H "Content-Type: application/json" \
  -b cookie.txt \
  -d '{"code":"123456"}'
```

## 3. Passkey / WebAuthn

Passkey 可以用于无密码登录，也可以作为 MFA 因子。生产环境需要稳定域名、HTTPS、正确的 RP ID 和 Origin；反向代理场景要正确传递 `X-Forwarded-*`。

初始化注册：

```bash
curl -X POST http://localhost:9000/api/account/v1/factors/passkey:begin-registration \
  -H "Content-Type: application/json" \
  -b cookie.txt \
  -d '{"name":"MacBook Touch ID","rpId":"auth.example.com","rpName":"ArmorAuth"}'
```

浏览器调用 WebAuthn API 后提交 attestation：

```bash
curl -X POST http://localhost:9000/api/account/v1/factors/passkey/{factorId}:finish-registration \
  -H "Content-Type: application/json" \
  -b cookie.txt \
  -d '{
    "challenge":"challenge-from-begin",
    "credentialId":"base64url-credential-id",
    "clientDataJSON":"base64url-client-data-json",
    "attestationObject":"base64url-attestation-object",
    "rpId":"auth.example.com",
    "name":"MacBook Touch ID"
  }'
```

## 4. 登录 MFA 触发链路

```text
用户完成第一步认证
  -> 登录成功处理器检查 MFA 策略
  -> 用户偏好、应用策略、角色策略或登录策略命中
  -> 会话保存 pending principal
  -> 跳转 /login/mfa
  -> 用户使用 TOTP 或 Passkey 完成挑战
  -> 恢复原登录态并继续 OAuth2/OIDC 授权流程
```

MFA 可能由以下条件触发：

- 用户自己启用了登录 MFA。
- 管理员角色等高权限角色要求 MFA。
- 当前 OAuth2/OIDC 应用开启了应用级 MFA。
- 登录策略要求额外验证。

## 5. 应用级 MFA

创建应用时可以设置 `mfaRequired`：

```bash
curl -X POST http://localhost:9000/api/admin/v1/applications \
  -H "Content-Type: application/json" \
  -u admin:admin123 \
  -d '{
    "clientName":"Secure App",
    "clientAuthenticationMethods":"client_secret_basic",
    "authorizationGrantTypes":"authorization_code,refresh_token",
    "redirectUris":"https://app.example.com/login/oauth2/code/armorauth",
    "scopes":["openid","profile"],
    "mfaRequired":true
  }'
```

已有应用通过更新应用或登录策略页面调整。敏感后台、管理控制台、财务和高权限 API 客户端建议启用。

## 6. 用户 MFA 偏好

用户登录后可以开关自己的登录 MFA 偏好：

```bash
curl -X PATCH http://localhost:9000/api/account/v1/security/mfa \
  -H "Content-Type: application/json" \
  -b cookie.txt \
  -d '{"mfaEnabled":true}'
```

开启前应至少有一个已验证且启用的 MFA 因子。管理员不应在没有恢复流程的情况下对全量用户强制 MFA。

## 7. 运维建议

- 管理员和高权限应用默认要求 MFA。
- 联系方式未验证前，不作为恢复、通知或 OTP 登录依据。
- Passkey 上线前先在目标浏览器、移动系统和反向代理环境中验证 RP ID/Origin。
- 生产环境关闭验证码 mock code。
- 记录 MFA 失败、Passkey 注册、因子删除和联系方式变更审计。
- 用户设备丢失时，优先由管理员核验身份后临时禁用 MFA 或删除失效因子。
