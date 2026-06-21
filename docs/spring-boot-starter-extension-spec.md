# ArmorAuth Spring Boot Starter 扩展机制说明

本文说明 `armorauth-spring-boot-starter` 面向业务 Spring Boot 应用暴露的扩展点。实际接入步骤见 [Spring Boot Starter Integration](spring-boot-starter.md)。

## 背景

`armorauth-spring-boot-starter` 面向接入 ArmorAuth 的业务 Spring Boot 应用，而不是授权服务器本身。当前 starter 只提供两类默认能力：

- Resource Server：保护 `/api/**`，放行 `/api/public/**`，将 `roles` 与 `scope` 映射为 Spring Security authorities。
- OIDC Client：启用默认 `oauth2Login`，放行 `/` 和 `/public/**`。

这些默认行为适合 demo，但不足以支撑真实业务扩展。业务方通常需要自定义 JWT claim 映射、租户/组织上下文、SecurityFilterChain 细节、登录成功/失败处理、Admin API 调用方式和跨服务 token relay。

## 设计目标

1. 保持 starter 轻量，不依赖 `armorauth-core`、`armorauth-admin`、`armorauth-model` 等授权服务器运行时模块。
2. 用 Spring Boot 风格暴露配置属性，所有默认行为都可以通过配置关闭或调整。
3. 用 `@ConditionalOnMissingBean` 和显式 Customizer 接口暴露可替换扩展点。
4. 默认启用前必须显式配置 `armorauth.resource-server.enabled=true` 或 `armorauth.oidc-client.enabled=true`，避免接入后意外接管用户安全配置。
5. 对 ArmorAuth 自定义 claim 提供一组稳定的读取工具，降低业务服务解析 `tenant_id`、`org_ids`、`org_roles`、`roles`、`scope` 的重复成本。

## 已实现扩展范围

### 1. Resource Server 扩展

新增配置：

```yaml
armorauth:
  resource-server:
    enabled: true
    security-matcher: /api/**
    permit-all:
      - /api/public/**
    csrf-enabled: false
    principal-claim: sub
    role-claims:
      - roles
    scope-claims:
      - scope
      - scp
    permission-claims:
      - permissions
    organization-role-claims:
      - org_roles
    role-prefix: ROLE_
    scope-prefix: SCOPE_
    permission-prefix: PERMISSION_
    organization-role-prefix: ORG_ROLE_
```

新增扩展点：

- `ArmorAuthJwtGrantedAuthoritiesConverter`：可配置 claim 名称与 authority 前缀。
- `ArmorAuthJwtAuthenticationConverterCustomizer`：允许用户定制 `JwtAuthenticationConverter`。
- `ArmorAuthResourceServerHttpSecurityCustomizer`：允许用户在默认 resource server `SecurityFilterChain` build 前继续定制 `HttpSecurity`。

### 2. OIDC Client 扩展

新增配置：

```yaml
armorauth:
  oidc-client:
    enabled: true
    permit-all:
      - /
      - /public/**
    csrf-enabled: false
    default-success-url: /
    logout-success-url: /
```

新增扩展点：

- `ArmorAuthOidcClientHttpSecurityCustomizer`：允许用户定制登录页、successHandler、failureHandler、logout、session 等。

### 3. 当前用户与租户上下文

新增 API：

- `ArmorAuthCurrentUser`：封装 subject、username、tenantId、organizationIds、organizationRoles、roles、scopes、permissions、claims。
- `ArmorAuthCurrentUserResolver`：从当前 `SecurityContext` 解析用户上下文。
- `SecurityContextArmorAuthCurrentUserResolver`：默认实现，支持 `JwtAuthenticationToken`、`OAuth2AuthenticationToken` 和普通 `Authentication`。

业务方可直接注入：

```java
private final ArmorAuthCurrentUserResolver currentUserResolver;
```

### 4. Admin API RestClient

新增配置：

```yaml
armorauth:
  admin-client:
    enabled: true
    base-url: http://localhost:9000
    username: admin
    password: admin123
    # 或使用 bearer-token
    # bearer-token: xxx
```

新增 API：

- `ArmorAuthAdminRestClient`：基于 Spring `RestClient` 的轻量 Admin API 客户端。
- `ArmorAuthAdminRestClientCustomizer`：允许业务方修改 `RestClient.Builder`，例如设置代理、超时、统一 header、观测拦截器。

第一版保持 Map 风格返回，避免引入 admin DTO 依赖；后续可单独新增 typed client。

### 5. Token Relay

新增 API：

- `ArmorAuthTokenRelayInterceptor`：从当前 `SecurityContext` 提取 `JwtAuthenticationToken` 的 token，转发为下游请求的 `Authorization: Bearer ...`。

它作为独立 bean 暴露，不强制挂到所有 `RestClient`，避免误把用户 token 发送到非受信服务。

## 明确不做

- 不在 starter 内启动授权服务器端点。
- 不引入 JPA、Flyway、admin/model/core/federation 模块。
- 不直接替换业务应用已有的 `SecurityFilterChain`。
- 不实现完整 typed Admin SDK。
- 不默认开启全局 token relay。

## 验收标准

1. 默认不开启时，starter 不创建安全过滤链。
2. 开启 resource server 后，可通过配置调整 matcher、permitAll、CSRF 和 claim 映射。
3. 用户自定义 `JwtAuthenticationConverter` 时，自动配置会让位；用户定义同名默认链 Bean 时，对应默认链会让位。
4. 用户可通过 `ArmorAuthResourceServerHttpSecurityCustomizer` 和 `ArmorAuthOidcClientHttpSecurityCustomizer` 追加安全配置。
5. 当前用户解析器能从 JWT claims 读取 `tenant_id`、`org_ids`、`org_roles`、`roles`、`scope/scp`。
6. Admin RestClient 支持 Basic 与 Bearer 两种认证方式，并支持 builder customizer。
7. autoconfigure 模块测试覆盖主要条件装配和扩展点。
