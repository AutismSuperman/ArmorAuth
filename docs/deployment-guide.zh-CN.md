# ArmorAuth 部署指南

本文说明 ArmorAuth 面向生产环境的部署要点，适用于从源码构建或从内部发布流水线获取服务端产物的场景。

## 部署模型

典型部署包含：

- 以 Spring Boot 服务运行的 `armorauth-server`。
- MySQL 作为主关系型数据库。
- 负责 HTTPS 终止的反向代理或 Ingress。
- 用于数据库凭据和 ArmorAuth 加密密钥的密钥管理系统。
- 集中式日志、指标、备份和告警。

管理控制台可以与服务端部署在同一域名下，也可以作为独立前端调用管理 API。

## 必要配置

| 范围 | 必要配置 |
| --- | --- |
| Issuer | 配置稳定的外部 issuer URL |
| 数据库 | 使用托管 MySQL 和最小权限数据库用户 |
| HTTPS | 在代理或 Ingress 层终止 TLS |
| Cookie | 启用 secure 和 HTTP-only session cookie |
| 加密 | 配置稳定的加密密钥和轮换策略 |
| 管理员 | 替换开发凭据并限制管理访问 |
| 备份 | 覆盖 JWK、应用、用户、授权记录和 Flyway 元数据 |

环境变量示例：

```bash
SPRING_DATASOURCE_URL=jdbc:mysql://db.example.internal:3306/identity_server
SPRING_DATASOURCE_USERNAME=armorauth
SPRING_DATASOURCE_PASSWORD=<strong-password>

SPRING_SECURITY_OAUTH2_AUTHORIZATIONSERVER_ISSUER=https://auth.example.com

SERVER_SERVLET_SESSION_COOKIE_SECURE=true
SERVER_SERVLET_SESSION_COOKIE_HTTPONLY=true

ARMORAUTH_CRYPTO_SECRET=<v1-secret>
ARMORAUTH_CRYPTO_KEYS=v2=<v2-secret>
ARMORAUTH_CRYPTO_ACTIVE_KEY_ID=v2
```

## 反向代理

代理层需要转发原始 Host 和协议头，确保 URL 生成、重定向和 Cookie 与公开 issuer 一致。

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
```

启用 forwarded header 处理：

```yaml
server:
  forward-headers-strategy: native
```

## 数据库迁移

ArmorAuth 使用 Flyway 管理数据库迁移。发布流程建议：

1. 升级前备份数据库。
2. 在受控环境部署新版本。
3. 让 Flyway 在启动时执行迁移，或把迁移作为独立发布步骤。
4. 健康检查通过后再逐步导流。

生产数据库不要启用 Hibernate 自动改表。

## 密钥加密与轮换

ArmorAuth 使用如下格式保存加密字段：

```text
{enc}<keyId>:<payload>
```

受保护数据包括身份源密钥、Webhook 密钥、TOTP 数据和 JWK 私钥。

推荐轮换流程：

1. 保留现有 key。
2. 在 `ARMORAUTH_CRYPTO_KEYS` 中增加新 key。
3. 将 `ARMORAUTH_CRYPTO_ACTIVE_KEY_ID` 设置为新 key id。
4. 通过管理端 rekey 操作重加密存量记录。
5. 验证所有节点都能读取已有密文。
6. 存量密文迁移并备份完成后，再移除旧 key。

## 备份与恢复

数据库和外部密钥配置需要一起备份。以下数据尤其重要：

| 数据 | 重要性 |
| --- | --- |
| `jwk_key` | 已签发 access token 和 ID token 依赖这些签名密钥 |
| OAuth2 clients | 应用和 redirect 配置 |
| 用户、角色、权限、组织 | 身份和授权数据 |
| 身份源与联合绑定 | 外部身份集成状态 |
| OAuth2 authorizations 和 consents | 活跃授权和用户同意记录 |
| Flyway history | 数据库迁移状态 |

丢失数据库或加密 key 可能导致现有 token 失效，或使加密密钥无法读取。

## 可观测性

使用健康检查端点作为存活和就绪判断：

```text
/actuator/health
```

指标、info 等运维端点应仅允许可信操作员访问。建议集中收集应用日志，并对连续登录失败、Token 签发异常、Webhook 投递失败和数据库连接异常建立告警。

## 安全清单

- 所有外部 OAuth/OIDC 端点必须使用 HTTPS。
- issuer 必须稳定且与公开 URL 一致。
- Cookie 必须启用 secure 和 HTTP-only。
- 替换所有开发凭据。
- 密钥放入密钥管理系统，不进入源码仓库。
- 管理 API 通过网络和角色双重限制。
- 数据库和加密 key 一起备份。
- 轮换加密 key 时保留旧 key，直到旧密文完成迁移。
- 定期审计管理员操作。
- 在依赖备份前先演练恢复流程。

## 升级清单

1. 阅读版本说明和迁移说明。
2. 备份数据库和密钥配置。
3. 构建或拉取新服务端产物。
4. 先部署到预发环境。
5. 验证健康检查、登录、Token、管理 API 和联合登录。
6. 渐进式发布。
7. 发布完成前保留回滚产物和旧密钥。
