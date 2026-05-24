# OAuth 2.0 / OIDC 概念

## 1. OAuth 2.0 基础

OAuth 2.0 是一个授权框架，允许第三方应用获取对用户资源的有限访问权限。

### 1.1 角色

| 角色 | 说明 | ArmorAuth 中的对应 |
|------|------|-------------------|
| Resource Owner | 资源所有者（用户） | 终端用户 |
| Client | 请求访问资源的第三方应用 | OAuth2 Client |
| Authorization Server | 验证身份并颁发 token | ArmorAuth Server |
| Resource Server | 托受保护资源的服务器 | 你的业务 API |

### 1.2 授权流程

#### Authorization Code（推荐）

```
用户 → Client → Authorization Server
                  ↓
              用户登录授权
                  ↓
              返回 Authorization Code
                  ↓
Client → Authorization Server: 用 Code 换 Token
                  ↓
              返回 Access Token (+ Refresh Token)
                  ↓
Client → Resource Server: 用 Access Token 访问资源
```

#### Client Credentials

```
Client → Authorization Server: 直接用 client_id + client_secret
                  ↓
              返回 Access Token
                  ↓
Client → Resource Server: 用 Access Token 访问资源
```

适用于机器对机器（M2M）通信，无需用户参与。

## 2. OIDC（OpenID Connect）

OIDC 是建立在 OAuth 2.0 之上的身份层，提供用户身份信息。

### 2.1 ID Token

ID Token 是一个 JWT，包含用户身份信息：

```json
{
  "iss": "https://auth.example.com",
  "sub": "user-id-123",
  "aud": "client-id",
  "exp": 1716000000,
  "iat": 1715996400,
  "name": "张三",
  "email": "zhangsan@example.com",
  "roles": ["USER", "ADMIN"]
}
```

### 2.2 UserInfo 端点

```
GET /userinfo
Authorization: Bearer <access_token>

Response:
{
  "sub": "user-id-123",
  "name": "张三",
  "email": "zhangsan@example.com"
}
```

### 2.3 Discovery 端点

```
GET /.well-known/openid-configuration

Response:
{
  "issuer": "https://auth.example.com",
  "authorization_endpoint": "https://auth.example.com/oauth2/authorize",
  "token_endpoint": "https://auth.example.com/oauth2/token",
  "userinfo_endpoint": "https://auth.example.com/userinfo",
  "jwks_uri": "https://auth.example.com/oauth2/jwks",
  ...
}
```

## 3. PKCE（Proof Key for Code Exchange）

PKCE 是 Authorization Code 流程的安全增强，防止授权码被拦截。

### 流程

1. Client 生成 `code_verifier`（随机字符串）
2. Client 计算 `code_challenge = SHA256(code_verifier)`
3. 授权请求携带 `code_challenge`
4. 换 token 时携带 `code_verifier`
5. Server 验证 `SHA256(code_verifier) == code_challenge`

### 适用场景

- SPA（单页应用）
- 移动应用
- 任何无法安全存储 client_secret 的客户端

## 4. Scopes

Scopes 定义了 token 可以访问的资源范围。

| Scope | 说明 | 返回的信息 |
|-------|------|-----------|
| `openid` | 启用 OIDC | ID Token |
| `profile` | 用户资料 | name, picture, etc. |
| `email` | 邮箱 | email, email_verified |
| `offline_access` | 离线访问 | Refresh Token |

## 5. Token 类型

### 5.1 Access Token

用于访问受保护资源，短生命周期（通常 5-15 分钟）。

### 5.2 Refresh Token

用于获取新的 Access Token，长生命周期（通常 7-30 天）。

### 5.3 ID Token

包含用户身份信息，由客户端验证和解析。

## 6. ArmorAuth 中的 OAuth 2.0 / OIDC

### 6.1 支持的端点

| 端点 | 路径 | 说明 |
|------|------|------|
| Authorization | `/oauth2/authorize` | 用户授权 |
| Token | `/oauth2/token` | 获取 token |
| Token Introspection | `/oauth2/introspect` | 验证 token |
| Token Revocation | `/oauth2/revoke` | 撤销 token |
| UserInfo | `/userinfo` | 获取用户信息 |
| JWKS | `/oauth2/jwks` | 公钥 |
| Discovery | `/.well-known/openid-configuration` | 配置发现 |
| Device Authorization | `/oauth2/device_authorization` | 设备授权 |
| Device Verification | `/activate` | 设备激活 |

### 6.2 支持的授权类型

- `authorization_code` - 标准授权码流程
- `client_credentials` - 客户端凭证
- `refresh_token` - 刷新 token
- `urn:ietf:params:oauth:grant-type:device_code` - 设备授权

### 6.3 安全特性

- PKCE 强制（公共客户端）
- 隐式流程已禁用
- Refresh Token 轮换
- 持久化 JWK 密钥
- 登录限流和锁定
- MFA 支持
