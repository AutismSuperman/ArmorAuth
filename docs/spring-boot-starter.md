# Spring Boot Starter Integration

`armorauth-spring-boot-starter` is intended for relying Spring Boot services that need to integrate with ArmorAuth as an OAuth2/OIDC identity provider. It provides lightweight auto-configuration for resource servers and OIDC login clients without bringing in the full ArmorAuth authorization server implementation.

For design boundaries and extension points, see [Spring Boot Starter Extension Spec](spring-boot-starter-extension-spec.md).

## Maven Dependency

```xml
<dependency>
    <groupId>com.armorauth</groupId>
    <artifactId>armorauth-spring-boot-starter</artifactId>
    <version>1.0.0</version>
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

The default filter chain protects `/api/**`, permits `/api/public/**`, maps `roles` to `ROLE_*`, maps `scope`/`scp` to `SCOPE_*`, maps `permissions` to `PERMISSION_*`, and maps `org_roles` to `ORG_ROLE_*`.

The defaults can be adjusted without replacing the whole filter chain:

```yaml
armorauth:
  resource-server:
    enabled: true
    security-matcher: /api/**
    permit-all:
      - /api/public/**
      - /api/health
    csrf-enabled: false
    principal-claim: preferred_username
    role-claims:
      - roles
      - groups
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

It also supports lightweight login/logout tuning:

```yaml
armorauth:
  oidc-client:
    enabled: true
    permit-all:
      - /
      - /public/**
      - /assets/**
    csrf-enabled: false
    default-success-url: /dashboard
    logout-success-url: /
```

For browser-only or mobile public clients, use Authorization Code + PKCE in ArmorAuth and configure Spring Security as a public client. The sample implementation uses Spring Security's `DefaultOAuth2AuthorizationRequestResolver` together with `OAuth2AuthorizationRequestCustomizers.withPkce()`; setting `client-authentication-method: none` alone is not enough to guarantee PKCE is sent.

## Custom Security

If the application defines its own `JwtAuthenticationConverter`, the resource server auto-configuration uses the custom converter. If the application defines a bean named `resourceServerSecurityFilterChain` or `oidcClientSecurityFilterChain`, the corresponding starter managed chain backs off.

For small changes, prefer customizers instead of replacing the whole chain:

```java
@Bean
ArmorAuthResourceServerHttpSecurityCustomizer apiSecurity() {
    return http -> http.exceptionHandling(ex -> ex.authenticationEntryPoint(customEntryPoint()));
}

@Bean
ArmorAuthOidcClientHttpSecurityCustomizer loginSecurity() {
    return http -> http.oauth2Login(oauth2 -> oauth2.failureHandler(customFailureHandler()));
}

@Bean
ArmorAuthJwtAuthenticationConverterCustomizer jwtPrincipal() {
    return converter -> converter.setPrincipalClaimName("preferred_username");
}
```

## Current User Context

The starter exposes `ArmorAuthCurrentUserResolver` so business code can read ArmorAuth claims consistently:

```java
@RestController
class ProfileController {

    private final ArmorAuthCurrentUserResolver currentUserResolver;

    ProfileController(ArmorAuthCurrentUserResolver currentUserResolver) {
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping("/api/me")
    ArmorAuthCurrentUser me() {
        return currentUserResolver.currentUser();
    }
}
```

The resolver reads `sub`, `preferred_username` / `username` / `name`, `tenant_id`, `org_ids`, `org_roles`, `roles`, `scope` / `scp`, and `permissions` from JWT or OIDC user attributes.

## Admin API Client

The starter can create a lightweight `RestClient` based Admin API client:

```yaml
armorauth:
  admin-client:
    enabled: true
    base-url: http://localhost:9000
    username: admin
    password: admin123
```

`base-url` is required when the Admin Client is enabled. If both `bearer-token` and `username` / `password` are configured, bearer token authentication takes precedence.

Or use a bearer token:

```yaml
armorauth:
  admin-client:
    enabled: true
    base-url: http://localhost:9000
    bearer-token: ${ARMORAUTH_ADMIN_TOKEN}
```

```java
@Service
class ApplicationProvisioningService {

    private final ArmorAuthAdminRestClient adminClient;

    ApplicationProvisioningService(ArmorAuthAdminRestClient adminClient) {
        this.adminClient = adminClient;
    }

    Map<String, Object> applications() {
        return adminClient.listApplications(0, 20);
    }
}
```

Customize the underlying builder when you need proxy, timeout, tracing, or extra headers:

```java
@Bean
ArmorAuthAdminRestClientCustomizer adminClientCustomizer() {
    return builder -> builder.defaultHeader("X-Source", "billing-service");
}
```

## Token Relay

`ArmorAuthTokenRelayInterceptor` is exposed as a bean. Attach it only to `RestClient` instances that call trusted downstream services:

```java
@Bean
RestClient downstreamClient(RestClient.Builder builder, ArmorAuthTokenRelayInterceptor tokenRelay) {
    return builder
            .baseUrl("http://orders-service")
            .requestInterceptor(tokenRelay)
            .build();
}
```

The interceptor forwards the current `JwtAuthenticationToken` bearer token and does not overwrite an existing `Authorization` header. It is intended for service-to-service calls where the downstream service trusts the same ArmorAuth issuer. It is not a replacement for OAuth2 Client token acquisition, and it does not automatically relay an OIDC Login `OAuth2AuthorizedClient` access token.

## Extension Boundaries

The starter is deliberately thin:

- It does not start an ArmorAuth authorization server inside the business application.
- It does not manage tenants, clients, users or MFA data locally.
- It backs off when the application defines its own named security filter chains or converter beans.
- It exposes customizers for small changes and lets applications replace the full Spring Security chain when needed.

The sample applications under `armorauth-samples` are intentionally close to native Spring Security usage. They are useful for protocol verification; production services should prefer the starter when they need the shared ArmorAuth claim mapping, current-user resolver, Admin Client or token relay behavior.
