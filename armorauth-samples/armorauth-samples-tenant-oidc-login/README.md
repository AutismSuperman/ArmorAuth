# ArmorAuth Tenant Spring Boot OIDC Login Sample

This sample uses Spring Security OAuth2 Client to log in through a tenant path issuer.

The server migrations seed:

- Tenant: `tenant-demo`, tenant code `demo`
- Client: `tenant-demo-spring`
- Client secret: `secret`
- Issuer: `http://localhost:9000/t/demo`
- Redirect URI: `http://localhost:8083/login/oauth2/code/tenant-demo`
- Post logout redirect URI: `http://localhost:8083/`

Run ArmorAuth with tenant path issuers enabled. The `mock` profile enables this automatically. For other profiles, set:

```properties
armorauth.authorization-server.multiple-issuers.enabled=true
```

Start this tenant sample:

```powershell
mvn -pl armorauth-samples/armorauth-samples-tenant-oidc-login spring-boot:run
```

Optional overrides:

```powershell
$env:ARMORAUTH_SAMPLE_ISSUER='http://localhost:9000/t/demo'
$env:ARMORAUTH_SAMPLE_CLIENT_ID='tenant-demo-spring'
$env:ARMORAUTH_SAMPLE_CLIENT_SECRET='secret'
```
