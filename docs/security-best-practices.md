# 安全最佳实践

## 1. 部署安全

### 1.1 HTTPS

**必须**在生产环境使用 HTTPS。OAuth 2.0 和 OIDC 协议要求安全的传输层。

```nginx
server {
    listen 443 ssl http2;
    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;
    # ... 其他配置
}
```

### 1.2 Issuer URL

配置稳定的、可公开访问的 issuer URL：

```yaml
spring:
  security:
    oauth2:
      authorizationserver:
        issuer: https://auth.example.com
```

### 1.3 Cookie 安全

```yaml
server:
  servlet:
    session:
      cookie:
        secure: true
        http-only: true
        same-site: lax
```

### 1.4 数据库安全

- 使用独立的数据库用户，仅授予必要权限
- 生产环境禁止 `ddl-auto: update`
- 定期备份数据库

## 2. OAuth 2.0 安全

### 2.1 PKCE

所有公共客户端（SPA、移动应用）**必须**使用 PKCE：

```
# 创建公共客户端时自动启用 PKCE
POST /api/admin/v1/applications
{
  "clientName": "SPA App",
  "clientAuthenticationMethods": "none",
  "authorizationGrantTypes": "authorization_code,refresh_token",
  "clientSettings": {
    "requireProofKey": true
  }
}
```

### 2.2 Redirect URI

- 禁止使用通配符 redirect URI
- 生产环境要求 HTTPS
- 精确匹配，不要使用路径前缀匹配

### 2.3 Token 生命周期

```yaml
# 推荐配置
access-token-ttl: 5m      # 短生命周期
refresh-token-ttl: 24h    # 较长但可撤销
reuse-refresh-tokens: false # 每次刷新生成新 refresh token
```

### 2.4 隐式流程

**已禁用**隐式流程。现代应用应使用 Authorization Code + PKCE。

## 3. 认证安全

### 3.1 密码策略

- 最小长度：8 字符
- 复杂度要求：大小写字母 + 数字 + 特殊字符
- 密码历史：禁止重复最近 5 次密码
- 建议密码过期：90 天

### 3.2 登录保护

- 连续 5 次失败锁定 30 分钟
- 按用户名、IP、客户端 ID 限流
- 记录所有登录失败事件

### 3.3 MFA

- 管理员账号强制 MFA
- 关键应用配置 `mfaRequired`
- 支持 TOTP、邮箱/短信 OTP

## 4. API 安全

### 4.1 管理 API

- 所有 `/api/admin/**` 端点需要认证
- 使用 RBAC 控制访问权限
- 所有变更操作记录审计日志

### 4.2 Actuator

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  endpoint:
    health:
      show-details: when-authorized
```

- `/actuator/health` 公开访问
- 其他端点需要 `SUPER_ADMIN` 角色

### 4.3 CORS

如果需要跨域访问，明确配置允许的来源：

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("https://app.example.com"));
    config.setAllowedMethods(List.of("GET", "POST"));
    config.setAllowedHeaders(List.of("Authorization"));
    // ...
}
```

## 5. 数据安全

### 5.1 敏感数据

- 客户端密钥：仅在创建/重置时返回明文
- JWK 密钥：定期轮换
- Token 存储：使用 SHA-256 哈希
- 数据库密码：使用环境变量，不要硬编码

### 5.2 审计日志

记录以下事件：
- 用户登录/登出
- 密码修改/重置
- MFA 绑定/解绑
- 应用创建/修改/删除
- 角色/权限变更
- 身份源配置变更
- Token 签发/刷新/撤销

### 5.3 密钥管理

- 使用持久化 JWK 存储
- 定期轮换签名密钥
- 保留旧密钥一段时间以验证已签发的 token
- 备份 `jwk_key` 表

## 6. 监控

### 6.1 核心指标

监控以下指标：
- 登录成功/失败率
- Token 签发数量
- MFA 挑战成功率
- API 响应时间
- 错误率

### 6.2 告警建议

- 登录失败率突增
- 异常 Token 请求量
- 服务不可用
- 数据库连接失败

## 7. 备份与恢复

### 7.1 定期备份

- 数据库：每日全量备份
- JWK 密钥：每次轮换后备份
- 配置文件：版本控制

### 7.2 恢复测试

定期测试备份恢复流程，确保：
- 数据库可以成功恢复
- JWK 密钥完整
- 服务可以正常启动

## 8. 安全检查清单

部署前检查：

- [ ] HTTPS 已启用
- [ ] Issuer URL 配置正确
- [ ] Cookie 设置为 secure/http-only
- [ ] 生产环境禁用 `ddl-auto`
- [ ] 数据库用户权限最小化
- [ ] 管理员已启用 MFA
- [ ] 审计日志已启用
- [ ] Actuator 端点已保护
- [ ] JWK 密钥已备份
- [ ] 防火墙仅暴露必要端口
