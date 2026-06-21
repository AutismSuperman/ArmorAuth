# Spring Authorization Server Capability Spec and Roadmap

Last reviewed: 2026-06-21

## Background

ArmorAuth currently uses Spring Boot 4.x and `org.springframework.security:spring-security-oauth2-authorization-server` 7.x. Spring Authorization Server has moved into Spring Security 7.0, and the standalone 1.5.x line is the last generation of the former project. New authorization-server features should be tracked primarily from Spring Security documentation while keeping the Spring Authorization Server 1.5.x reference as a compatibility reference.

Primary references:

- Spring Authorization Server reference: <https://docs.spring.io/spring-authorization-server/reference/index.html>
- Spring Authorization Server project notice: <https://spring.io/projects/spring-authorization-server/>
- Spring Security OAuth2 Authorization Server reference: <https://docs.spring.io/spring-security/reference/servlet/oauth2/authorization-server/index.html>
- Spring Authorization Server source mirror reviewed locally: `.codex-temp/spring-authorization-server-main`

## Current ArmorAuth Baseline

The project already covers a broad authorization-server surface:

- OAuth2/OIDC core flows: authorization code, refresh token, client credentials, device code, discovery, JWKS, introspection, revocation, OIDC logout, and userinfo.
- Persistent core services: custom JPA `RegisteredClientRepository`, `OAuth2AuthorizationService`, `OAuth2AuthorizationConsentService`, and persistent JWK source.
- Hosted identity pages: login, consent, MFA, device activation, activation result, and federation confirmation.
- Admin surface: application, scope, user, role, permission, tenant, organization, identity provider, audit, token statistics, webhook, JWK, and secret-protection management.
- Security extensions: captcha, TOTP/MFA, Passkey/WebAuthn, passwordless login, OAuth2/OIDC federation, SAML SP login, LDAP/AD bind login, SCIM, actions, and webhooks.

Important gaps are mostly not basic OAuth features. They are product hardening and enterprise-grade protocol capabilities:

- Spring Security 7.x API/style alignment is incomplete.
- PAR is not productized.
- mTLS options appear in the admin UI but lack full persisted settings and operational guidance.
- DPoP-bound access tokens are not exposed or verified in the local starter/resource-server path.
- Dynamic client registration is not exposed as a controlled standard protocol endpoint.
- Tenant records exist, but authorization-server issuer/JWK/client/authorization storage is still effectively single-issuer.
- High-availability session and core-service strategy needs a clearer implementation path.

## Implementation Status

Status as of 2026-06-21:

| Capability | Status | Implemented Artifacts | Verification |
| --- | --- | --- | --- |
| Spring Security 7.x alignment | Partially complete | Existing 7.0.4 baseline retained; current SAS config explicitly enables PAR; DCR is feature-flagged | OAuth/OIDC E2E passes on current dependency line |
| PAR | Complete for optional PAR | `/oauth2/par` enabled; discovery assertions added; confidential authorization-code flow through `request_uri` covered | `OAuth2OidcFlowE2eTest` |
| mTLS settings | Backend/UI complete; live TLS handshake requires deployment validation | `x509CertificateSubjectDN`, `x509CertificateBoundAccessTokens`, migrations, DTO/API/UI validation, SAS settings mapping | `ClientTransformUtilTest`, compile/E2E |
| DPoP token issuance | Authorization-server issuance covered | DPoP proof on token request returns `token_type=DPoP`; JWT access token contains `cnf.jkt` | `OAuth2OidcFlowE2eTest` |
| Dynamic client registration | Controlled endpoint complete; admin-visible | Feature flag `armorauth.authorization-server.dynamic-client-registration.enabled`; `/connect/register`; registrar token flow; audit event `CLIENT_REGISTRATION_CREATED`; application list shows registration source and DCR registrar clients | `OAuth2OidcFlowE2eTest` |
| Redis/HA sessions | Optional profile added | `spring-session-data-redis`, Redis starter, `application-ha-redis.yml`; OAuth/SAS state remains JPA | Compile; Redis runtime smoke test still required |
| Tenant-aware issuers | Path issuer baseline complete; admin-visible | `multiple-issuers.enabled`; `/t/{tenantCode}` issuer routing; tenant-scoped clients, authorizations, consents, and JWKs; tenant page shows issuer status and application page can filter/create by tenant | `OAuth2OidcFlowE2eTest` |
| Admin integration | Complete for DCR/DPoP/path issuer controls | Application management shows tenant/source/DPoP status, supports DPoP policy fields and DCR registrar switch; tenant management shows issuer path status | Maven compile, admin UI build |

