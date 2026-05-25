# ArmorAuth 开发种子 Profile

ArmorAuth 提供面向开发的种子 profile，用于本地 UI 和 API 探索。它不是生产部署模型的一部分。

## 用途

该 profile 会创建具有代表性的数据：

- 用户和角色。
- 租户和组织。
- OAuth2/OIDC 应用。
- 身份源。
- Webhook。
- 审计事件。
- Token 统计。

这些数据帮助贡献者开发管理页面和集成流程，避免每次手动准备记录。

## 使用方式

在本地开发环境中使用 `mock` profile 启动服务端：

```bash
mvn -pl armorauth-server -am spring-boot:run -Dspring-boot.run.profiles=mock
```

该 profile 使用本地 runtime 目录下的 H2 文件数据库。生成的 runtime 文件不要提交到仓库。

## 种子账号

种子账号只用于本地开发。密码和测试用户不能复用到共享环境。

| 用户名 | 角色 |
| --- | --- |
| `admin` | 超级管理员 |
| `app.manager` | 应用管理员 |
| `audit.viewer` | 审计查看者 |
| `demo.user` | 普通用户 |

## 贡献者说明

- 种子数据应足够真实，能覆盖管理控制台常见场景。
- 不要把本机路径、代理设置、PID、日志和临时命令输出写进本文档。
- 优先增加可复用的种子记录，而不是记录一次性手动步骤。
