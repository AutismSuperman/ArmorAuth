# ArmorAuth Spring Boot PKCE Sample

This sample runs Authorization Code + PKCE as a Spring Boot OAuth2 client.

It replaces the old React SPA PKCE sample with the same public-client semantics:

- client authentication method: `none`
- authorization grant: `authorization_code`
- PKCE: required
- default sample client: `spring-pkce`
- tenant sample client: `tenant-demo-pkce`

## Run default issuer

```powershell
mvn -pl armorauth-samples/armorauth-samples-spring-pkce spring-boot:run
```

Open `http://localhost:8086`.

Registered callback:

```text
http://localhost:8086/login/oauth2/code/spring-pkce
```

## Run tenant issuer

```powershell
$env:ARMORAUTH_SAMPLE_REGISTRATION_ID='tenant-demo-pkce'; mvn -pl armorauth-samples/armorauth-samples-spring-pkce spring-boot:run
```

Open `http://localhost:8086`.

Registered callback:

```text
http://localhost:8086/login/oauth2/code/tenant-demo-pkce
```

Useful overrides:

```powershell
$env:ARMORAUTH_SAMPLE_PORT='8086'
$env:ARMORAUTH_SAMPLE_ISSUER='http://localhost:9000'
$env:ARMORAUTH_SAMPLE_TENANT_ISSUER='http://localhost:9000/t/demo'
$env:ARMORAUTH_SAMPLE_REGISTRATION_ID='spring-pkce'
```