New defaults and flags:

- DCR is disabled by default and enabled with `armorauth.authorization-server.dynamic-client-registration.enabled=true`.
- Tenant path issuers are disabled by default and enabled with `armorauth.authorization-server.multiple-issuers.enabled=true`.
- Redis-backed servlet sessions are opt-in with the `ha-redis` profile. Registered clients, authorizations, consents, JWKs, and device/PAR state remain in JPA.
- mTLS certificate-bound access tokens are a per-client token setting and should be enabled only with `tls_client_auth` or `self_signed_tls_client_auth`.
- DPoP admin policy fields are stored on `oauth2_client_settings`; current SAS DPoP proof validation and token binding remain upstream-driven. Enforcing `dpopRequired` on missing token-request proofs is a follow-up token-endpoint validator task.
- DCR-created clients are marked with `registration_source=DYNAMIC_REGISTRATION`; admin-created clients use `ADMIN`.

## Design Principles

- Keep SAS/Spring Security protocol behavior close to upstream defaults unless ArmorAuth has a clear product reason to customize.
- Treat admin UI switches as contractual product behavior: if the UI exposes a protocol option, backend persistence, validation, docs, and tests must support it.
- Prefer feature flags for advanced protocol capabilities so local development remains simple.
- Keep enterprise features auditable. Client registration, certificate changes, DPoP enablement, issuer changes, and token-policy changes must emit admin/audit events.
- Avoid mixing tenant branding with tenant issuer isolation. Branding can be lightweight; issuer isolation changes protocol identities and must be designed as a separate capability.

## Target Capability Matrix

| Capability | Current State | Target State | Priority |
| --- | --- | --- | --- |
| Spring Security 7.x alignment | Uses 7.0.4, partially old config style | Upgrade path to 7.1.x, use current config API, add compatibility tests | P0 |
| PAR | Endpoint available upstream, not productized locally | `/oauth2/par` enabled, documented, tested, visible in discovery | P0 |
| mTLS client auth | UI lists methods, backend settings incomplete | Full `tls_client_auth` and `self_signed_tls_client_auth` lifecycle | P1 |
| Certificate-bound access tokens | Not modeled | Per-client token setting with JWT/introspection confirmation claims | P1 |
| DPoP-bound access tokens | Not modeled | Optional per-client sender-constrained token support and resource-server verification | P1 |
| Dynamic client registration | Admin API exists, standard endpoint not exposed | Controlled OAuth2/OIDC DCR with registrar clients and audit | P2 |
| Multi-issuer tenancy | Tenant/org model exists, issuer not isolated | Optional path/domain based issuer isolation | P2/P3 |
| Redis/HA core services | JPA core services, in-memory servlet sessions likely | Optional Spring Session/Redis and token-state HA guidance | P2 |

## Feature Specs

### 1. Spring Security 7.x Alignment

Goal: keep ArmorAuth on the supported Spring Security authorization-server line and reduce future migration cost.

Functional requirements:

- Upgrade `spring-security-oauth2-authorization-server` from `7.0.4` to the current compatible 7.1.x line after dependency verification.
- Migrate authorization-server configuration toward the current `http.oauth2AuthorizationServer(...)` style where practical.
- Keep existing endpoint paths and hosted page behavior unchanged.
- Keep current customizations: custom consent page, device verification pages, device client authentication, JPA repositories, persistent JWK source, OIDC enablement, and resource-server JWT support.

