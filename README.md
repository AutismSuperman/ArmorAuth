<p align="center">
  <img src="./armorauth-admin-ui/public/brand/logo.svg" alt="ArmorAuth logo" width="720">
</p>

# ArmorAuth

> Shield-first identity infrastructure for modern Spring Security workflows.

ArmorAuth 是一个基于 Spring Security 和 Spring Authorization Server 的认证授权实验项目。当前仓库已经切到 `Spring Boot 4.0.5`、`Spring Security 7.0.4`、`Spring Authorization Server 7.0.4`，并完成了一轮联合登录相关代码的 Maven 模块拆分。

目前仓库已统一使用新的品牌识别：logo 采用“盾牌 + 橡果”组合，强调安全防护、身份认证和项目名里“坚果认证”的主题语义。

项目现在更接近“可演示、可继续开发的认证框架原型”而不是已经封装完成的成品。核心认证授权、JPA 持久化、设备授权、验证码登录、联合登录扩展和服务端页面都在，但管理端、starter 抽象和部分运行时适配仍在收敛中。

## 最近更新

- 已升级到 `Spring Boot 4.0.5`
- 已统一到 `Java 21`
- 已将联合登录模块收敛为 `armorauth-federation` + `armorauth-federation-providers`
- 已理顺 `armorauth-core -> federation` 的反向依赖
- 已为 `armorauth-server` 补充本地 `local` profile，用于无 MySQL 时的 H2 调试
- 已统一服务端与管理端品牌资源，采用最终版橡果盾牌 logo

## 模块结构

| 模块 | 说明 |
| --- | --- |
| `armorauth-common` | 通用基础能力 |
| `armorauth-model` | JPA 实体与 Repository |
| `armorauth-core` | 认证授权核心能力、本地登录、设备授权、JPA 持久化适配 |
| `armorauth-federation` | 联合登录编排、确认页、配置器、安全处理器、provider SPI |
| `armorauth-federation-providers` | QQ / 微信 / Gitee provider 实现与默认元数据 |
| `armorauth` | 对核心模块的聚合封装 |
| `armorauth-server` | 可独立启动的认证服务端 |
| `armorauth-server-ui` | 服务端模板和静态资源 |
| `armorauth-spring-boot` | Spring Boot 聚合模块 |
| `armorauth-spring-boot/armorauth-spring-boot-starter` | 预留 starter 模块 |
| `armorauth-samples` | OIDC、PKCE、`client_credentials` 等样例 |
| `armorauth-admin` | 管理端后端，占位中 |
| `armorauth-admin-ui` | 管理端前端原型，不在 Maven Reactor 中 |

## 当前能力

- OAuth2 Authorization Server / OIDC 基础能力
- JPA 版 `RegisteredClientRepository`
- JPA 版 `OAuth2AuthorizationService`
- JPA 版 `OAuth2AuthorizationConsentService`
- 自定义登录页 `/login`
- 自定义授权确认页 `/consent`
- Device Authorization Flow 页面 `/activate`、`/activated`
- 验证码登录扩展
- 联合登录自动注册 / 中间页确认双策略
- 第三方账号绑定表 `user_federated_binding`

## 技术栈

- Java 21
- Spring Boot 4.0.5
- Spring Security 7.0.4
- Spring Authorization Server 7.0.4
- Spring Data JPA
- MySQL
- H2（仅本地 `local` profile 调试）
- FreeMarker
- Ant Design Vue

## 构建

环境要求：

- JDK 21
- Maven 3.9+ 优先
- Node.js 18+ 仅在开发 `armorauth-admin-ui` 时需要

根目录编译：

```bash
mvn -DskipTests compile
```

如果你的系统 Maven 太旧，至少需要切到支持 Java 21 和 Spring Boot 4 的 Maven 版本。

## 启动服务端

### MySQL 模式

默认配置文件：

```text
armorauth-server/src/main/resources/application.yml
armorauth-server/src/main/resources/application-mysql.yml
```

