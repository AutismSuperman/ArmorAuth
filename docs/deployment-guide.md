# ArmorAuth 部署指南

## 1. 快速启动（Docker Compose）

```bash
# 构建
mvn clean package -DskipTests

# 启动
docker-compose up -d
```

服务启动后访问：
- 登录页：http://localhost:9000/login
- 管理台：http://localhost:9000/api/admin/v1/
- Actuator：http://localhost:9000/actuator/health

## 2. 生产环境配置

### 2.1 必须配置项

| 配置项 | 说明 | 示例 |
|--------|------|------|
| `server.servlet.session.cookie.secure` | Cookie 仅 HTTPS | `true` |
| `spring.security.oauth2.authorizationserver.issuer` | 稳定的 issuer URL | `https://auth.example.com` |
| `spring.datasource.url` | 生产数据库连接 | MySQL/JDBC URL |
| `spring.datasource.password` | 数据库密码 | 使用环境变量 |
| `spring.jpa.hibernate.ddl-auto` | 禁止自动建表 | `none` |
| `armorauth.crypto.secret-key` | `v1` 可逆密钥 fallback | 使用环境变量 |
| `armorauth.crypto.keys` | 额外 key-ring | `v2=<secret>` |
| `armorauth.crypto.active-key-id` | 新写入 secret 使用的 key id | `v2` |

### 2.2 环境变量

```bash
# 数据库
SPRING_DATASOURCE_URL=jdbc:mysql://db-host:3306/identity_server
SPRING_DATASOURCE_USERNAME=armorauth
SPRING_DATASOURCE_PASSWORD=<strong-password>

# Session
SERVER_SERVLET_SESSION_COOKIE_SECURE=true
SERVER_SERVLET_SESSION_COOKIE_HTTPONLY=true

# Issuer（必须与外部访问地址一致）
SPRING_SECURITY_OAUTH2_AUTHORIZATIONSERVER_ISSUER=https://auth.example.com

# Secret encryption at rest
ARMORAUTH_CRYPTO_SECRET=<old-or-v1-production-secret>
ARMORAUTH_CRYPTO_KEYS=v2=<new-production-secret>
ARMORAUTH_CRYPTO_ACTIVE_KEY_ID=v2
```

### 2.3 Secret 加密与轮换

ArmorAuth 会加密保存 IdP client secret、Webhook secret、TOTP secret 和 `jwk_key.private_key`。密文格式为：

```text
{enc}<keyId>:<payload>
```

轮换建议：

1. 先保留旧 key，例如 `ARMORAUTH_CRYPTO_SECRET` 作为 `v1`。
2. 增加新 key，例如 `ARMORAUTH_CRYPTO_KEYS=v2=<new-secret>`。
3. 设置 `ARMORAUTH_CRYPTO_ACTIVE_KEY_ID=v2`，让新写入数据使用 `v2`。
4. 先 dry-run 存量密文重加密：

```bash
curl -u admin:<admin-password> \
  -H "Content-Type: application/json" \
  -d '{"dryRun":true}' \
  https://auth.example.com/api/admin/v1/secret-protection/rekey
```

5. 确认 `total.failed=0` 后执行重加密：

```bash
curl -u admin:<admin-password> \
  -H "Content-Type: application/json" \
  -d '{"dryRun":false}' \
  https://auth.example.com/api/admin/v1/secret-protection/rekey
```

6. 再次 dry-run，确认 `total.wouldRekey=0`。
7. 等所有实例都完成重加密并验证启动后，再移除旧 key。

注意：当前 `ARMORAUTH_CRYPTO_KEYS` 使用逗号分隔 `keyId=secret`，secret 内不要包含逗号。

## 3. 反向代理配置

### 3.1 Nginx 示例

```nginx
server {
    listen 443 ssl http2;
    server_name auth.example.com;

    ssl_certificate /etc/nginx/ssl/auth.example.com.crt;
    ssl_certificate_key /etc/nginx/ssl/auth.example.com.key;

    location / {
        proxy_pass http://127.0.0.1:9000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $host;
        proxy_set_header X-Forwarded-Port $server_port;
    }
}

server {
    listen 80;
    server_name auth.example.com;
    return 301 https://$host$request_uri;
}
```

### 3.2 关键 Header

