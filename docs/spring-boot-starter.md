# Spring Boot Starter Integration

`armorauth-spring-boot-starter` is intended for relying Spring Boot services that need to integrate with ArmorAuth as an OAuth2/OIDC identity provider. It provides lightweight auto-configuration for resource servers and OIDC login clients without bringing in the full ArmorAuth authorization server implementation.

## Maven Dependency

```xml
<dependency>
    <groupId>com.armorauth</groupId>
    <artifactId>armorauth-spring-boot-starter</artifactId>
    <version>0.0.1</version>
</dependency>
```

## Resource Server

Enable the resource server auto-configuration and point Spring Security at the ArmorAuth issuer or JWK set.

```yaml
armorauth:
  resource-server:
    enabled: true

spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:9000
```

The default filter chain protects `/api/**`, permits `/api/public/**`, maps `roles` to `ROLE_*`, and maps `scope` to `SCOPE_*`.

Use `jwk-set-uri` instead of `issuer-uri` when the service cannot reach the issuer metadata endpoint:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: http://localhost:9000/oauth2/jwks
```

## OIDC Login Client

Enable the OIDC client auto-configuration and configure a standard Spring Security OAuth2 client registration.

```yaml
armorauth:
  oidc-client:
    enabled: true

spring:
  security:
    oauth2:
      client:
        registration:
          armorauth:
            client-id: sample-client
            client-secret: sample-secret
            scope: openid,profile,email
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
        provider:
          armorauth:
            issuer-uri: http://localhost:9000
```

The default filter chain permits `/` and `/public/**`, requires authentication for other requests, enables OAuth2 login, and disables CSRF for simple service integration.

## Custom Security

If the application defines its own `SecurityFilterChain`, ArmorAuth's default resource server or OIDC client filter chain backs off. If the application defines its own `JwtAuthenticationConverter`, the resource server auto-configuration uses the custom converter.
