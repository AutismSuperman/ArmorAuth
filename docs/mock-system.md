# ArmorAuth Mock System

这套 mock profile 用 H2 本地库启动完整服务端，并补充用户、租户、组织、OAuth2 应用、身份源、Webhook、审计事件和 token 统计数据。

## 启动服务端

```powershell
$env:SystemRoot='C:\WINDOWS'; $env:windir='C:\WINDOWS'; $env:COMSPEC='C:\WINDOWS\system32\cmd.exe'; $env:HTTP_PROXY='http://127.0.0.1:7897'; $env:HTTPS_PROXY='http://127.0.0.1:7897'; $env:ALL_PROXY='http://127.0.0.1:7897'; $env:JAVA_HOME="$env:USERPROFILE\.jdks\temurin-21.0.11"; $env:MAVEN_HOME="$env:USERPROFILE\.maven\apache-maven-3.9.9"; $env:PATH="$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:PATH"; $env:MAVEN_OPTS='-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897'; mvn -pl armorauth-server -am spring-boot:run "-Dspring-boot.run.profiles=mock"
```

服务默认监听 `http://localhost:9000`，H2 文件库在 `.runtime/h2/identity_server_mock`。

## Mock 账号

所有 mock 账号密码都是 `admin123`：

| 用户名 | 手机号 | 角色 |
| --- | --- | --- |
| `admin` | `13103777777` | 超级管理员 |
| `app.manager` | `13900000001` | 应用管理员 |
| `audit.viewer` | `13900000002` | 审计查看者 |
| `demo.user` | `13900000003` | 普通用户 |

mock profile 会开启 `armorauth.captcha.sms.expose-code=true`，短信验证码登录时页面会显示本次 OTP。

## 样例测试

Java samples 要从 samples 父 POM 跑，根目录 `-pl armorauth-samples` 只会跑聚合 POM：

```powershell
$env:SystemRoot='C:\WINDOWS'; $env:windir='C:\WINDOWS'; $env:COMSPEC='C:\WINDOWS\system32\cmd.exe'; $env:HTTP_PROXY='http://127.0.0.1:7897'; $env:HTTPS_PROXY='http://127.0.0.1:7897'; $env:ALL_PROXY='http://127.0.0.1:7897'; $env:JAVA_HOME="$env:USERPROFILE\.jdks\temurin-21.0.11"; $env:MAVEN_HOME="$env:USERPROFILE\.maven\apache-maven-3.9.9"; $env:PATH="$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:PATH"; $env:MAVEN_OPTS='-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897'; mvn -f armorauth-samples/pom.xml test
```

React PKCE sample 使用 `react-spa-pkce` 客户端：

```powershell
$env:SystemRoot='C:\WINDOWS'; $env:windir='C:\WINDOWS'; $env:COMSPEC='C:\WINDOWS\system32\cmd.exe'; $env:HTTP_PROXY='http://127.0.0.1:7897'; $env:HTTPS_PROXY='http://127.0.0.1:7897'; $env:ALL_PROXY='http://127.0.0.1:7897'; cd armorauth-samples/armorauth-samples-react-pkce; npm install; npm run build
```