Technical tasks:

- Review Boot 4.0.x dependency compatibility with Spring Security 7.1.x.
- Run focused compilation for `armorauth-core`, `armorauth-server`, and sample modules.
- Update code where old `OAuth2AuthorizationServerConfigurer` package/style is deprecated or incompatible.
- Add a migration note in deployment or implementation docs.

Acceptance criteria:

- Existing OAuth/OIDC E2E tests pass.
- Discovery, JWKS, token, introspection, revocation, userinfo, logout, and device flow remain backward compatible.
- No admin API contract changes are required.

### 2. Pushed Authorization Request (PAR)

Goal: add high-security OAuth/OIDC authorization request handling for clients that need request integrity, reduced front-channel leakage, and Financial-grade API style readiness.

Functional requirements:

- Enable and test the Pushed Authorization Request endpoint at `/oauth2/par`.
- Discovery metadata must expose `pushed_authorization_request_endpoint`.
- Authorization requests may use `request_uri` returned from PAR.
- Pushed request URIs must be one-time use and expire according to upstream behavior.
- PAR failures must return standard OAuth error responses.

Admin/API requirements:

- Add optional application-level field `requirePushedAuthorizationRequests` only if upstream supports a per-client enforcement hook or if ArmorAuth adds validation.
- If enforcement is not implemented in the first release, document PAR as supported but optional.
- Add audit events for PAR request creation only if token/authorization audit volume is acceptable; otherwise rely on aggregate metrics.

Data model:

- No new table is expected for basic PAR because SAS persists pushed authorization requests through `OAuth2AuthorizationService`.
- Verify current `Authorization` entity can round-trip attributes used by `OAuth2AuthorizationRequest`.

Tests:

- PAR request success with confidential client.
- Authorization using returned `request_uri`.
- One-time use rejection.
- Expired `request_uri` rejection.
- Invalid client or redirect URI rejection.
- Discovery metadata contains PAR endpoint.

Acceptance criteria:

- A compliant client can complete authorization code + PKCE using PAR.
- Existing non-PAR authorization requests continue to work.

### 3. mTLS Client Authentication and Certificate-Bound Tokens

Goal: make `tls_client_auth` and `self_signed_tls_client_auth` production-ready instead of UI-only options.

Functional requirements:

- Support `tls_client_auth` with expected subject DN validation.
- Support `self_signed_tls_client_auth` using the client's JWK Set URL and certificate chain.
- Optionally bind access tokens to the client certificate for mTLS clients.
- Support both self-contained JWT and reference token formats.

Admin/API requirements:

- Extend application settings:
  - `x509CertificateSubjectDN`
  - `x509CertificateBoundAccessTokens`
  - certificate display metadata, if available: subject, issuer, serial number, thumbprint, not-before, not-after
- Add UI validation:
  - `tls_client_auth` requires `x509CertificateSubjectDN`.
  - `self_signed_tls_client_auth` requires `jwkSetUrl`.
  - certificate-bound access tokens should only be enabled for mTLS methods.
- Add audit events for certificate subject/JWK URL changes.

Data model:

- Add `oauth2_client_settings.x509_certificate_subject_dn`.
- Add `oauth2_token_settings.x509_certificate_bound_access_tokens`.
- Map both fields in `ClientTransformUtil`.
- Update MySQL/H2 migrations and seed data.

Deployment requirements:

- Document reverse proxy/Tomcat certificate forwarding requirements.
- Document how `jakarta.servlet.request.X509Certificate` reaches the authorization server.
- Provide Nginx/Ingress examples for client certificate verification and forwarding.

Tests:

- Client authentication with valid `tls_client_auth`.
- Rejection when subject DN mismatches.
- `self_signed_tls_client_auth` with matching JWK certificate.
- JWT access token contains certificate confirmation claim when certificate-bound access tokens are enabled.
- Introspection response exposes confirmation data for reference tokens.

Acceptance criteria:

