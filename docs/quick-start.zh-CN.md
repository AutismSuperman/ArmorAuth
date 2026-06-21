# ArmorAuth 快速开始

本文用于从源码启动一个本地 ArmorAuth 环境，适合贡献者和评估者。生产部署请阅读 [部署指南](deployment-guide.zh-CN.md)。

## 前置条件

| 依赖 | 版本 |
| --- | --- |
| JDK | 21+ |
| Maven | 3.9+ |
| MySQL | 8.0+ |
| Node.js | 18+ |

## 准备数据库

创建 ArmorAuth 数据库：

```sql
CREATE DATABASE IF NOT EXISTS identity_server
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

可以通过环境变量或 Spring Boot 启动参数配置数据源：

```bash
SPRING_DATASOURCE_URL=jdbc:mysql://127.0.0.1:3306/identity_server
SPRING_DATASOURCE_USERNAME=armorauth
SPRING_DATASOURCE_PASSWORD=<password>
```

服务启动时会自动执行 Flyway 数据库迁移。

## 配置开发密钥

本地开发可以先配置一个加密密钥：

```bash
ARMORAUTH_CRYPTO_SECRET=<local-development-secret>
```

如果需要验证 key rotation：

```bash
ARMORAUTH_CRYPTO_SECRET=<old-secret>
ARMORAUTH_CRYPTO_KEYS=v2=<new-secret>
ARMORAUTH_CRYPTO_ACTIVE_KEY_ID=v2
```

开发密钥不要复用到共享环境或生产环境。

## 构建服务端

在仓库根目录执行：

```bash
mvn -pl armorauth-server -am package -DskipTests
```

构建产物：

```text
armorauth-server/target/armorauth-server-1.0.0.jar
```

## 启动服务端

启动打包后的服务端：

```bash
java -jar armorauth-server/target/armorauth-server-1.0.0.jar
```

服务端会提供托管身份页面、OAuth/OIDC 标准端点和管理 API。

如果只需要本地 H2 开发环境，可以使用 `local` profile：

```bash
java -jar armorauth-server/target/armorauth-server-1.0.0.jar --spring.profiles.active=local
```

## 启动管理控制台

```bash
cd armorauth-admin-ui
npm install
npm run dev
```

Vite 开发服务器会把 API 请求代理到 ArmorAuth 服务端。

## 验证环境

检查健康状态和 OpenID Connect discovery：

```bash
curl http://localhost:9000/actuator/health
curl http://localhost:9000/.well-known/openid-configuration
```

默认开发种子数据包含一个本地评估用管理员账号。进入共享环境前必须替换默认凭据。

## 下一步

- 在管理控制台配置第一个 OAuth/OIDC 应用。
- 阅读 [操作手册](operation-manual.zh-CN.md)，了解日常管理流程。
- 对外开放服务前阅读 [部署指南](deployment-guide.zh-CN.md)。
- 阅读 [安全最佳实践](security-best-practices.md)，完成加固检查。
