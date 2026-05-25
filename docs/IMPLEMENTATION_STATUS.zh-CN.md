# ArmorAuth 项目状态

本文汇总当前项目能力状态。公开文档中不保留本地运行日志、一次性验证记录、PID 或机器路径。

## 当前范围

ArmorAuth 当前提供：

- OAuth 2.0 与 OpenID Connect 授权服务器流程。
- 托管身份页面：登录、授权确认、MFA、设备激活、激活结果、联合身份确认。
- 管理 API 与管理控制台：应用、Scope、用户、角色、权限、组织、身份源、登录策略、审计、Webhook、Token 统计和 JWK 元数据。
- JWK 持久化存储与密钥加密保存。
- 密码、验证码、TOTP、MFA、Passkey/WebAuthn 和无密码登录能力。
- OAuth2/OIDC、SAML SP、LDAP/AD bind 登录和账号绑定。
- SCIM 2.0 用户与组同步端点。
- Webhook 投递与 Java SPI Actions。
- 用于应用侧权限判断的授权检查 API。
- MySQL 与本地 H2 开发 profile 的 Flyway 迁移。

## 能力矩阵

| 范围 | 状态 | 说明 |
| --- | --- | --- |
| OAuth2/OIDC 核心 | 可用 | Authorization code、refresh、client credentials、device flow、discovery、JWKS、revocation、introspection、logout |
| 托管登录 UI | 可用 | 统一身份页面风格，支持中英文切换 |
| 管理 API | 可用 | `/api/admin/v1` 下的管理接口 |
| 管理控制台 | 可用 | Vue 3 控制台覆盖核心管理流程 |
| 用户与权限模型 | 可用 | 用户、角色、权限、组织、租户、登录策略钩子 |
| MFA 与 Passkey | 可用 | TOTP、MFA challenge、WebAuthn assertion、无密码入口 |
| 联合身份 | 可用 | OAuth2/OIDC、SAML SP、LDAP/AD bind/search、账号绑定 |
| SCIM | 可用 | 用户和组同步接口 |
| Webhook | 可用 | 签名投递记录和重试模型 |
| Actions | 可用 | Java SPI 扩展点 |
| 密钥保护 | 可用 | 版本化密文格式和 key-ring 轮换流程 |
| 部署文档 | 可用 | 生产配置、反向代理、备份和安全清单 |

## 近期完成

- 统一登录、授权确认、MFA、设备激活、激活结果、首页和联合确认页的托管身份页面风格。
- 为服务端 UI 页面增加中英文切换。
- 增加可关闭、可自动消失的通知行为。
- 修复联合确认页取消操作，返回登录页时会清理待确认联合登录上下文。
- 为不同身份页面增加按功能区分的插画。
- 增加身份源展示偏好和图标元数据。
- 扩展登录策略与 MFA 相关管理行为。

## 后续方向

- 发布更完整的 Docker/Kubernetes 示例和 release 打包流程。
- 改进 Spring Boot Starter 的接入体验。
- 为常见接入栈提供 SDK 或客户端辅助库。
- 深化企业级联合身份能力，例如 SAML IdP、SLO 编排和证书生命周期工具。
- 增强 WebAuthn attestation trust store 管理。
- 扩展运营仪表盘和告警模板。
- 持续补强多租户和高可用部署路径的测试覆盖。

## 验证原则

代码变更应先运行最小有用检查，涉及共享行为时扩大验证范围。常见检查包括：

- 变更模块编译或打包。
- 变更模块的聚焦单元测试或集成测试。
- 授权行为变化时执行 OAuth/OIDC 协议 smoke test。
- 模板、静态资源或 i18n 变化时检查托管 UI 渲染。
- 数据库 schema 变化时检查迁移。

文档应记录可长期维护的产品行为。不要提交本地 PID、机器路径、临时日志或一次性运行转录。