- Admin-created mTLS clients can authenticate at token/introspection/revocation endpoints.
- Misconfigured mTLS clients fail with standard OAuth errors and useful admin validation messages.

### 4. DPoP-Bound Access Tokens

Goal: reduce impact of leaked access tokens by allowing proof-of-possession access tokens for browser/native/public clients and selected confidential clients.

Functional requirements:

- Accept valid DPoP proof JWTs on token requests.
- Return `token_type=DPoP` for DPoP-bound access tokens.
- Include `cnf.jkt` in JWT access tokens or expose equivalent confirmation data through introspection.
- Reject malformed, replayed, wrong-method, wrong-URL, or unsupported-algorithm DPoP proofs.
- Add resource-server starter support to validate DPoP proofs for protected API requests.

Admin/API requirements:

- Add per-client policy:
  - `dpopEnabled`
  - `dpopRequired`
  - `dpopAllowedAlgorithms`
  - optional `dpopProofMaxClockSkewSeconds`
- For public SPA/native clients, recommend `dpopRequired=true` when resource servers support it.
- Show DPoP state in application management and validate `dpopRequired` only when `dpopEnabled=true`.

Data model:

- Current normalized settings model stores DPoP policy in `oauth2_client_settings`:
  - `dpop_enabled`
  - `dpop_required`
  - `dpop_allowed_algorithms`

Operational requirements:

- Add replay cache for DPoP `jti`. Redis is preferred for multi-node deployments.
- Add metrics for DPoP validation failures by reason.

Tests:

- Token request with valid DPoP proof.
- Protected resource request with valid DPoP proof using starter/resource-server sample.
- Rejection for missing proof when required.
- Rejection for proof replay.
- Rejection for access token/proof key mismatch.

Acceptance criteria:

- A DPoP-enabled sample client can obtain and use a DPoP-bound access token end to end.
- Resource server starter documents and verifies DPoP-bound tokens.

### 5. OAuth2/OIDC Dynamic Client Registration

Goal: expose standards-based client onboarding for platform integrations while preserving ArmorAuth governance, audit, and tenant controls.

Functional requirements:

- Enable OAuth2 Client Registration endpoint `/oauth2/register` and/or OIDC Client Registration endpoint `/connect/register`.
- Require registrar clients with `client.create`; require `client.read` for read operations.
- Support one-time initial access token behavior according to upstream semantics where applicable.
- Apply ArmorAuth validation for redirect URI, grant type, client authentication method, allowed scopes, and tenant ownership.
- Emit audit events for dynamic client creation and read operations.

Admin/API requirements:

- Add registrar client type in application management.
- Show registration source in application list: `ADMIN` or `DYNAMIC_REGISTRATION`.
- Mark registrar clients when they carry both `client.create` and `client.read`.
- Add allowed dynamic registration policy:
  - allowed grant types
  - allowed scopes
  - allowed redirect URI patterns
  - default token settings
  - tenant binding
- Show dynamically registered clients in the normal application list with source `DYNAMIC_REGISTRATION`.

Data model:

- Add fields to `oauth2_client` or settings:
  - `registration_source`
  - `registration_access_token_hash`, if ArmorAuth stores registration tokens outside upstream authorization state
  - `tenant_id`, if dynamic registration is tenant-scoped
  - optional `contacts`, `logo_uri`, `client_uri`, `policy_uri`, `tos_uri`

Tests:

- Registrar obtains `client.create` token.
- Client registration succeeds with valid metadata.
- Registration fails with invalid redirect URI or disallowed scope.
- Registered client can perform authorization code + PKCE.
- Client read requires `client.read`.
- Audit event is recorded.

Acceptance criteria:

- A standards-compliant integration can self-register under an admin-defined policy without bypassing ArmorAuth governance.

### 6. Tenant-Aware Issuer Model

Goal: promote existing tenant records from administrative grouping to protocol-level issuer isolation when needed by enterprise customers.

Functional requirements:

- Support optional multiple issuers per host using path routing.
- Resolve issuer from request path and tenant configuration.
- Isolate registered clients, authorizations, consents, and JWKs by issuer/tenant.
- Maintain current single-issuer behavior as the default.
- Keep domain-based issuer routing, tenant branding, and login-policy isolation as follow-up enterprise enhancements.

Routing options:

- Path issuer: `https://auth.example.com/t/{tenantCode}`
- Domain issuer: `https://{tenantCustomDomain}`
- Hybrid: default issuer plus opt-in tenant issuers.

Data model:

- Reuse existing `tenant.tenant_code` for the path issuer segment.
- Add `tenant_id` to OAuth client, authorization, consent, and JWK key tables.
- Existing global unique constraints remain for `client_id` and `kid`; relaxing them to tenant-scoped uniqueness is a future migration.

Technical tasks:

- Enable multiple issuers in `AuthorizationServerSettings` only for tenant-aware deployments.
- Validate `/t/{tenantCode}` requests against enabled tenants before SAS endpoint handling.
- Show tenant issuer path and enablement state in tenant management.
- Allow application management to filter and create clients by tenant.
- Implement tenant-aware component lookup:
  - `RegisteredClientRepository`
  - `OAuth2AuthorizationService`
  - `OAuth2AuthorizationConsentService`
  - `JWKSource<SecurityContext>`
- Update hosted pages to resolve tenant branding from issuer rather than only default branding in a later UI/branding pass.

Tests:

- Discovery for default and tenant issuers returns issuer-specific metadata.
- JWKS differs or is correctly scoped per tenant.
- Default issuer clients cannot be used with tenant issuer token endpoints, and tenant clients cannot be used with the default issuer.
- Tenant-issued access tokens contain the tenant issuer claim.
- Authorization from tenant A cannot be introspected or revoked by tenant B client.
- Hosted login/consent pages render tenant-specific branding in a later UI/branding pass.

Acceptance criteria:

- Multi-issuer mode can be enabled without breaking single-issuer deployments.
- Protocol data is isolated by tenant and verified by E2E tests.

### 7. Redis and High Availability Support

Goal: make multi-node deployments predictable for authorization state, login sessions, WebAuthn/MFA challenges, DPoP replay cache, and operational consistency.

Functional requirements:

- Provide optional Spring Session Redis support for hosted login sessions.
- Define which authorization-server state remains in JPA and which state may use Redis.
- Add Redis-backed DPoP replay cache when DPoP is enabled.
- Add cleanup jobs for expired authorization/device/PAR state if JPA remains the source of truth.

Implementation options:

- Conservative path: keep `RegisteredClient`, `Authorization`, and `Consent` in JPA; use Redis only for HTTP sessions, challenge state, rate limits, and replay cache.
- Advanced path: add optional Redis implementations for core SAS services following upstream guide patterns.

Tests:

- Session survives node switch when Spring Session Redis is enabled.
- MFA/WebAuthn challenge state survives node switch.
- Device and PAR flows work across nodes.
- DPoP replay detection works across nodes.

Acceptance criteria:

- Deployment docs clearly state the supported HA topology and required infrastructure.

## Cross-Cutting Requirements

### Security

- All new client settings must be validated on write.
- Client secrets and registration access tokens must never be logged in plaintext.
- Certificate and DPoP thumbprints may be logged only as non-secret identifiers.
- Dynamic registration must be disabled by default.
- PAR, mTLS, DPoP, and tenant issuer features must have explicit negative tests.

### Audit and Observability

Add or extend audit event types:

- `CLIENT_REGISTRATION_CREATED`
- `CLIENT_REGISTRATION_READ`
- `CLIENT_CERTIFICATE_POLICY_UPDATED`
- `CLIENT_DPOP_POLICY_UPDATED`
- `TENANT_ISSUER_UPDATED`
- `PAR_REQUEST_ACCEPTED`, only if volume is acceptable
- `TOKEN_DPOP_VALIDATION_FAILED`, likely metric-first rather than audit-first

Metrics to add:

