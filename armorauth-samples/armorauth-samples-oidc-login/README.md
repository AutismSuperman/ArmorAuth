# ArmorAuth Spring Boot OIDC Login Sample

This sample uses Spring Security OAuth2 Client to log in through the default ArmorAuth issuer.

## Run

The default `test` profile uses the existing `autism-client-oidc` registration.

```powershell
mvn -pl armorauth-samples/armorauth-samples-oidc-login spring-boot:run
```

Tenant issuer testing is kept in the separate `armorauth-samples-tenant-oidc-login` module.
