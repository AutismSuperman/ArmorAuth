# ArmorAuth 文档索引

本文是 `docs` 目录的入口。中文文档优先覆盖完整产品使用链路；英文文档保留对外介绍和基础接入信息。

## 推荐阅读顺序

1. [产品概览](product-overview.zh-CN.md)：了解 ArmorAuth 的定位、核心对象和系统入口。
2. [快速开始](quick-start.zh-CN.md)：从源码启动服务端和管理控制台。
3. [基础使用](basic-usage.zh-CN.md)：完成第一条租户、应用、用户、MFA 和样例接入流程。
4. [功能链路总览](feature-flows.zh-CN.md)：按功能域理解管理入口、API、运行时和注意事项。
5. [操作手册](operation-manual.zh-CN.md)：面向管理员的日常运营流程。
6. [API Reference](api-reference.md)：管理 API、账号 API、SCIM 和协议端点参考。
7. [Spring Boot Starter](spring-boot-starter.md)：业务系统接入 ArmorAuth 的 Spring Boot 配置。

## 文档分类

### 产品与入门

| 文档 | 说明 |
| --- | --- |
| [产品概览](product-overview.zh-CN.md) | 产品定位、适用场景、能力矩阵、对象模型 |
| [快速开始](quick-start.zh-CN.md) | 本地构建、服务端启动、管理控制台启动 |
| [基础使用](basic-usage.zh-CN.md) | 第一次配置租户、应用、用户和 MFA |
| [功能链路总览](feature-flows.zh-CN.md) | 各功能的入口、API、运行时链路和注意事项 |
| [开发种子 Profile](mock-system.zh-CN.md) | 本地开发和演示用种子数据 |

### 功能专题

| 文档 | 说明 |
| --- | --- |
| [操作手册](operation-manual.zh-CN.md) | 管理员日常运营流程 |
| [联合登录配置](federation-config.md) | OIDC/OAuth2、SAML、LDAP/AD 身份源链路 |
| [MFA 配置](mfa-config.md) | TOTP、Passkey、联系方式验证和登录 MFA |
| [OAuth2/OIDC 概念](oauth2-oidc-concepts.md) | 协议基础概念 |

### 集成、运维与参考

| 文档 | 说明 |
| --- | --- |
| [Spring Boot Starter](spring-boot-starter.md) | Resource Server、OIDC Login、当前用户上下文、Admin Client、Token Relay |
| [Spring Boot Starter 扩展规格](spring-boot-starter-extension-spec.md) | Starter 扩展点和自动配置退让规则 |
| [部署指南](deployment-guide.zh-CN.md) | 数据库、反向代理、HTTPS、密钥、备份 |
| [安全最佳实践](security-best-practices.md) | 安全加固和检查清单 |
| [API Reference](api-reference.md) | 端点、请求示例和响应格式 |

## 维护约定

- 用户文档优先引用真实控制器、前端页面和样例路径，避免记录未实现接口。
- API 变化后同步更新 [API Reference](api-reference.md) 和 [功能链路总览](feature-flows.zh-CN.md)。
- 示例命令应标注运行目录、端口和 profile。
- 规划类文档可以保留，但不要作为用户的第一阅读入口。