- Token requests by grant type and client authentication method.
- PAR success/failure counts.
- mTLS authentication failures by reason.
- DPoP validation failures by reason.
- Dynamic client registration success/failure counts.
- Tenant issuer request counts.

### Documentation

Add documentation for:

- Spring Security 7.x migration and supported versions.
- PAR client integration.
- mTLS certificate setup behind reverse proxies.
- DPoP client and resource-server integration.
- Dynamic client registration governance.
- Tenant issuer deployment modes.
- HA deployment topology with Redis.

## Development Roadmap

### Phase 0: Baseline Hardening

Scope:

- Confirm dependency compatibility for Spring Security 7.1.x.
- Add focused protocol smoke tests around existing behavior before broad changes.
- Document current SAS/Spring Security version policy.

Deliverables:

- Dependency upgrade PR or explicit decision to stay on 7.0.x temporarily.
- Passing OAuth/OIDC E2E baseline.
- Migration note in docs.

Recommended duration: 1 sprint.

### Phase 1: PAR and Current Configuration Alignment

Scope:

- Migrate authorization-server config style where feasible.
- Enable and test `/oauth2/par`.
- Add discovery assertions for PAR.
- Add optional admin-visible capability indicator.

Deliverables:

- PAR E2E tests.
- Client integration doc.
- No behavior regression for existing authorization code and PKCE flows.

Recommended duration: 1 sprint.

### Phase 2: mTLS Productization

Scope:

- Add certificate subject DN and certificate-bound token settings.
- Extend admin API/UI validation.
- Add migrations and seed-safe defaults.
- Add reverse proxy and certificate forwarding docs.

Deliverables:

- mTLS client auth E2E tests.
- Certificate-bound access token tests for JWT and/or introspection.
- Admin audit events for certificate policy changes.

Recommended duration: 1 to 2 sprints.

### Phase 3: DPoP End-to-End

Scope:

- Add per-client DPoP policy.
- Add DPoP proof validation tests for token issuance.
- Add resource-server starter/sample support.
- Add Redis-backed replay cache if HA mode is in scope.

Deliverables:

- DPoP-enabled sample client/resource flow.
- DPoP validation metrics.
- Resource-server integration guide.

Recommended duration: 2 sprints.

### Phase 4: Controlled Dynamic Client Registration

Scope:

- Enable standard registration endpoint behind a feature flag.
- Add registrar client and registration policy model.
- Add tenant-aware restrictions if tenant ownership is required.
- Add audit and admin visibility.

Deliverables:

- Dynamic registration E2E tests.
- Admin policy UI/API.
- Registration governance documentation.

Recommended duration: 2 sprints.

### Phase 5: Tenant-Aware Issuers and HA

Scope:

- Decide path-based, domain-based, or hybrid issuer model.
- Add tenant-scoped OAuth data model.
- Implement tenant-aware core component lookup.
- Add Redis-backed session/challenge/replay support.

Deliverables:

- Multi-issuer E2E suite.
- Migration plan for existing single-issuer deployments.
- HA deployment guide and smoke tests.

Recommended duration: 3+ sprints. This is the highest-risk phase and should be split into design, migration, and implementation PRs.

## Recommended Implementation Order

1. Upgrade/alignment and baseline tests.
2. PAR.
3. mTLS settings and certificate-bound tokens.
4. DPoP.
5. Dynamic client registration.
6. Tenant-aware issuers and HA.

This order keeps early work close to upstream defaults, converts already visible UI options into real supported behavior, and defers protocol-level tenancy until the product has stronger test coverage around issuer isolation.

## Open Decisions

- Should the project standardize on Spring Security 7.1.x immediately, or stay on 7.0.x until Boot 4.0.x dependency alignment is confirmed?
- Should PAR be optional only, or should ArmorAuth support per-client PAR-required enforcement?
- Should dynamic registration be tenant-scoped from day one?
- Should multi-issuer tenancy use path issuers, custom domains, or both?
- Should Redis become required for HA deployments, or remain optional with a single-node default?