| Header | 说明 |
|--------|------|
| `X-Forwarded-Proto` | 告知后端实际协议（https） |
| `X-Forwarded-Host` | 告知后端实际域名 |
| `X-Forwarded-For` | 传递客户端真实 IP |
| `X-Forwarded-Port` | 告知后端实际端口 |

### 3.3 Spring Boot 配置

如果使用 Nginx 等反向代理，需要配置：

```yaml
server:
  forward-headers-strategy: native
  tomcat:
    remoteip:
      protocol-header: X-Forwarded-Proto
      remote-ip-header: X-Forwarded-For
      host-header: X-Forwarded-Host
```

## 4. 数据库备份与恢复

### 4.1 MySQL 备份

```bash
# 全量备份
mysqldump -u armorauth -p identity_server > backup_$(date +%Y%m%d_%H%M%S).sql

# 仅结构
mysqldump -u armorauth -p --no-data identity_server > schema_backup.sql

# 仅数据
mysqldump -u armorauth -p --no-create-info identity_server > data_backup.sql
```

### 4.2 MySQL 恢复

```bash
mysql -u armorauth -p identity_server < backup_20260523_120000.sql
```

### 4.3 关键表

| 表 | 说明 | 备份优先级 |
|----|------|-----------|
| `jwk_key` | 签名密钥 | **最高** - 丢失后旧 token 无法验签 |
| `oauth2_client` | OAuth2 客户端配置 | 高 |
| `user_info` | 用户信息 | 高 |
| `user_federated_binding` | 联合登录绑定 | 高 |
| `oauth2_authorization` | 授权记录 | 中 |
| `oauth2_authorization_consent` | 授权同意 | 中 |
| `flyway_schema_history` | 迁移记录 | 中 |

### 4.4 JWK 密钥备份

`jwk_key` 表包含签名密钥元数据和加密后的私钥，**必须定期备份**。丢失 JWK 密钥将导致：
- 所有已签发的 access token 和 ID token 无法验签
- 用户需要重新登录

建议：
1. 每次数据库备份时包含 `jwk_key` 表
2. 单独备份 `ARMORAUTH_CRYPTO_SECRET` / `ARMORAUTH_CRYPTO_KEYS` 到安全的密钥管理系统
3. JWK 轮换或 crypto key 轮换后立即备份数据库和密钥配置

## 5. 监控

### 5.1 Actuator 端点

| 端点 | 说明 | 访问权限 |
|------|------|---------|
| `/actuator/health` | 健康检查 | 公开 |
| `/actuator/info` | 应用信息 | SUPER_ADMIN |
| `/actuator/metrics` | 指标 | SUPER_ADMIN |
| `/actuator/prometheus` | Prometheus 格式指标 | SUPER_ADMIN |

### 5.2 Prometheus 抓取配置

```yaml
scrape_configs:
  - job_name: 'armorauth'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['auth.example.com']
    scheme: https
    basic_auth:
      username: admin
      password: <admin-password>
```

### 5.3 核心指标

| 指标 | 说明 |
|------|------|
| `armorauth.login.success` | 登录成功次数 |
| `armorauth.login.failure` | 登录失败次数 |
| `armorauth.token.issued` | Token 签发次数 |
| `armorauth.token.refreshed` | Token 刷新次数 |
| `armorauth.mfa.challenge` | MFA 挑战次数 |
| `http.server.requests` | HTTP 请求延迟 |

## 6. 安全检查清单

- [ ] 启用 HTTPS，禁止 HTTP 访问 OAuth 端点
- [ ] 配置稳定的 `issuer` URL
- [ ] 设置 `cookie.secure=true`
- [ ] 生产环境禁用 `ddl-auto: update`
- [ ] 使用强密码的独立数据库用户
- [ ] 定期备份数据库，特别是 `jwk_key` 表
- [ ] 显式配置 `ARMORAUTH_CRYPTO_SECRET` / `ARMORAUTH_CRYPTO_KEYS`，不要使用默认开发 key
- [ ] 轮换 crypto key 时保留旧 key，直到存量 `{enc}<oldKeyId>:` 数据完成重加密
- [ ] 配置防火墙，仅暴露 443 端口
- [ ] Actuator 端点需要认证访问
- [ ] 定期轮换 JWK 密钥
- [ ] 启用审计日志