默认行为：

- 服务端端口 `9000`
- 默认激活 `mysql` profile
- 数据源指向本地 MySQL

数据库脚本：

```text
armorauth-server/src/main/resources/sql/sas-schema.sql
armorauth-server/src/main/resources/sql/sas-data.sql
```

启动前请先确认：

- 数据库地址、用户名、密码
- 第三方 OAuth 客户端配置
- JDK 版本为 21

启动示例：

```bash
mvn -pl armorauth-server -am -DskipTests spring-boot:run
```

### 本地 `local` profile

仓库额外提供了：

```text
armorauth-server/src/main/resources/application-local.yml
```

用途：

- 使用 H2 文件数据库替代 MySQL
- 仅用于本地调试和兼容验证
- 当前配置里默认关闭了联合登录编排链：

```yaml
armorauth:
  federation:
    enabled: false
    default-login-mode: auto
```

原因：

- 当前桌面环境里 JDK / Windows socket 栈异常时，联合登录默认 token client 会在初始化阶段失败
- `local` profile 的目标是先让服务端基础页面和主流程能单机调试

## 样例工程

可运行样例：

- `armorauth-samples/armorauth-samples-oidc-login`
- `armorauth-samples/armorauth-samples-client`
- `armorauth-samples/armorauth-samples-pkce`

默认端口：

- `armorauth-server`: `9000`
- `armorauth-samples-oidc-login`: `8083`
- `armorauth-samples-client`: `8084`
- `armorauth-samples-pkce`: `8085`

样例访问与回调 host 需要保持一致：

- 如果通过 `http://armorauth-demo:8083` 或 `http://armorauth-demo:8085` 访问 sample
- 那么 `redirect_uri` 也必须使用 `armorauth-demo`
- 不要混用 `armorauth-demo` 和 `127.0.0.1`，否则 OAuth2 `state` 对应的 session cookie 会丢失，回调阶段会失败

样例域名仍建议通过 hosts 绑定：

```text
127.0.0.1 armorauth-demo
127.0.0.1 armorauth-server
```

参考文件：

```text
armorauth-samples/hosts/armorauth-hosts
```

## 当前注意事项

### 1. 项目仍偏 demo / prototype

以下模块仍处于占位或未收敛状态：

- `armorauth-admin`
- `armorauth-spring-boot-starter`

### 2. 验证码校验仍是 mock

当前仓库仍提供显式 mock 验证码 Bean，值为：

```text
1234
```

### 3. JWK 仍为运行时动态生成

当前 `AuthorizationServerConfig` 启动时会生成 RSA Key，更适合本地演示，不适合生产直接使用。

### 4. `local` profile 不是生产配置

`application-local.yml` 的目标是便于本地调试，不用于生产环境：

- 使用 H2
- 为了本地兼容验证关闭了 federated login 编排链
- 只适合作为临时调试入口

### 5. 当前机器可能存在系统级网络栈问题

在本次适配过程中，服务端最终启动受过一次环境级阻塞：

- Windows socket 错误 `10106`
- JDK `HttpClient` loopback / Tomcat 端口绑定均可能受影响

如果你在本机遇到这类错误，优先检查系统网络栈，而不是继续怀疑业务代码。

## 推荐阅读顺序

1. `armorauth-core/src/main/java/com/armorauth/config/AuthorizationServerConfig.java`
2. `armorauth-core/src/main/java/com/armorauth/config/DefaultSecurityConfig.java`
3. `armorauth-federation/src/main/java/com/armorauth/federation/config/`
4. `armorauth-federation/src/main/java/com/armorauth/federation/configurer/`
5. `armorauth-federation-providers/src/main/java/com/armorauth/federation/provider/`
6. `armorauth-model/src/main/java/com/armorauth/data/`
7. `armorauth-server/src/main/resources/application*.yml`

## License

Apache License 2.0
