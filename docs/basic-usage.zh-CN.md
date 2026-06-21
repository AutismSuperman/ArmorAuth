# ArmorAuth 基础使用

本文说明 ArmorAuth 启动后的第一批常用操作。做本地评估时配合 [快速开始](quick-start.zh-CN.md) 使用；进入共享或生产环境前配合 [部署指南](deployment-guide.zh-CN.md) 使用。

如果需要按功能域理解“管理入口 -> API -> 运行时行为 -> 注意事项”的完整链路，请阅读 [功能链路总览](feature-flows.zh-CN.md)。

## 1. 登录管理控制台

打开管理控制台，使用管理员账号登录。

本地开发种子账号只在启用开发种子 Profile 时可用，详见 [开发种子 Profile](mock-system.zh-CN.md)。

进入共享环境前需要先完成：

- 替换开发凭据。
- 确认公开 issuer URL。
- 配置稳定的加密密钥。
- 通过角色和网络限制管理员访问。

## 2. 创建或检查租户

租户用于归集应用、用户、组织和租户感知 issuer 路径。

创建新租户时：

1. 使用稳定、适合 URL 的租户编码。
2. 设置显示名称和启停状态。
3. 需要时配置品牌和域名元数据。
4. 使用 `/t/{tenantCode}` 这样的路径承载租户特定流程。

如果暂时不需要租户隔离，可以只使用默认租户。

## 3. 创建组织和角色

组织用于表达部门、业务单元或客户侧层级。

推荐流程：

1. 先创建顶级组织。
2. 只有当层级会影响授权或报表时，再创建子组织。
3. 将用户加入组织。
4. 优先通过角色和权限授权，避免大量直接给用户绑定权限。

管理员角色应保持精简，并定期复核。

## 4. 创建 OAuth2/OIDC 应用

应用对应 OAuth2/OIDC Client。

创建应用时：

- 选择租户。
- 选择客户端类型。
- 只登记可信 redirect URI。
- 浏览器和移动端优先使用 Authorization Code + PKCE。
- 服务间调用才使用 Client Credentials。
- Client Secret 只放在能保护密钥的后端。
- Scope 控制在最小必要范围。
- 敏感应用按需开启应用级 MFA。

创建后可以在管理控制台的应用端点详情中查看 discovery、authorization、token、JWKS、introspection、revocation 和 logout URL。

## 5. 创建用户并验证联系方式

用户可以本地创建，也可以通过 SCIM 或联合登录进入系统。

本地用户推荐流程：

1. 设置用户名和显示名称。
2. 填写邮箱和手机号。
3. 依赖邮箱/手机号做找回或登录前，先完成验证。
4. 分配角色和组织成员关系。
5. 需要保留审计历史时，优先禁用用户而不是删除用户。

用户登录后可以在托管账号中心管理自己的账号安全。本地开发环境会展示模拟验证码，方便在没有短信或邮件服务的情况下完成验证流程。

## 6. 配置登录和 MFA

登录策略和应用设置决定何时需要额外验证。

推荐基线：

- 管理员必须开启 MFA。
- 敏感应用开启应用级 MFA。
- 用户可以在账号中心绑定 Authenticator app。
- 全局强制 MFA 前准备好恢复和人工支持流程。
- 在审计视图中关注登录失败和 MFA 失败。

TOTP、Passkey 和策略配置详见 [MFA 配置](mfa-config.md)。

## 7. 测试接入应用

Spring Boot 服务推荐流程：

1. 添加 `armorauth-spring-boot-starter`。
2. 配置 Resource Server 或 OIDC Login。
3. 将服务指向 ArmorAuth issuer。
4. 通过 starter 的当前用户解析器读取用户、租户、组织、角色、Scope 和权限声明。
5. Token Relay 只挂到可信下游服务。

配置示例详见 [Spring Boot Starter](spring-boot-starter.md)，扩展点详见 [Spring Boot Starter 扩展规格](spring-boot-starter-extension-spec.md)。

## 8. 查看活动记录

完成登录和 Token 流程测试后，建议查看：

- 活跃会话。
- 审计日志。
- Token 统计。
- Webhook 投递状态。
- 登录失败和 MFA 失败事件。
- 授权检查拒绝记录。

这些视图可以帮助确认应用授权是否符合预期，用户侧安全控制是否生效。

完成以上步骤后，建议再按 [操作手册](operation-manual.zh-CN.md) 建立日常巡检流程，并把租户、应用、管理员、身份源、Webhook 和 JWK 轮换责任人记录到团队运维文档中。
