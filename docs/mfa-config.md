# MFA 多因素认证配置指南

## 概述

ArmorAuth 支持多种 MFA 方式：
- **TOTP**：基于时间的一次性密码（Google Authenticator、Microsoft Authenticator 等）
- **邮箱 OTP**：发送验证码到用户邮箱
- **短信 OTP**：发送验证码到用户手机

## 1. 用户绑定 TOTP

### 1.1 通过 Web 界面

1. 登录后访问账户设置页面
2. 点击"绑定 MFA"
3. 使用认证器 App 扫描二维码
4. 输入 6 位验证码完成绑定

### 1.2 通过 API

```bash
# 生成 TOTP 密钥
curl -X POST http://localhost:9000/api/account/v1/factors/totp \
  -H "Content-Type: application/json" \
  -u user:password

# 验证并绑定
curl -X POST http://localhost:9000/api/account/v1/factors/{factorId}:verify \
  -H "Content-Type: application/json" \
  -u user:password \
  -d '{"code": "123456"}'
```

## 2. 应用级别 MFA 策略

### 2.1 为应用启用强制 MFA

```bash
# 创建应用时启用 MFA
curl -X POST http://localhost:9000/api/admin/v1/applications \
  -H "Content-Type: application/json" \
  -u admin:admin123 \
  -d '{
    "clientName": "Secure App",
    "clientAuthenticationMethods": "client_secret_basic",
    "authorizationGrantTypes": "authorization_code,refresh_token",
    "redirectUris": "http://localhost:8080/callback",
    "mfaRequired": true
  }'

# 为已有应用启用 MFA
curl -X PUT http://localhost:9000/api/admin/v1/applications/{id} \
  -H "Content-Type: application/json" \
  -u admin:admin123 \
  -d '{"mfaRequired": true}'
```

### 2.2 角色级别 MFA

以下角色自动要求 MFA：
- `SUPER_ADMIN`
- `TENANT_ADMIN`

这些角色的用户登录时会自动触发 MFA 挑战。

## 3. MFA 认证流程

```
用户登录
  ↓
密码验证成功
  ↓
检查 MFA 策略：
  - 用户是否绑定了 MFA 因子？
  - 应用是否要求 MFA？
  - 用户角色是否要求 MFA？
  ↓
需要 MFA → 跳转 /login/mfa
  ↓
用户输入 TOTP 验证码
  ↓
验证通过 → 完成登录
```

## 4. 恢复码

绑定 TOTP 时会生成一组恢复码，用于在无法访问认证器时恢复账户。

```bash
# 查看恢复码
curl http://localhost:9000/api/account/v1/factors/{factorId}/recovery-codes \
  -u user:password

# 使用恢复码登录（在 MFA 页面输入恢复码）
```

## 5. 邮箱/短信 OTP（框架实现）

`EmailOtpService` 和 `SmsOtpService` 提供了 OTP 生成和验证框架：

```java
// 生成 OTP
String otp = emailOtpService.generateOtp("user@example.com");

// 验证 OTP
boolean valid = emailOtpService.verifyOtp("user@example.com", "123456");
```

实际发送邮件/短信需要集成第三方服务商。

## 6. 安全建议

- 生产环境务必为管理员账号启用 MFA
- 定期审查 MFA 绑定情况
- 为关键应用启用 `mfaRequired`
- 保管好恢复码，建议离线存储
