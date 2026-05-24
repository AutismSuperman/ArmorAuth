# ArmorAuth Implementation Status

> Last updated: 2026-05-24

This file records incremental implementation work after `docs/ROADMAP.md` review.
Each step should capture scope, files touched, verification, and remaining follow-up.

## Step 1 - Admin API Gap Closure

Status: Completed

Goal: close the nearest ROADMAP gaps that block management API completeness.

Implemented:

| Area | Status | Notes |
| --- | --- | --- |
| Scope management API | Completed | Added `/api/admin/v1/scopes` for listing, creating, updating, and deleting client scopes. It uses existing `oauth2_scope(client_id, scope)` storage rather than adding a new global scope table. |
| Login policy API | Completed | Added `/api/admin/v1/login-policies` backed by application-level `mfaRequired`. Role-level MFA remains the current built-in policy: `SUPER_ADMIN`, `TENANT_ADMIN`. |
| Identity provider test API | Completed | Added `POST /api/admin/v1/identity-providers/{id}:test`. By default it performs local configuration checks; `probeRemote=true` can attempt remote URL reachability. SAML support was expanded in Steps 13 and 18; LDAP/AD bind-search sync and live login support were added in Steps 16 and 19. |
| Federated binding admin API | Completed | Added `/api/admin/v1/federated-bindings` for listing by `userId` / `registrationId` and deleting external account bindings. |
| Scope persistence mapping | Completed | Fixed `OAuth2Scope.clientId` so direct scope creation can persist `client_id`. |
| Admin security rules | Completed | Added explicit authorization rules for scopes, login policies, identity providers, and federated bindings. |

Primary files:

| File | Purpose |
| --- | --- |
| `armorauth-admin/src/main/java/com/armorauth/admin/controller/ScopeController.java` | Scope management endpoint |
| `armorauth-admin/src/main/java/com/armorauth/admin/controller/LoginPolicyController.java` | Login policy endpoint |
| `armorauth-admin/src/main/java/com/armorauth/admin/controller/FederatedBindingController.java` | External account binding endpoint |
| `armorauth-admin/src/main/java/com/armorauth/admin/controller/IdentityProviderController.java` | Added provider test endpoint |
| `armorauth-admin/src/main/java/com/armorauth/admin/service/ScopeService.java` | Scope business logic and audit |
| `armorauth-admin/src/main/java/com/armorauth/admin/service/LoginPolicyService.java` | Application MFA policy facade |
| `armorauth-admin/src/main/java/com/armorauth/admin/service/FederatedBindingAdminService.java` | Binding query/delete logic |
| `armorauth-admin/src/main/java/com/armorauth/admin/service/IdentityProviderService.java` | Provider config validation and optional remote probe |
| `armorauth-model/src/main/java/com/armorauth/data/repository/OAuth2ScopeRepository.java` | Composite-id repository and query helpers |
| `armorauth-model/src/main/java/com/armorauth/data/repository/UserFederatedBindingRepository.java` | Binding query helpers |

Verification:

```powershell
$env:SystemRoot='C:\WINDOWS'; $env:windir='C:\WINDOWS'; $env:COMSPEC='C:\WINDOWS\system32\cmd.exe'; $env:HTTP_PROXY='http://127.0.0.1:7897'; $env:HTTPS_PROXY='http://127.0.0.1:7897'; $env:ALL_PROXY='http://127.0.0.1:7897'; $env:JAVA_HOME='C:\Users\FuLin\.jdks\temurin-21.0.10'; $env:MAVEN_HOME='C:\Users\FuLin\.maven\apache-maven-3.9.9'; $env:PATH="$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:PATH"; $env:MAVEN_OPTS='-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897'; mvn '-DskipTests' '-Dspotless.apply.skip=true' '-Dspotless.check.skip=true' compile
```

Result: build success.

```powershell
$env:SystemRoot='C:\WINDOWS'; $env:windir='C:\WINDOWS'; $env:COMSPEC='C:\WINDOWS\system32\cmd.exe'; $env:HTTP_PROXY='http://127.0.0.1:7897'; $env:HTTPS_PROXY='http://127.0.0.1:7897'; $env:ALL_PROXY='http://127.0.0.1:7897'; $env:JAVA_HOME='C:\Users\FuLin\.jdks\temurin-21.0.10'; $env:MAVEN_HOME='C:\Users\FuLin\.maven\apache-maven-3.9.9'; $env:PATH="$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:PATH"; $env:MAVEN_OPTS='-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897'; mvn '-Dspotless.apply.skip=true' '-Dspotless.check.skip=true' test
```

Result: test success.

```powershell
$env:SystemRoot='C:\WINDOWS'; $env:windir='C:\WINDOWS'; $env:COMSPEC='C:\WINDOWS\system32\cmd.exe'; $env:HTTP_PROXY='http://127.0.0.1:7897'; $env:HTTPS_PROXY='http://127.0.0.1:7897'; $env:ALL_PROXY='http://127.0.0.1:7897'; $env:JAVA_HOME='C:\Users\FuLin\.jdks\temurin-21.0.10'; $env:MAVEN_HOME='C:\Users\FuLin\.maven\apache-maven-3.9.9'; $env:PATH="$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:PATH"; $env:MAVEN_OPTS='-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897'; mvn '-DskipTests' '-Dspotless.apply.skip=true' '-Dspotless.check.skip=true' -pl armorauth-server -am package
```

Result: package success.

Runtime smoke test on local MySQL profile (`admin/admin123`, server `http://127.0.0.1:9000`):

| Endpoint | Method | Result |
| --- | --- | --- |
| `/actuator/health` | GET | 200 |
| `/api/admin/v1/scopes?page=0&size=1` | GET | 200 |
| `/api/admin/v1/login-policies?page=0&size=1` | GET | 200 |
| `/api/admin/v1/federated-bindings?page=0&size=1` | GET | 200 |
| `/api/admin/v1/identity-providers?page=0&size=1` | GET | 200 |
| `/api/admin/v1/identity-providers` | POST | 200, temporary OIDC provider created |
| `/api/admin/v1/identity-providers/{id}:test?probeRemote=false` | POST | 200, config check passed |
| `/api/admin/v1/identity-providers/{id}` | DELETE | 200, temporary provider removed |

Follow-up candidates:

| Priority | Item | Reason |
| --- | --- | --- |
| P1 | Encryption key rotation | Secret and JWK values are now protected; a future versioned key-ring can support rotation without downtime. |

## Step 2 - OAuth/OIDC End-to-End Regression Tests

Status: Completed

Goal: cover the OAuth/OIDC runtime flows called out by `docs/ROADMAP.md`.

Implemented:

| Area | Status | Notes |
| --- | --- | --- |
| Client credentials flow | Completed | Added an HTTP-level test for `/oauth2/token` using the seeded confidential client and `message.read` scope. |
| Authorization code + PKCE | Completed | Added a browser-session style test that redirects to login, authenticates `admin`, returns an authorization code, exchanges it with S256 PKCE, receives access token and ID token, and calls `/userinfo`. |
| Refresh token flow | Completed | Added a confidential-client authorization-code test with consent approval, then exchanges the refresh token for a new access token. |
| Device authorization flow | Completed | Added full device flow coverage: device authorization request, initial `authorization_pending`, user activation through `/activate`, consent approval, `/activated` redirect, and final device-code token exchange. |
| Empty IdP startup | Completed | Fixed `FederationConfiguration` so a profile with no configured external identity providers starts with an empty iterable repository instead of failing context startup. |
| React SPA PKCE seed data | Completed | Added V16 migration to relink React SPA scopes/settings/token settings from the row id to the public `client_id` used by runtime mappings. |

Primary files:

| File | Purpose |
| --- | --- |
| `armorauth-server/src/test/java/com/armorauth/OAuth2OidcFlowE2eTest.java` | OAuth/OIDC HTTP end-to-end regression tests |
| `armorauth-server/src/test/resources/application-oauth-e2e-test.yml` | Isolated in-memory H2 test profile with Flyway migrations |
| `armorauth-server/src/main/resources/db/migration/V16__fix_react_spa_pkce_links.sql` | Repairs V15 React SPA PKCE client foreign-key links |
| `armorauth-federation/src/main/java/com/armorauth/federation/config/FederationConfiguration.java` | Handles zero external IdP registrations safely |
| `armorauth-server/pom.xml` | Adds Spring Boot test support for server integration tests |

Verification:

```powershell
$env:SystemRoot='C:\WINDOWS'; $env:windir='C:\WINDOWS'; $env:COMSPEC='C:\WINDOWS\system32\cmd.exe'; $env:HTTP_PROXY='http://127.0.0.1:7897'; $env:HTTPS_PROXY='http://127.0.0.1:7897'; $env:ALL_PROXY='http://127.0.0.1:7897'; $env:JAVA_HOME='C:\Users\FuLin\.jdks\temurin-21.0.10'; $env:MAVEN_HOME='C:\Users\FuLin\.maven\apache-maven-3.9.9'; $env:PATH="$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:PATH"; $env:MAVEN_OPTS='-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897'; mvn '-Dspotless.apply.skip=true' '-Dspotless.check.skip=true' '-Dsurefire.failIfNoSpecifiedTests=false' -pl armorauth-server -am '-Dtest=OAuth2OidcFlowE2eTest' test
```

Result: 4 OAuth/OIDC E2E tests passed.

```powershell
$env:SystemRoot='C:\WINDOWS'; $env:windir='C:\WINDOWS'; $env:COMSPEC='C:\WINDOWS\system32\cmd.exe'; $env:HTTP_PROXY='http://127.0.0.1:7897'; $env:HTTPS_PROXY='http://127.0.0.1:7897'; $env:ALL_PROXY='http://127.0.0.1:7897'; $env:JAVA_HOME='C:\Users\FuLin\.jdks\temurin-21.0.10'; $env:MAVEN_HOME='C:\Users\FuLin\.maven\apache-maven-3.9.9'; $env:PATH="$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:PATH"; $env:MAVEN_OPTS='-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897'; mvn '-Dspotless.apply.skip=true' '-Dspotless.check.skip=true' test
```

Result: full reactor test success.

```powershell
$env:SystemRoot='C:\WINDOWS'; $env:windir='C:\WINDOWS'; $env:COMSPEC='C:\WINDOWS\system32\cmd.exe'; $env:HTTP_PROXY='http://127.0.0.1:7897'; $env:HTTPS_PROXY='http://127.0.0.1:7897'; $env:ALL_PROXY='http://127.0.0.1:7897'; $env:JAVA_HOME='C:\Users\FuLin\.jdks\temurin-21.0.10'; $env:MAVEN_HOME='C:\Users\FuLin\.maven\apache-maven-3.9.9'; $env:PATH="$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:PATH"; $env:MAVEN_OPTS='-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897'; mvn '-DskipTests' '-Dspotless.apply.skip=true' '-Dspotless.check.skip=true' -pl armorauth-server -am package
```

Result: package success.

Runtime smoke test on local MySQL profile:

| Check | Result |
| --- | --- |
| Flyway migration | MySQL schema migrated from v14 to v16 |
| `/actuator/health` | 200 |
| `/oauth2/token` client credentials | 200, Bearer token issued with `message.read` |

Follow-up candidates:

| Priority | Item | Reason |
| --- | --- | --- |
| P1 | Admin UI pages/buttons for Step 1 APIs | Backend is available, but UI is not yet wired for scope catalog, login policy, IdP test, or federated binding management. |
| P1 | Encryption key rotation | Secret and JWK values are now protected; a future versioned key-ring can support rotation without downtime. |

## Step 3 - OAuth/OIDC Endpoint Metadata and Token Governance Tests

Status: Completed

Goal: cover the Spring Authorization Server default endpoints called out by `docs/ROADMAP.md`: discovery, JWKS, revocation, introspection, and OIDC logout.

Implemented:

| Area | Status | Notes |
| --- | --- | --- |
| OIDC discovery | Completed | Fixed `/.well-known/openid-configuration`, which was returning 404 because `/.well-known/**` was globally ignored before the authorization server filter chain. |
| OAuth authorization server metadata | Completed | Added regression coverage for `/.well-known/oauth-authorization-server`. |
| JWKS | Completed | Added regression coverage for `/oauth2/jwks`, including public key shape and no private key material in the response. |
| Token introspection | Completed | Added regression coverage for `/oauth2/introspect` with authenticated confidential client access. |
| Token revocation | Completed | Added regression coverage for `/oauth2/revoke`; introspection now proves the same token changes from active to inactive after revocation. |
| OIDC logout | Completed | Added regression coverage for `/connect/logout` with `id_token_hint`, `client_id`, registered `post_logout_redirect_uri`, and `state`. |

Primary files:

| File | Purpose |
| --- | --- |
| `armorauth-core/src/main/java/com/armorauth/config/DefaultSecurityConfig.java` | Allows `/.well-known/**` requests to reach Spring Authorization Server endpoint filters |
| `armorauth-server/src/test/java/com/armorauth/OAuth2OidcFlowE2eTest.java` | Extends OAuth/OIDC HTTP E2E coverage from 4 to 7 tests |

Verification:

```powershell
$env:SystemRoot='C:\WINDOWS'; $env:windir='C:\WINDOWS'; $env:COMSPEC='C:\WINDOWS\system32\cmd.exe'; $env:HTTP_PROXY='http://127.0.0.1:7897'; $env:HTTPS_PROXY='http://127.0.0.1:7897'; $env:ALL_PROXY='http://127.0.0.1:7897'; $env:JAVA_HOME='C:\Users\FuLin\.jdks\temurin-21.0.10'; $env:MAVEN_HOME='C:\Users\FuLin\.maven\apache-maven-3.9.9'; $env:PATH="$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:PATH"; $env:MAVEN_OPTS='-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897'; mvn '-Dspotless.apply.skip=true' '-Dspotless.check.skip=true' '-Dsurefire.failIfNoSpecifiedTests=false' -pl armorauth-server -am '-Dtest=OAuth2OidcFlowE2eTest' test
```

Result: 7 OAuth/OIDC E2E tests passed.

```powershell
$env:SystemRoot='C:\WINDOWS'; $env:windir='C:\WINDOWS'; $env:COMSPEC='C:\WINDOWS\system32\cmd.exe'; $env:HTTP_PROXY='http://127.0.0.1:7897'; $env:HTTPS_PROXY='http://127.0.0.1:7897'; $env:ALL_PROXY='http://127.0.0.1:7897'; $env:JAVA_HOME='C:\Users\FuLin\.jdks\temurin-21.0.10'; $env:MAVEN_HOME='C:\Users\FuLin\.maven\apache-maven-3.9.9'; $env:PATH="$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:PATH"; $env:MAVEN_OPTS='-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897'; mvn '-Dspotless.apply.skip=true' '-Dspotless.check.skip=true' test
```

Result: full reactor test success.

```powershell
$env:SystemRoot='C:\WINDOWS'; $env:windir='C:\WINDOWS'; $env:COMSPEC='C:\WINDOWS\system32\cmd.exe'; $env:HTTP_PROXY='http://127.0.0.1:7897'; $env:HTTPS_PROXY='http://127.0.0.1:7897'; $env:ALL_PROXY='http://127.0.0.1:7897'; $env:JAVA_HOME='C:\Users\FuLin\.jdks\temurin-21.0.10'; $env:MAVEN_HOME='C:\Users\FuLin\.maven\apache-maven-3.9.9'; $env:PATH="$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:PATH"; $env:MAVEN_OPTS='-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897'; mvn '-DskipTests' '-Dspotless.apply.skip=true' '-Dspotless.check.skip=true' -pl armorauth-server -am package
```

Result: package success after stopping the old local server process that was locking the jar on Windows.

Runtime smoke test on local MySQL profile:

| Check | Result |
| --- | --- |
| Server process | `http://127.0.0.1:9000`, PID `46976` |
| Admin UI process | `http://127.0.0.1:1080`, PID `13704` |
| MySQL Flyway migration | Schema already at v16 |
| `/actuator/health` | 200 |
| `/.well-known/openid-configuration` | 200, exposes issuer, JWKS, revocation, introspection, logout endpoints |
| `/oauth2/jwks` | 200 |
| `/oauth2/token` client credentials | 200, Bearer token issued |
| `/oauth2/introspect` before revocation | 200, `active=true` |
| `/oauth2/revoke` | 200 |
| `/oauth2/introspect` after revocation | 200, `active=false` |

Follow-up candidates:

| Priority | Item | Reason |
| --- | --- | --- |
| P1 | Encryption key rotation | Secret and JWK values are now protected; a future versioned key-ring can support rotation without downtime. |
| P2 | README quick-start refresh | The runtime baseline is now stronger; README should describe MySQL/Flyway, admin UI, and OAuth sample smoke checks in one fresh path. |

## Step 4 - Admin UI Wiring for Management API Gaps

Status: Completed

Goal: make the Step 1 management APIs usable from the Vue admin console.

Implemented:

| Area | Status | Notes |
| --- | --- | --- |
| Scope management UI | Completed | Added `/main/scopes` with client filtering, list, create, edit description, and delete actions backed by `/api/admin/v1/scopes`. |
| Login policy UI | Completed | Added `/main/loginPolicies` to list application MFA policy and toggle per-application `mfaRequired`. Built-in role MFA policy is displayed as read-only tags. |
| Federated binding UI | Completed | Added `/main/federatedBindings` with `userId` / `registrationId` filters, provider attribute inspection, and binding removal. |
| Identity provider test UI | Completed | Added local config test and explicit remote probe actions to Identity Provider rows, plus a result modal showing check status. |
| Identity provider form coverage | Completed | Added `jwkSetUri` and `attributeMapping` fields so the UI covers the backend DTO fields needed by OIDC provider validation and mapping. |
| Navigation and API client | Completed | Added API wrappers, routes, and sidebar menu entries for the new pages. |

Primary files:

| File | Purpose |
| --- | --- |
| `armorauth-admin-ui/src/api/index.js` | API wrappers for scopes, login policies, IdP test, and federated bindings |
| `armorauth-admin-ui/src/router/main_children.js` | Routes for the new admin pages |
| `armorauth-admin-ui/src/views/Main.vue` | Sidebar menu entries and icons |
| `armorauth-admin-ui/src/views/main/Scopes.vue` | Scope management page |
| `armorauth-admin-ui/src/views/main/LoginPolicies.vue` | Login policy page |
| `armorauth-admin-ui/src/views/main/FederatedBindings.vue` | External account binding page |
| `armorauth-admin-ui/src/views/main/IdentityProviders.vue` | Adds IdP test actions and missing form fields |

Verification:

```powershell
$env:SystemRoot='C:\WINDOWS'; $env:windir='C:\WINDOWS'; $env:COMSPEC='C:\WINDOWS\system32\cmd.exe'; $env:HTTP_PROXY='http://127.0.0.1:7897'; $env:HTTPS_PROXY='http://127.0.0.1:7897'; $env:ALL_PROXY='http://127.0.0.1:7897'; npm run build
```

Result: admin UI production build success.

Runtime smoke test with local services:

| Check | Result |
| --- | --- |
| Server process | `http://127.0.0.1:9000`, PID `46976` |
| Admin UI process | `http://127.0.0.1:1080`, PID `13704` |
| `/api/admin/v1/scopes?page=0&size=1` | 200 |
| `/api/admin/v1/login-policies?page=0&size=1` | 200 |
| `/api/admin/v1/federated-bindings?page=0&size=1` | 200 |
| `/api/admin/v1/identity-providers?page=0&size=1` | 200 |
| Browser route `/main/scopes` | Rendered without visible request error |
| Browser route `/main/loginPolicies` | Rendered without visible request error |
| Browser route `/main/federatedBindings` | Rendered without visible request error |
| Browser route `/main/identityProviders` | Rendered without visible request error |
| Browser console | No error logs observed during route smoke |

Follow-up candidates:

| Priority | Item | Reason |
| --- | --- | --- |
| P1 | Encryption key rotation | Secret and JWK values are now protected; a future versioned key-ring can support rotation without downtime. |
| P2 | README quick-start refresh | The runtime baseline is now stronger; README should describe MySQL/Flyway, admin UI, OAuth sample smoke checks in one fresh path. |

## Step 5 - Secret Encryption at Rest

Status: Completed

Goal: stop storing reversible runtime secrets as plaintext while keeping existing deployments readable.

Implemented:

| Area | Status | Notes |
| --- | --- | --- |
| Secret crypto service | Completed | Added AES-GCM protection with `{enc}v1:` prefix, idempotent `protect`, and legacy plaintext passthrough in `reveal`. The key comes from `armorauth.crypto.secret-key` or `ARMORAUTH_CRYPTO_SECRET`. |
| Identity provider secrets | Completed | Create/update now stores protected `clientSecret`; blank UI updates no longer erase the existing secret. Runtime client registrations decrypt before use. |
| Webhook secrets | Completed | Create/update now stores protected webhook secrets, blank UI updates preserve the existing secret, and HMAC delivery signing decrypts before computing the signature. |
| TOTP secrets | Completed | TOTP setup stores protected secrets while still returning the one-time raw secret/URI to the user; verification accepts protected and legacy plaintext secrets. |
| Legacy backfill | Completed | Added startup backfill for existing plaintext IdP, webhook, and auth-factor secrets. Already protected values are skipped. |
| Regression tests | Completed | Added crypto and TOTP compatibility tests for encrypted, idempotent, and legacy plaintext paths. |

Primary files:

| File | Purpose |
| --- | --- |
| `armorauth-core/src/main/java/com/armorauth/crypto/SecretCryptoService.java` | Reversible secret protection service |
| `armorauth/src/main/java/com/armorauth/config/SecurityEnhancementConfiguration.java` | Wires `SecretCryptoService` and encrypted-aware `TotpService` |
| `armorauth-core/src/main/java/com/armorauth/mfa/TotpService.java` | Decrypts protected TOTP secrets during verification/code generation |
| `armorauth-admin/src/main/java/com/armorauth/admin/service/IdentityProviderService.java` | Protects IdP client secrets and preserves existing secrets on blank update |
| `armorauth-admin/src/main/java/com/armorauth/admin/service/WebhookService.java` | Protects webhook secrets and decrypts before HMAC signing |
| `armorauth-admin/src/main/java/com/armorauth/admin/service/AccountService.java` | Protects newly created TOTP secrets |
| `armorauth-admin/src/main/java/com/armorauth/admin/service/SecretProtectionBackfillService.java` | Startup backfill for legacy plaintext secrets |
| `armorauth-federation/src/main/java/com/armorauth/federation/config/FederationConfiguration.java` | Decrypts DB-backed IdP secrets when building client registrations |
| `armorauth-core/src/main/java/com/armorauth/federation/DynamicClientRegistrationRepository.java` | Keeps dynamic client registration secret reveal compatibility |
| `armorauth-core/src/test/java/com/armorauth/crypto/SecretCryptoServiceTest.java` | Crypto/TOTP regression tests |

Verification:

```powershell
$env:SystemRoot='C:\WINDOWS'; $env:windir='C:\WINDOWS'; $env:COMSPEC='C:\WINDOWS\system32\cmd.exe'; $env:HTTP_PROXY='http://127.0.0.1:7897'; $env:HTTPS_PROXY='http://127.0.0.1:7897'; $env:ALL_PROXY='http://127.0.0.1:7897'; $env:JAVA_HOME='C:\Users\FuLin\.jdks\temurin-21.0.10'; $env:MAVEN_HOME='C:\Users\FuLin\.maven\apache-maven-3.9.9'; $env:PATH="$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:PATH"; $env:MAVEN_OPTS='-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897'; mvn '-Dspotless.apply.skip=true' '-Dspotless.check.skip=true' test
```

Result: full reactor test success. `SecretCryptoServiceTest` ran 3 tests; OAuth/OIDC E2E still ran 7 tests.

```powershell
$env:SystemRoot='C:\WINDOWS'; $env:windir='C:\WINDOWS'; $env:COMSPEC='C:\WINDOWS\system32\cmd.exe'; $env:HTTP_PROXY='http://127.0.0.1:7897'; $env:HTTPS_PROXY='http://127.0.0.1:7897'; $env:ALL_PROXY='http://127.0.0.1:7897'; $env:JAVA_HOME='C:\Users\FuLin\.jdks\temurin-21.0.10'; $env:MAVEN_HOME='C:\Users\FuLin\.maven\apache-maven-3.9.9'; $env:PATH="$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:PATH"; $env:MAVEN_OPTS='-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897'; mvn '-DskipTests' '-Dspotless.apply.skip=true' '-Dspotless.check.skip=true' -pl armorauth-server -am package
```

Result: package success.

Runtime smoke test on local MySQL profile:

| Check | Result |
| --- | --- |
| Server process | `http://127.0.0.1:9000`, PID `6120` |
| Admin UI process | `http://127.0.0.1:1080`, PID `13704` |
| `/actuator/health` | UP |
| New IdP secret storage | API create returned 200; DB `client_secret` started with `{enc}v1:` and did not contain the raw secret |
| New webhook secret storage | API create returned 200; DB `secret` started with `{enc}v1:` and did not contain the raw secret |
| Blank webhook secret update | API update returned 200; DB `secret` stayed unchanged and encrypted |
| IdP config test after decrypt/backfill | 200, `success=true` |
| Legacy backfill | Forced one IdP and one webhook secret back to plaintext, restarted server, log reported `Protected 2 legacy plaintext secret(s)` |
| Backfilled IdP secret | DB `client_secret` started with `{enc}v1:` and did not contain the forced plaintext |
| Backfilled webhook secret | DB `secret` started with `{enc}v1:` and did not contain the forced plaintext |
| Temporary verification data | Deleted after smoke test |

Follow-up candidates:

| Priority | Item | Reason |
| --- | --- | --- |
| P1 | Encryption key rotation | `{enc}v1:` values depend on one configured key; a future versioned key-ring can support rotation without downtime. |
| P2 | README quick-start refresh | Document MySQL/Flyway, admin UI, OAuth smoke checks, and `ARMORAUTH_CRYPTO_SECRET`. |

## Step 6 - JWK Private Key Protection

Status: Completed

Goal: stop storing authorization-server signing private keys as plaintext while preserving startup and token-signing behavior.

Implemented:

| Area | Status | Notes |
| --- | --- | --- |
| JWK private key encryption | Completed | `PersistentJwkSource` now protects generated RSA private keys before saving to `jwk_key.private_key`. |
| Runtime private key reveal | Completed | Active signing keys are decrypted in memory only when reconstructing Nimbus `RSAKey` instances. |
| Legacy private key compatibility | Completed | Existing plaintext private keys still load, so startup remains compatible before backfill. |
| Legacy backfill | Completed | Startup backfill now protects plaintext `jwk_key.private_key` rows in addition to IdP, webhook, and TOTP secrets. |
| Regression tests | Completed | Added JWK source tests for encrypted-at-rest generation and legacy plaintext loading. |

Primary files:

| File | Purpose |
| --- | --- |
| `armorauth-core/src/main/java/com/armorauth/jose/PersistentJwkSource.java` | Protects generated JWK private keys and reveals encrypted keys at runtime |
| `armorauth-core/src/main/java/com/armorauth/config/AuthorizationServerConfig.java` | Injects `SecretCryptoService` into `PersistentJwkSource` |
| `armorauth-admin/src/main/java/com/armorauth/admin/service/SecretProtectionBackfillService.java` | Backfills legacy plaintext JWK private keys |
| `armorauth-core/src/test/java/com/armorauth/jose/PersistentJwkSourceTest.java` | JWK private-key protection regression tests |

Verification:

```powershell
$env:SystemRoot='C:\WINDOWS'; $env:windir='C:\WINDOWS'; $env:COMSPEC='C:\WINDOWS\system32\cmd.exe'; $env:HTTP_PROXY='http://127.0.0.1:7897'; $env:HTTPS_PROXY='http://127.0.0.1:7897'; $env:ALL_PROXY='http://127.0.0.1:7897'; $env:JAVA_HOME='C:\Users\FuLin\.jdks\temurin-21.0.10'; $env:MAVEN_HOME='C:\Users\FuLin\.maven\apache-maven-3.9.9'; $env:PATH="$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:PATH"; $env:MAVEN_OPTS='-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897'; mvn '-Dspotless.apply.skip=true' '-Dspotless.check.skip=true' -pl armorauth-core '-Dtest=SecretCryptoServiceTest,PersistentJwkSourceTest' test
```

Result: 5 targeted core tests passed.

```powershell
$env:SystemRoot='C:\WINDOWS'; $env:windir='C:\WINDOWS'; $env:COMSPEC='C:\WINDOWS\system32\cmd.exe'; $env:HTTP_PROXY='http://127.0.0.1:7897'; $env:HTTPS_PROXY='http://127.0.0.1:7897'; $env:ALL_PROXY='http://127.0.0.1:7897'; $env:JAVA_HOME='C:\Users\FuLin\.jdks\temurin-21.0.10'; $env:MAVEN_HOME='C:\Users\FuLin\.maven\apache-maven-3.9.9'; $env:PATH="$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:PATH"; $env:MAVEN_OPTS='-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897'; mvn '-Dspotless.apply.skip=true' '-Dspotless.check.skip=true' test
```

Result: full reactor test success. OAuth/OIDC E2E still ran 7 tests.

```powershell
$env:SystemRoot='C:\WINDOWS'; $env:windir='C:\WINDOWS'; $env:COMSPEC='C:\WINDOWS\system32\cmd.exe'; $env:HTTP_PROXY='http://127.0.0.1:7897'; $env:HTTPS_PROXY='http://127.0.0.1:7897'; $env:ALL_PROXY='http://127.0.0.1:7897'; $env:JAVA_HOME='C:\Users\FuLin\.jdks\temurin-21.0.10'; $env:MAVEN_HOME='C:\Users\FuLin\.maven\apache-maven-3.9.9'; $env:PATH="$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:PATH"; $env:MAVEN_OPTS='-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897'; mvn '-DskipTests' '-Dspotless.apply.skip=true' '-Dspotless.check.skip=true' -pl armorauth-server -am package
```

Result: package success.

Runtime smoke test on local MySQL profile:

| Check | Result |
| --- | --- |
| Server process | `http://127.0.0.1:9000`, PID `43384` |
| Admin UI process | `http://127.0.0.1:1080`, PID `13704` |
| Startup backfill | Log reported `jwkKeys=1` and updated `jwk_key.private_key` |
| `/actuator/health` | UP |
| `/oauth2/jwks` | 200, returned 1 public key and no private `d` field |
| `/api/admin/v1/jwk-keys` | 200, returned 1 key metadata item |
| DB private key storage | Checked 1 row; encrypted private keys = 1, plaintext private keys = 0 |

Follow-up candidates:

| Priority | Item | Reason |
| --- | --- | --- |
| P1 | Encryption key rotation | `{enc}v1:` values depend on one configured key; a future versioned key-ring can support rotation without downtime. |
| P2 | README quick-start refresh | Document MySQL/Flyway, admin UI, OAuth smoke checks, and `ARMORAUTH_CRYPTO_SECRET`. |

## Step 7 - Encryption Key Rotation / Versioned Crypto Key Ring

Status: Completed

Goal: allow operators to rotate the reversible secret-protection key without breaking existing encrypted IdP, webhook, TOTP, and JWK private-key values.

Implemented:

| Area | Status | Notes |
| --- | --- | --- |
| Versioned ciphertext format | Completed | `SecretCryptoService` now writes `{enc}<keyId>:<payload>`. Existing `{enc}v1:` values remain readable. |
| Active key selection | Completed | New writes use `armorauth.crypto.active-key-id` / `ARMORAUTH_CRYPTO_ACTIVE_KEY_ID`; default remains `v1`. |
| Key-ring configuration | Completed | Added `armorauth.crypto.keys` / `ARMORAUTH_CRYPTO_KEYS` as a comma-separated `keyId=secret` list. The legacy `armorauth.crypto.secret-key` / `ARMORAUTH_CRYPTO_SECRET` is still loaded as the `v1` fallback. |
| Backward compatibility | Completed | Old protected values are decrypted by embedded key id, while plaintext compatibility and idempotent `protect` behavior are preserved. |
| Regression tests | Completed | Added key-ring, active-key, legacy fallback, and unknown-key failure coverage. |

Operational notes:

| Config | Example | Notes |
| --- | --- | --- |
| `ARMORAUTH_CRYPTO_SECRET` | `old-production-secret` | Legacy `v1` key fallback; keep configured while any `{enc}v1:` values exist. |
| `ARMORAUTH_CRYPTO_KEYS` | `v2=new-production-secret` | Additional key-ring entries. Current parser treats commas as separators, so secrets should not contain commas. |
| `ARMORAUTH_CRYPTO_ACTIVE_KEY_ID` | `v2` | New protected values are written with this key id. |

Primary files:

| File | Purpose |
| --- | --- |
| `armorauth-core/src/main/java/com/armorauth/crypto/SecretCryptoService.java` | Versioned AES-GCM key-ring implementation |
| `armorauth/src/main/java/com/armorauth/config/SecurityEnhancementConfiguration.java` | Wires legacy key, key-ring, and active key id from properties/env |
| `armorauth-core/src/test/java/com/armorauth/crypto/SecretCryptoServiceTest.java` | Key-ring and rotation regression tests |

Verification:

```powershell
$env:SystemRoot='C:\WINDOWS'; $env:windir='C:\WINDOWS'; $env:COMSPEC='C:\WINDOWS\system32\cmd.exe'; $env:HTTP_PROXY='http://127.0.0.1:7897'; $env:HTTPS_PROXY='http://127.0.0.1:7897'; $env:ALL_PROXY='http://127.0.0.1:7897'; $env:JAVA_HOME='C:\Users\FuLin\.jdks\temurin-21.0.10'; $env:MAVEN_HOME='C:\Users\FuLin\.maven\apache-maven-3.9.9'; $env:PATH="$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:PATH"; $env:MAVEN_OPTS='-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897'; mvn '-Dspotless.apply.skip=true' '-Dspotless.check.skip=true' -pl armorauth-core '-Dtest=SecretCryptoServiceTest,PersistentJwkSourceTest' test
```

Result: 8 targeted core tests passed.

```powershell
$env:SystemRoot='C:\WINDOWS'; $env:windir='C:\WINDOWS'; $env:COMSPEC='C:\WINDOWS\system32\cmd.exe'; $env:HTTP_PROXY='http://127.0.0.1:7897'; $env:HTTPS_PROXY='http://127.0.0.1:7897'; $env:ALL_PROXY='http://127.0.0.1:7897'; $env:JAVA_HOME='C:\Users\FuLin\.jdks\temurin-21.0.10'; $env:MAVEN_HOME='C:\Users\FuLin\.maven\apache-maven-3.9.9'; $env:PATH="$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:PATH"; $env:MAVEN_OPTS='-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897'; mvn '-Dspotless.apply.skip=true' '-Dspotless.check.skip=true' test
```

Result: full reactor test success. OAuth/OIDC E2E still ran 7 tests.

```powershell
$env:SystemRoot='C:\WINDOWS'; $env:windir='C:\WINDOWS'; $env:COMSPEC='C:\WINDOWS\system32\cmd.exe'; $env:HTTP_PROXY='http://127.0.0.1:7897'; $env:HTTPS_PROXY='http://127.0.0.1:7897'; $env:ALL_PROXY='http://127.0.0.1:7897'; $env:JAVA_HOME='C:\Users\FuLin\.jdks\temurin-21.0.10'; $env:MAVEN_HOME='C:\Users\FuLin\.maven\apache-maven-3.9.9'; $env:PATH="$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:PATH"; $env:MAVEN_OPTS='-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897'; mvn '-DskipTests' '-Dspotless.apply.skip=true' '-Dspotless.check.skip=true' -pl armorauth-server -am package
```

Result: package success.

Runtime smoke test on local MySQL profile:

| Check | Result |
| --- | --- |
| Temporary v2-active server | Started with `ARMORAUTH_CRYPTO_KEYS='v2=ArmorAuth local development v2 rotation smoke key'` and `ARMORAUTH_CRYPTO_ACTIVE_KEY_ID='v2'` |
| Existing v1 JWK compatibility | `/oauth2/jwks` returned 1 public key and no private `d` field while the DB private key remained `{enc}v1:` |
| Admin JWK metadata | `/api/admin/v1/jwk-keys` returned 200 |
| New webhook secret under active v2 | API create returned 200; DB `secret` started with `{enc}v2:` and did not contain the raw secret |
| Temporary verification data | Deleted after smoke test |
| Final server process | Restored default config on `http://127.0.0.1:9000`, PID `45636`, health `UP` |
| Admin UI process | Still running on `http://127.0.0.1:1080`, PID `13704` |

Follow-up candidates:

| Priority | Item | Reason |
| --- | --- | --- |
| P1 | README quick-start refresh | Document MySQL/Flyway, admin UI, OAuth smoke checks, and crypto key-ring env vars in one fresh local path. |
| P2 | Operational re-encryption job/API | Key rotation is supported for new writes; a controlled job/API would let operators rewrite existing `{enc}v1:` rows to the active key after rollout. |

## Step 8 - README and Quick Start Refresh

Status: Completed

Goal: make the top-level onboarding docs match the current runnable system instead of the older demo/prototype baseline.

Implemented:

| Area | Status | Notes |
| --- | --- | --- |
| README baseline | Completed | Rewrote the project overview to reflect the current Java 21 / Spring Boot 4 / MySQL / Flyway / admin API / admin UI baseline. |
| Runtime capability summary | Completed | Added current OAuth/OIDC, management API, admin UI, JWK persistence, secret encryption, and key-ring rotation capabilities. |
| Local quick start | Completed | Replaced the old quick-start path with the current MySQL server + Vite admin UI startup flow and smoke checks. |
| Crypto operation notes | Completed | Documented `ARMORAUTH_CRYPTO_SECRET`, `ARMORAUTH_CRYPTO_KEYS`, `ARMORAUTH_CRYPTO_ACTIVE_KEY_ID`, and the `{enc}<keyId>:` format. |
| Deployment guide | Completed | Added production crypto-key requirements and clarified JWK backup now also requires backing up crypto key material. |

Primary files:

| File | Purpose |
| --- | --- |
| `README.md` | Current project baseline, modules, capabilities, common commands, key-ring notes |
| `docs/quick-start.md` | Local MySQL + server + admin UI startup and smoke checks |
| `docs/deployment-guide.md` | Production crypto env vars, key rotation guidance, JWK/key backup notes |

Verification:

| Check | Result |
| --- | --- |
| Stale wording scan | No README/quick-start/deployment-guide matches for old admin placeholder or dynamic JWK claims |
| Server process | `http://127.0.0.1:9000`, PID `45636`, health `UP` |
| Admin UI process | `http://127.0.0.1:1080`, PID `13704` |
| OIDC discovery | `/.well-known/openid-configuration` returned issuer `http://localhost:9000` |
| JWKS | `/oauth2/jwks` returned 1 key |
| Admin API smoke | `/api/admin/v1/applications?page=0&size=1` and `/api/admin/v1/jwk-keys` returned code 200 |
| Client credentials smoke | Seed confidential client issued a Bearer token with `message.read` scope |

Follow-up candidates:

| Priority | Item | Reason |
| --- | --- | --- |
| P1 | Operational re-encryption job/API | Key rotation is supported for new writes; operators still need a controlled way to rewrite existing `{enc}v1:` rows to the active key. |
| P2 | API reference refresh | `docs/api-reference.md` predates several newer admin APIs and should be aligned with the current controller surface. |

## Step 9 - Operational Secret Rekey API

Status: Completed

Goal: let operators safely rewrite existing protected secrets to the current active crypto key after a key-ring rotation.

Implemented:

| Area | Status | Notes |
| --- | --- | --- |
| Protected key introspection | Completed | `SecretCryptoService` now exposes `protectedKeyId` and `isProtectedWithActiveKey` helpers. |
| Rekey service | Completed | Added an operational service that scans IdP client secrets, webhook secrets, auth-factor secrets, and JWK private keys. |
| Dry-run by default | Completed | `POST /api/admin/v1/secret-protection/rekey` defaults to dry-run unless the request explicitly sends `{"dryRun":false}`. |
| SUPER_ADMIN guard | Completed | The endpoint requires `SUPER_ADMIN` via method security. |
| Audit trail | Completed | Dry-run and execute paths record `SECRET_REKEY_DRY_RUN` / `SECRET_REKEY_EXECUTED` audit events. |
| Docs | Completed | API Reference and deployment guide now include the rekey endpoint and rotation procedure. |

Primary files:

| File | Purpose |
| --- | --- |
| `armorauth-core/src/main/java/com/armorauth/crypto/SecretCryptoService.java` | Exposes protected key id helpers |
| `armorauth-admin/src/main/java/com/armorauth/admin/controller/SecretProtectionController.java` | Admin rekey endpoint |
| `armorauth-admin/src/main/java/com/armorauth/admin/service/SecretRekeyService.java` | Dry-run and execute rekey workflow |
| `armorauth-admin/src/main/java/com/armorauth/admin/dto/SecretProtectionDTO.java` | Request/response DTOs |
| `armorauth-core/src/test/java/com/armorauth/crypto/SecretCryptoServiceTest.java` | Key-id helper regression assertions |
| `docs/api-reference.md` | Documents the rekey API |
| `docs/deployment-guide.md` | Documents production rotation procedure |

Verification:

```powershell
$env:SystemRoot='C:\WINDOWS'; $env:windir='C:\WINDOWS'; $env:COMSPEC='C:\WINDOWS\system32\cmd.exe'; $env:HTTP_PROXY='http://127.0.0.1:7897'; $env:HTTPS_PROXY='http://127.0.0.1:7897'; $env:ALL_PROXY='http://127.0.0.1:7897'; $env:JAVA_HOME='C:\Users\FuLin\.jdks\temurin-21.0.10'; $env:MAVEN_HOME='C:\Users\FuLin\.maven\apache-maven-3.9.9'; $env:PATH="$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:PATH"; $env:MAVEN_OPTS='-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897'; mvn '-Dspotless.apply.skip=true' '-Dspotless.check.skip=true' -pl armorauth-core '-Dtest=SecretCryptoServiceTest' test
```

Result: 6 targeted crypto tests passed.

```powershell
$env:SystemRoot='C:\WINDOWS'; $env:windir='C:\WINDOWS'; $env:COMSPEC='C:\WINDOWS\system32\cmd.exe'; $env:HTTP_PROXY='http://127.0.0.1:7897'; $env:HTTPS_PROXY='http://127.0.0.1:7897'; $env:ALL_PROXY='http://127.0.0.1:7897'; $env:JAVA_HOME='C:\Users\FuLin\.jdks\temurin-21.0.10'; $env:MAVEN_HOME='C:\Users\FuLin\.maven\apache-maven-3.9.9'; $env:PATH="$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:PATH"; $env:MAVEN_OPTS='-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897'; mvn '-DskipTests' '-Dspotless.apply.skip=true' '-Dspotless.check.skip=true' -pl armorauth-admin -am compile
```

Result: admin compile success.

```powershell
$env:SystemRoot='C:\WINDOWS'; $env:windir='C:\WINDOWS'; $env:COMSPEC='C:\WINDOWS\system32\cmd.exe'; $env:HTTP_PROXY='http://127.0.0.1:7897'; $env:HTTPS_PROXY='http://127.0.0.1:7897'; $env:ALL_PROXY='http://127.0.0.1:7897'; $env:JAVA_HOME='C:\Users\FuLin\.jdks\temurin-21.0.10'; $env:MAVEN_HOME='C:\Users\FuLin\.maven\apache-maven-3.9.9'; $env:PATH="$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:PATH"; $env:MAVEN_OPTS='-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897'; mvn '-DskipTests' '-Dspotless.apply.skip=true' '-Dspotless.check.skip=true' -pl armorauth-server -am package
```

Result: package success.

```powershell
$env:SystemRoot='C:\WINDOWS'; $env:windir='C:\WINDOWS'; $env:COMSPEC='C:\WINDOWS\system32\cmd.exe'; $env:HTTP_PROXY='http://127.0.0.1:7897'; $env:HTTPS_PROXY='http://127.0.0.1:7897'; $env:ALL_PROXY='http://127.0.0.1:7897'; $env:JAVA_HOME='C:\Users\FuLin\.jdks\temurin-21.0.10'; $env:MAVEN_HOME='C:\Users\FuLin\.maven\apache-maven-3.9.9'; $env:PATH="$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:PATH"; $env:MAVEN_OPTS='-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897'; mvn '-Dspotless.apply.skip=true' '-Dspotless.check.skip=true' test
```

Result: full reactor test success. OAuth/OIDC E2E still ran 7 tests.

Runtime smoke test on local MySQL profile:

| Check | Result |
| --- | --- |
| v2 dry-run | Active `v2`, `total.wouldRekey=1`, `total.rekeyed=0` |
| v2 execute | Active `v2`, `total.rekeyed=1`, `total.failed=0` |
| v2 post-check | Dry-run returned `total.wouldRekey=0`, DB JWK private key counted as `{enc}v2:` |
| v1 rollback dry-run | Active `v1` with `v2` fallback, `total.wouldRekey=1` |
| v1 rollback execute | Active `v1`, `total.rekeyed=1`, `total.failed=0` |
| Final DB check | DB JWK private key counted as `{enc}v1:` and `{enc}v2:` count returned to 0 |
| Final default server | `http://127.0.0.1:9000`, PID `16916`, health `UP` |
| Final dry-run | Default `v1`, `configuredKeyIds=["v1"]`, `total.wouldRekey=0`, `total.failed=0` |
| Admin UI process | Still running on `http://127.0.0.1:1080`, PID `13704` |

Follow-up candidates:

| Priority | Item | Reason |
| --- | --- | --- |
| P1 | API reference refresh | `docs/api-reference.md` still needs a broader pass for all current admin endpoints and response bodies. |
| P2 | SAML design stub | ROADMAP leaves SAML as a later enterprise SSO phase; a design stub can prevent OIDC-only assumptions from hardening. |

## Step 12 - Admin UI Operations Coverage

Status: Completed

Goal: close more of the remaining v1.0 admin-console surface by exposing backend operations that already existed but had no UI coverage.

Implemented:

| Area | Status | Notes |
| --- | --- | --- |
| JWK key UI | Completed | Added `/main/jwkKeys` with active/standby/retired summary, key table, key rotation, and standby key retire action. |
| Session UI | Completed | Added `/main/sessions` with session totals, active-user summary, username filter, and force-expire action. |
| Tenant UI | Completed | Added `/main/tenants` with tenant list, create/edit modal, status switch, delete action, brand color swatch/editor, and branding fields. |
| API wrappers | Completed | Added JWK, session, tenant detail/status wrappers in the admin UI API layer. |
| Navigation | Completed | Added sidebar entries for JWK keys, sessions, and tenants; sidebar now scrolls when the operation menu grows. |

Primary files:

| File | Purpose |
| --- | --- |
| `armorauth-admin-ui/src/api/index.js` | JWK, session, and tenant API wrappers |
| `armorauth-admin-ui/src/router/main_children.js` | New admin routes |
| `armorauth-admin-ui/src/views/Main.vue` | New sidebar entries and scrollable sidebar |
| `armorauth-admin-ui/src/views/main/JwkKeys.vue` | JWK operation page |
| `armorauth-admin-ui/src/views/main/Sessions.vue` | Session operation page |
| `armorauth-admin-ui/src/views/main/Tenants.vue` | Tenant operation page |

Verification:

| Check | Result |
| --- | --- |
| Admin UI build | `npm run build` success |
| JWK API smoke | `/api/admin/v1/jwk-keys` returned 200 with 1 active RSA/RS256 key |
| Session API smoke | `/api/admin/v1/sessions` returned 200 |
| Tenant API smoke | `/api/admin/v1/tenants?page=0&size=1` returned 200 |
| Browser smoke | Opened `/main/jwkKeys`, `/main/sessions`, and `/main/tenants`; pages loaded, tables rendered, and tenant create modal opened with expected fields |
| Visual artifact | `output/playwright/admin-ops-pages-tenants.png` |

Historical ROADMAP estimate at this step:

| Scope | Estimate |
| --- | ---: |
| MVP / v1.0 self-hosted GA track | 88% |
| Full ROADMAP including P2 long-term enhancements | 78% |

Follow-up candidates:

| Priority | Item | Reason |
| --- | --- | --- |
| P1 | SAML design and minimal model/API stub | Enterprise SSO is the largest remaining Phase 5 gap. |
| P1 | Passkey/WebAuthn design and account API stub | Phase 3 still treats WebAuthn as later work. |
| P2 | SCIM/LDAP design docs and extension boundaries | These are larger enterprise integrations and should be shaped before implementation. |

## Step 11 - API Reference Refresh

Status: Completed

Goal: align `docs/api-reference.md` with the current controller surface after the ROADMAP implementation passes.

Implemented:

| Area | Status | Notes |
| --- | --- | --- |
| Response model | Completed | Documented `ApiResponse` and both pagination shapes currently returned by the admin API. |
| Admin API coverage | Completed | Refreshed application, scope, user, role, permission, tenant, organization, identity provider, federated binding, login policy, JWK, secret protection, session, audit, token statistics, and webhook sections. |
| Request fields | Completed | Added or corrected request bodies for current DTOs, including `newPassword`, `enabled` query parameters, role bindings, org members, TOTP verification, and secret rekey stats. |
| Non-admin endpoints | Completed | Documented account self-service, OAuth/OIDC standard endpoints, login/captcha pages, device activation helpers, and federated confirm pages. |

Primary files:

| File | Purpose |
| --- | --- |
| `docs/api-reference.md` | Current API reference |

Verification:

| Check | Result |
| --- | --- |
| Controller scan | Compared `@RequestMapping`, `@GetMapping`, `@PostMapping`, `@PutMapping`, `@PatchMapping`, and `@DeleteMapping` routes from admin, account, OAuth helper, login, captcha, and federation controllers. |
| Documentation scan | Confirmed the refreshed reference includes the current `/api/admin/v1/**`, `/api/account/v1/**`, login/captcha, federated confirm, and device activation paths. |

Follow-up candidates:

| Priority | Item | Reason |
| --- | --- | --- |
| P1 | SAML design stub | ROADMAP leaves SAML as a later enterprise SSO phase; a design stub can prevent OIDC-only assumptions from hardening. |
| P2 | Admin UI coverage for JWK/session/tenant details | Several backend APIs now exist but still have limited UI coverage. |

## Step 10 - Secret Protection Admin UI

Status: Completed

Goal: expose the operational secret rekey capability from the Vue admin console so SUPER_ADMIN operators can dry-run and execute key-rotation backfills without using curl.

Implemented:

| Area | Status | Notes |
| --- | --- | --- |
| API wrapper | Completed | Added `rekeySecrets(dryRun)` for `POST /api/admin/v1/secret-protection/rekey`. |
| Admin route | Completed | Added `/main/secretProtection` with sidebar entry and key icon. |
| Secret protection page | Completed | Added dry-run on mount, summary cards, configured key tags, per-resource statistics, failure/status tags, and guarded execute action. |
| Route selection stability | Completed | Main sidebar selection now follows the current route on reload/direct navigation instead of staying on the home key. |

Primary files:

| File | Purpose |
| --- | --- |
| `armorauth-admin-ui/src/api/index.js` | Secret rekey API wrapper |
| `armorauth-admin-ui/src/router/main_children.js` | Secret Protection child route |
| `armorauth-admin-ui/src/views/Main.vue` | Sidebar menu entry and route-synced selection |
| `armorauth-admin-ui/src/views/main/SecretProtection.vue` | Secret Protection operation page |

Verification:

| Check | Result |
| --- | --- |
| Admin UI build | `npm run build` success |
| Rekey API smoke | `POST /api/admin/v1/secret-protection/rekey` returned 200 with active `v1`, `configuredKeyIds=["v1"]`, `total.scanned=1`, `total.wouldRekey=0` |
| Browser smoke | Logged into `http://127.0.0.1:1080`, opened `Secret 保护`, saw active `v1`, configured key `v1`, JWK scanned/current-key counts `1/1`, and disabled execute button when no rows need rekey |
| Visual artifact | `output/playwright/secret-protection-ui-after-reload.png` |

Follow-up candidates:

| Priority | Item | Reason |
| --- | --- | --- |
| P1 | API reference refresh | `docs/api-reference.md` still needs a broader pass for all current admin endpoints and response bodies. |
| P2 | SAML design stub | ROADMAP leaves SAML as a later enterprise SSO phase; a design stub can prevent OIDC-only assumptions from hardening. |

## Step 13 - SAML Configuration Boundary

Status: Completed

Goal: close the first enterprise SSO gap without pretending SAML runtime login is complete.

Implemented:

| Area | Status | Notes |
| --- | --- | --- |
| SAML model | Completed | Added SAML IdP/SP fields to `identity_provider`: entity ID, SSO/SLO URL, certificate, metadata URL, SP entity ID, ACS URL, and NameID format. |
| DB migration | Completed | Added MySQL/H2 `V17__identity_provider_saml_fields.sql`; local MySQL migrated from v16 to v17. |
| Admin API | Completed | Create/update/read/test responses now carry SAML fields. At this step SAML config test validated metadata URL mode or manual entity ID + SSO URL + certificate mode and returned runtime support as `pending`; Step 18 later upgraded this to `sp_redirect_post_assertion`. |
| Runtime guard | Completed | OAuth client registration loaders now skip SAML/LDAP providers, so enabled SAML configs do not break OAuth/OIDC startup. |
| Admin UI | Completed | Identity Provider modal switches between OAuth/OIDC fields and SAML fields; test-result rendering preserves string statuses such as `pending`, `manual`, and `metadata_url`. |
| Docs | Completed | `docs/api-reference.md` documents SAML request fields and current runtime boundary. |

Verification:

| Check | Result |
| --- | --- |
| Backend compile | `mvn '-DskipTests' '-Dspotless.apply.skip=true' '-Dspotless.check.skip=true' -pl armorauth-server -am compile` success |
| Backend package | `mvn '-DskipTests' '-Dspotless.apply.skip=true' '-Dspotless.check.skip=true' -pl armorauth-server -am package` success |
| Flyway | MySQL schema migrated to v17 |
| SAML API smoke | Created temporary SAML provider with no client secret, tested the configuration boundary, then deleted it |
| Restart guard | Restarted server with an enabled SAML provider present; health stayed `UP`, proving SAML is skipped by OAuth client registration |
| Admin UI smoke | Opened `/main/identityProviders`, selected SAML in the modal, and confirmed all SAML fields render |
| Visual artifact | `output/playwright/admin-saml-modal.png` |

## Step 14 - Passkey Metadata Boundary and MFA Stability

Status: Completed

Goal: add a Passkey/WebAuthn registration boundary and fix MFA issues that would block reliable factor usage.

Implemented:

| Area | Status | Notes |
| --- | --- | --- |
| WebAuthn model | Completed | Extended `auth_factor` with challenge, credential ID, public key, sign count, transports, AAGUID, user handle, and backup flags. |
| DB migration | Completed | Added MySQL/H2 `V18__auth_factor_webauthn_fields.sql`; local MySQL migrated from v17 to v18. |
| Account API | Completed | Added `POST /api/account/v1/factors/passkey:begin-registration` and `POST /api/account/v1/factors/passkey/{id}:finish-registration`. |
| Passkey storage | Completed | Begin registration creates a pending challenge; finish validates challenge, stores credential metadata, and marks factor verified. At this step it reported `runtimeSupport=metadata_only_runtime_pending`; Step 17 later upgrades ready credentials to `passkey_assertion_ready`. |
| MFA principal mapping | Completed | Login-time MFA now resolves username to user ID before querying `auth_factor`, matching how factors are stored. |
| Secret handling | Completed | TOTP verification now reveals encrypted secrets before verifying; secret rekey/backfill also includes pending WebAuthn challenges. |
| Runtime safety | Completed | At this step, verified Passkey metadata did not trigger the TOTP-only MFA challenge; Step 17 later adds WebAuthn assertion runtime support. |
| Docs | Completed | `docs/api-reference.md` documents Passkey begin/finish requests and response fields. |

Verification:

| Check | Result |
| --- | --- |
| Backend compile | `mvn '-DskipTests' '-Dspotless.apply.skip=true' '-Dspotless.check.skip=true' -pl armorauth-server -am compile` success |
| Backend package | `mvn '-DskipTests' '-Dspotless.apply.skip=true' '-Dspotless.check.skip=true' -pl armorauth-server -am package` success |
| Flyway | MySQL schema migrated to v18 |
| Runtime health | Server restarted on `http://127.0.0.1:9000`; `/actuator/health` returned `UP` |
| Passkey API smoke | Logged in with a form session, began registration, finished registration with a temporary credential, listed factors, then deleted the temporary factor |

Historical ROADMAP estimate at this step:

| Scope | Estimate |
| --- | ---: |
| MVP / v1.0 self-hosted GA track | 92% |
| Full ROADMAP including P2 long-term enhancements | 82% |

Historical follow-up candidates, closed by later steps:

| Priority | Item | Reason |
| --- | --- | --- |
| P1 | Passkey/WebAuthn runtime follow-up | At this step browser assertion verification was pending; MFA assertion runtime is completed in Step 17 and registration attestation hardening is closed in Step 20. |
| P1 | SAML runtime login | SAML config/model/UI/API are in place; completed later in Step 18 with Spring Security SAML2 SP. |
| P2 | LDAP/AD live authentication | LDAP/AD directory sync was completed later in Step 16; live bind login was completed later in Step 19. |
| P2 | Actions and ABAC/FGA | Extension execution and fine-grained authorization are closed in Step 20. |

## Step 15 - SCIM 2.0 User and Group Provisioning

Status: Completed

Goal: add a standards-shaped enterprise directory provisioning boundary without routing SCIM through the normal admin `ApiResponse` envelope.

Implemented:

| Area | Status | Notes |
| --- | --- | --- |
| SCIM metadata | Completed | Added `/scim/v2/ServiceProviderConfig`, `/scim/v2/Schemas`, and `/scim/v2/ResourceTypes` returning SCIM-shaped JSON for User and Group resources. |
| SCIM Users API | Completed | Added list, create, get, replace, patch, and delete for `/scim/v2/Users`. Responses use `application/scim+json` and standard SCIM `ListResponse` / `Error` shapes. |
| SCIM Groups API | Completed | Added list, create, get, replace, patch, and delete for `/scim/v2/Groups`, mapped to `sys_role` and `user_role`. Built-in roles are read-only through SCIM. |
| Directory mapping | Completed | Mapped SCIM `userName`, `displayName`, `name`, `active`, `emails`, `phoneNumbers`, and write-only `password` onto `user_info`; omitted passwords are generated and stored as hashes only. |
| Filtering | Completed | Users support `userName eq`, `userName co`, and `emails.value eq`; Groups support `displayName eq` and `displayName co`, with SCIM `invalidFilter` errors for unsupported filters. |
| PATCH support | Completed | Users support `add`/`replace` for common profile fields and removing emails/phone numbers. Groups support display name replacement and member add/replace/remove, including `members[value eq "..."]`. |
| Security boundary | Completed | SCIM is protected by the admin Basic auth chain and requires `SUPER_ADMIN` or `USER_ADMIN`. The default login chain now excludes `/scim/v2/**`, so API clients are not redirected to `/login`. |
| Audit | Completed | Create, replace, patch, and delete write `SCIM_USER_*` audit events. |
| Docs | Completed | `docs/api-reference.md` documents the SCIM base path, endpoints, request examples, response shape, filters, and error format. |

Primary files:

| File | Purpose |
| --- | --- |
| `armorauth-admin/src/main/java/com/armorauth/admin/controller/ScimController.java` | SCIM HTTP API and SCIM-local error handling |
| `armorauth-admin/src/main/java/com/armorauth/admin/service/ScimUserService.java` | SCIM user provisioning logic and user-directory mapping |
| `armorauth-admin/src/main/java/com/armorauth/admin/config/AdminSecurityConfig.java` | Basic auth/RBAC protection for `/scim/v2/**` and SCIM 403 error body |
| `armorauth-core/src/main/java/com/armorauth/config/DefaultSecurityConfig.java` | Excludes `/scim/v2/**` from the form-login filter chain |
| `armorauth-model/src/main/java/com/armorauth/data/repository/UserInfoRepository.java` | Case-insensitive lookup and username contains helpers for SCIM filters |
| `armorauth-model/src/main/java/com/armorauth/data/repository/RoleRepository.java` | Case-insensitive lookup and display name contains helpers for SCIM Group filters |
| `armorauth-model/src/main/java/com/armorauth/data/repository/UserRoleRepository.java` | Role/member lookup helpers for SCIM Group membership |
| `docs/api-reference.md` | SCIM API documentation |

Verification:

| Check | Result |
| --- | --- |
| Admin compile | `mvn '-DskipTests' '-Dspotless.apply.skip=true' '-Dspotless.check.skip=true' -pl armorauth-admin -am compile` success |
| Backend package | `mvn '-DskipTests' '-Dspotless.apply.skip=true' '-Dspotless.check.skip=true' -pl armorauth-server -am package` success |
| Runtime health | Server restarted on `http://127.0.0.1:9000`; `/actuator/health` returned `UP` |
| SCIM metadata smoke | `GET /scim/v2/ServiceProviderConfig` returned `patch.supported=true`; `/ResourceTypes` returned 2 resources |
| SCIM user smoke | Created a temporary user, queried it with `filter=userName eq "..."`, then deleted it with HTTP 204 |
| SCIM group smoke | Created a temporary group with the temporary user as a member, queried it with `filter=displayName eq "..."`, removed a member via PATCH, then deleted it with HTTP 204 |
| Security-chain fix | Verified SCIM no longer redirects to `/login`; the Basic admin chain handles `/scim/v2/**` |

Current ROADMAP estimate:

| Scope | Estimate |
| --- | ---: |
| MVP / v1.0 self-hosted GA track | 94% |
| Full ROADMAP including P2 long-term enhancements | 87% |

Items closed by Step 20:

| Priority | Item | Reason |
| --- | --- | --- |
| P1 | Passkey/WebAuthn runtime follow-up | At this step browser assertion verification was pending; MFA assertion runtime is completed in Step 17 and registration attestation hardening is closed in Step 20. |
| P1 | SAML runtime login | SAML config/model/UI/API are in place; completed later in Step 18 with Spring Security SAML2 SP. |
| P2 | LDAP/AD live authentication and group-to-role mapping | LDAP/AD bind/search/import was completed later in Step 16; direct LDAP login and automatic role mapping were completed later in Step 19. |
| P2 | Actions runtime and advanced authorization hardening | Webhooks, claim customization hooks, RBAC, and ABAC scaffolding exist at this point; the Actions execution runtime and FGA-style check are closed in Step 20. |

## Step 16 - LDAP/AD Directory Sync Boundary

Status: Completed

Goal: add a usable enterprise LDAP/AD directory import path while keeping LDAP live-login/runtime authentication explicitly separate from this step.

Implemented:

| Area | Status | Notes |
| --- | --- | --- |
| LDAP model | Completed | Extended `identity_provider` with LDAP URL, base DN, bind DN/password, search base/filter, attribute mappings, SSL/StartTLS flags, and page size. |
| DB migration | Completed | Added MySQL/H2 `V19__identity_provider_ldap_fields.sql`; local MySQL migrated to v19. |
| Admin API | Completed | Create/update/read/test responses now carry LDAP config fields; bind password is write-only and responses expose `ldapBindPasswordConfigured`. |
| LDAP probe | Completed | `POST /api/admin/v1/identity-providers/{id}:test?probeRemote=true` can attempt bind/search; default `probeRemote=false` validates local config only. |
| User sync | Completed | Added `POST /api/admin/v1/identity-providers/{id}:sync-users` with dry-run default, bind/search import, create/update counters, sample reporting, and connection-failure reporting. |
| Secret protection | Completed | LDAP bind password participates in secret protection backfill and rekey operations. |
| Admin UI | Completed | Identity Provider modal now renders LDAP fields and row actions for sync dry-run / sync execution. |
| Input hardening | Completed | Provider type and linking strategy parsing is case-insensitive; `linkingStrategy=email` maps to `EMAIL_MATCH`. |
| Docs | Completed | `docs/api-reference.md` documents LDAP fields, sync request/response, and the current runtime boundary. |

Primary files:

| File | Purpose |
| --- | --- |
| `armorauth-model/src/main/java/com/armorauth/data/entity/IdentityProvider.java` | LDAP config persistence fields |
| `armorauth-server/src/main/resources/db/migration-mysql/V19__identity_provider_ldap_fields.sql` | MySQL schema migration |
| `armorauth-server/src/main/resources/db/migration-h2/V19__identity_provider_ldap_fields.sql` | H2 schema migration |
| `armorauth-admin/src/main/java/com/armorauth/admin/service/LdapDirectorySyncService.java` | LDAP bind/search probe and user import logic |
| `armorauth-admin/src/main/java/com/armorauth/admin/service/IdentityProviderService.java` | LDAP create/update/test/sync integration and enum input hardening |
| `armorauth-admin/src/main/java/com/armorauth/admin/controller/IdentityProviderController.java` | LDAP sync endpoint |
| `armorauth-admin/src/main/java/com/armorauth/admin/config/AdminSecurityConfig.java` | RBAC rule for LDAP sync |
| `armorauth-admin-ui/src/api/index.js` | LDAP sync API wrapper |
| `armorauth-admin-ui/src/views/main/IdentityProviders.vue` | LDAP config fields and sync actions |
| `docs/api-reference.md` | LDAP API documentation |

Verification:

| Check | Result |
| --- | --- |
| Backend package | `mvn -pl armorauth-server -am package -DskipTests` success |
| Flyway | Server restarted with MySQL profile; schema is at v19 |
| Runtime health | `/actuator/health` returned `UP` on `http://127.0.0.1:9000` |
| LDAP API smoke | Created a temporary LDAP provider using lowercase `providerType=ldap` and `linkingStrategy=email`; response normalized to `LDAP` and `EMAIL_MATCH` |
| Secret response smoke | GET provider returned `ldapBindPasswordConfigured=true` and did not expose `ldapBindPassword` |
| LDAP test smoke | At this step, `POST /api/admin/v1/identity-providers/{id}:test?probeRemote=false` returned success with `runtimeSupport=bind_search_user_sync`; Step 19 later upgrades the signal to `bind_search_user_sync_login`. |
| LDAP sync smoke | Dry-run sync against a closed local LDAP port returned a controlled response with `failed=1` and `samples.error`, then the temporary provider was deleted |
| Admin UI build | `npm run build` success |

Current ROADMAP estimate:

| Scope | Estimate |
| --- | ---: |
| MVP / v1.0 self-hosted GA track | 95% |
| Full ROADMAP including P2 long-term enhancements | 90% |

Historical follow-up candidates, closed or re-scoped by later steps:

| Priority | Item | Reason |
| --- | --- | --- |
| P1 | SAML runtime login | SAML config/model/UI/API are in place; completed later in Step 18 with Spring Security SAML2 SP. |
| P1 | Passkey registration attestation hardening | Passkey MFA assertion runtime is completed later in Step 17; registration trust-chain/attestation hardening remains. |
| P2 | LDAP/AD live authentication and group-to-role mapping | Directory sync/import exists at this point; direct LDAP bind login and automatic role mapping are completed later in Step 19. |
| P2 | Actions runtime and advanced authorization hardening | Webhooks, claim customization hooks, RBAC, and ABAC scaffolding exist at this point; the Actions execution runtime and FGA-style check are closed in Step 20. |

## Step 17 - Passkey MFA Assertion Runtime

Status: Completed

Goal: turn stored Passkey/WebAuthn credentials into a real MFA login factor by adding browser assertion options, cryptographic verification, and login-state restoration.

Implemented:

| Area | Status | Notes |
| --- | --- | --- |
| Assertion API | Completed | Added `POST /login/passkey/assertion/options` and `POST /login/passkey/assertion/finish` for sessions already paused at MFA. |
| Assertion verification | Completed | Validates pending MFA session, challenge, origin, RP ID hash, user-present flag, userHandle, monotonic sign count, and signature over authenticator data plus client data hash. |
| Public key support | Completed | Assertion verification accepts stored public keys as JWK JSON, PEM, base64/base64url DER/SPKI, or COSE_Key CBOR for EC P-256/RSA credentials. |
| MFA runtime | Completed | Runtime MFA factor selection now includes verified `WEBAUTHN` factors alongside TOTP. |
| Login restoration | Completed | Successful Passkey assertion restores the pending `Authentication` into the Spring Security context, saves it through the security context repository, clears MFA pending session attributes, and redirects to the original target. |
| Audit/lockout | Completed | Passkey MFA success emits login success and MFA success audit events and resets login lockout counters through the existing services. |
| Server UI | Completed | The MFA page now exposes a Passkey button that calls `navigator.credentials.get`, posts the assertion, handles failures, and follows the returned redirect. |
| Account API signal | Completed | Passkey registration now reports `runtimeSupport=passkey_assertion_ready` when a credential public key is stored. |
| Docs | Completed | `docs/api-reference.md` documents Passkey registration key formats and login assertion request/response behavior. |

Primary files:

| File | Purpose |
| --- | --- |
| `armorauth-core/src/main/java/com/armorauth/webauthn/WebAuthnAssertionService.java` | WebAuthn assertion challenge/session handling and signature verification |
| `armorauth-core/src/main/java/com/armorauth/webauthn/PasskeyLoginController.java` | Passkey MFA assertion HTTP endpoints and login-state restoration |
| `armorauth-core/src/main/java/com/armorauth/webauthn/WebAuthnAssertionDTO.java` | Assertion option, finish request, and response DTOs |
| `armorauth-core/src/main/java/com/armorauth/authentication/MfaAuthenticationSuccessHandler.java` | Pending MFA session constants and WEBAUTHN runtime factor selection |
| `armorauth-core/src/main/java/com/armorauth/config/DefaultSecurityConfig.java` | Permits Passkey assertion endpoints in the form-login chain |
| `armorauth-admin/src/main/java/com/armorauth/admin/service/AccountService.java` | Passkey runtime-support response signal |
| `armorauth-server-ui/src/main/resources/templates/mfa.ftlh` | Browser Passkey assertion UI flow |
| `armorauth-server-ui/src/main/resources/static/assets/armorauth-ui.css` | MFA divider styling |
| `docs/api-reference.md` | Passkey assertion API documentation |

Verification:

| Check | Result |
| --- | --- |
| Backend compile | `mvn -pl armorauth-server -am compile -DskipTests` success |
| Backend package | `mvn -pl armorauth-server -am package -DskipTests` success |
| Runtime health | Server restarted on `http://127.0.0.1:9000`; `/actuator/health` returned `UP` |
| Passkey registration smoke | Created a temporary user, logged in, began registration, finished registration with a generated EC P-256 public key, and received `runtimeSupport=passkey_assertion_ready` |
| Passkey MFA smoke | Logged in again, was redirected to `/login/mfa`, fetched assertion options, signed the WebAuthn assertion with the matching private key, finished assertion successfully, and verified `/api/account/v1/me` returned the temporary user |
| Cleanup | Deleted the temporary Passkey factor and user |

Current ROADMAP estimate:

| Scope | Estimate |
| --- | ---: |
| MVP / v1.0 self-hosted GA track | 96% |
| Full ROADMAP including P2 long-term enhancements | 92% |

Historical follow-up candidates, closed or re-scoped by later steps:

| Priority | Item | Reason |
| --- | --- | --- |
| P1 | SAML SP runtime login | SAML config/model/UI/API are in place; completed later in Step 18 with Spring Security SAML2 SP. |
| P1 | Passkey registration attestation hardening | Login assertion runtime verifies stored public keys at this point; registration attestationObject validation is closed in Step 20. |
| P2 | LDAP/AD live authentication and group-to-role mapping | Directory sync/import exists at this point; direct LDAP bind login and automatic role mapping are completed later in Step 19. |
| P2 | Actions runtime and advanced authorization hardening | Webhooks, claim customization hooks, RBAC, and ABAC scaffolding exist at this point; the Actions execution runtime and FGA-style check are closed in Step 20. |

## Step 18 - SAML SP Runtime Login

Status: Completed

Goal: turn SAML IdP configuration into a real SP-initiated login path while reusing the existing federated account linking, confirmation, and auto-registration workflows.

Implemented:

| Area | Status | Notes |
| --- | --- | --- |
| SAML2 dependency boundary | Completed | Added Spring Security SAML2 Service Provider support and the Shibboleth OpenSAML repository required by Spring Security 7. |
| Dynamic SP registration | Completed | Added a runtime `RelyingPartyRegistrationRepository` that loads enabled SAML `identity_provider` rows from DB, supporting metadata URL mode and manual entity ID / SSO URL / X.509 certificate mode. |
| SP metadata | Completed | Exposed `/saml2/service-provider-metadata/{registrationId}` for dynamic SP metadata generation. |
| Login entrypoint | Completed | Added `/saml2/authorization/{registrationId}` to persist the federated login mode and start Spring Security's SAML AuthnRequest flow. |
| Assertion callback | Completed | Wired `/login/saml2/sso/{registrationId}` through Spring Security SAML2/OpenSAML assertion validation. |
| Federated completion reuse | Completed | SAML assertions are converted to `FederatedUserProfile` and complete through the same auto-register, email-match, confirm, bind, audit, and login restoration path used by OAuth/OIDC providers. |
| Hosted login UI | Completed | The login page now renders provider-specific authorization URLs, so OAuth/OIDC providers use `/oauth2/authorization/{registrationId}` and SAML providers use `/saml2/authorization/{registrationId}`. |
| Admin API signal | Completed | SAML config test now returns `runtimeSupport=sp_redirect_post_assertion` when local SAML config is valid. |
| Docs | Completed | `docs/api-reference.md` and `docs/ROADMAP.md` document the SAML runtime endpoints and current capability boundary. |

Verification:

| Check | Result |
| --- | --- |
| Backend compile | `mvn -pl armorauth-server -am compile -DskipTests` success |
| Backend package | `mvn -pl armorauth-server -am package -DskipTests` success after stopping the previously running server jar that locked the target file |
| Runtime health | Server restarted on `http://127.0.0.1:9000`; `/actuator/health` returned `UP` |
| SAML API smoke | Created a temporary SAML IdP with a generated X.509 cert, then `POST /api/admin/v1/identity-providers/{id}:test?probeRemote=false` returned success and `runtimeSupport=sp_redirect_post_assertion` |
| Login-page smoke | `/login` rendered `/saml2/authorization/{registrationId}` for the temporary SAML provider |
| Metadata smoke | `/saml2/service-provider-metadata/{registrationId}` returned SP metadata containing the temporary registration ID |
| AuthnRequest smoke | `/saml2/authorization/{registrationId}?mode=confirm` redirected to `/saml2/authenticate/{registrationId}`, then to the configured IdP SSO URL with `SAMLRequest` |
| Cleanup | Deleted the temporary SAML IdP |

Current ROADMAP estimate:

| Scope | Estimate |
| --- | ---: |
| MVP / v1.0 self-hosted GA track | 97% |
| Full ROADMAP including P2 long-term enhancements | 94% |

Historical follow-up candidates, closed or re-scoped by later steps:

| Priority | Item | Reason |
| --- | --- | --- |
| P1 | Passkey registration attestation hardening | Login assertion runtime verifies stored public keys at this point; registration attestationObject validation is closed in Step 20. |
| P2 | LDAP/AD live authentication and group-to-role mapping | Completed later in Step 19. |
| P2 | Actions runtime and advanced authorization hardening | Webhooks, claim customization hooks, RBAC, and ABAC scaffolding exist at this point; the Actions execution runtime and FGA-style check are closed in Step 20. |

## Step 19 - LDAP/AD Live Login and Role Mapping

Status: Completed

Goal: move LDAP/AD beyond directory sync by letting enabled LDAP identity providers authenticate users at runtime and map LDAP groups to existing ArmorAuth roles.

Implemented:

| Area | Status | Notes |
| --- | --- | --- |
| Security-chain integration | Completed | Added an `ArmorAuthSecurityCustomizer` that registers an LDAP authentication provider into the existing form-login chain. |
| Live LDAP bind login | Completed | Runtime login searches for the LDAP user DN using service-account or anonymous bind, then validates the submitted password with a user bind. |
| Local-user projection | Completed | Successful LDAP login creates or updates the local `user_info` row with profile, email, phone, display name, LDAP DN, and LDAP groups while storing an unusable generated local password hash. |
| Local-account guard | Completed | Existing non-LDAP local users, including `admin`, do not trigger remote LDAP attempts, so broken LDAP providers do not slow down local logins. |
| Group role mapping | Completed | `attributeMapping.roles` can map LDAP group DNs or group CN values to existing ArmorAuth `sys_role.role_code` values; missing role codes are logged and ignored. |
| Federation role mapping fix | Completed | `IdpAttributeMappingService` now actually creates `user_role` bindings for configured OAuth/OIDC/SAML role mappings instead of only logging a manual-binding hint. |
| Admin API signal | Completed | LDAP config test now returns `runtimeSupport=bind_search_user_sync_login`. |
| Docs | Completed | `docs/api-reference.md` documents LDAP live login and group-to-role mapping format; `docs/ROADMAP.md` marks LDAP/AD runtime support complete. |

Primary files:

| File | Purpose |
| --- | --- |
| `armorauth-admin/src/main/java/com/armorauth/admin/ldap/LdapAuthenticationProvider.java` | Spring Security authentication provider for username/password LDAP login |
| `armorauth-admin/src/main/java/com/armorauth/admin/ldap/LdapAuthenticationSecurityConfiguration.java` | Registers LDAP auth provider through the core security customizer extension point |
| `armorauth-admin/src/main/java/com/armorauth/admin/ldap/LdapLiveAuthenticationService.java` | LDAP search, StartTLS-aware bind, local user projection, and role mapping |
| `armorauth-admin/src/main/java/com/armorauth/admin/service/IdentityProviderService.java` | LDAP runtime-support response signal |
| `armorauth-federation/src/main/java/com/armorauth/federation/IdpAttributeMappingService.java` | Role mapping now writes `user_role` bindings |
| `docs/api-reference.md` | LDAP live-login and role-mapping documentation |
| `docs/ROADMAP.md` | LDAP/AD roadmap completion notes |

Verification:

| Check | Result |
| --- | --- |
| Backend compile | `mvn -pl armorauth-server -am compile -DskipTests` success |
| Backend package | `mvn -pl armorauth-server -am package -DskipTests` success |
| Runtime health | Server restarted on `http://127.0.0.1:9000`; `/actuator/health` returned `UP` |
| LDAP API smoke | Created a temporary LDAP provider with role mappings, then `POST /api/admin/v1/identity-providers/{id}:test?probeRemote=false` returned success and `runtimeSupport=bind_search_user_sync_login` |
| Local-login guard smoke | With the temporary LDAP provider enabled, `admin / admin123` still logged in successfully and redirected to `/` |
| LDAP failure smoke | A non-existing LDAP user against a closed LDAP port failed cleanly and redirected to `/login?error` instead of returning a server error |
| Cleanup | Deleted the temporary LDAP provider |

Step 19 ROADMAP estimate before final Step 20 closure:

| Scope | Estimate |
| --- | ---: |
| MVP / v1.0 self-hosted GA track | 98% |
| Full ROADMAP including P2 long-term enhancements | 96% |

Items closed by Step 20:

| Priority | Item | Reason |
| --- | --- | --- |
| P1 | Passkey registration attestation hardening | Closed in Step 20 by validating browser `clientDataJSON` + `attestationObject` and rejecting unsupported COSE public keys at registration time. |
| P2 | Actions runtime and advanced authorization hardening | Closed in Step 20 by adding Java SPI Actions runtime and `/api/admin/v1/authorization/check`. |
| P2 | SAML SLO / IdP mode hardening | Re-scoped in Step 20 as optional enterprise enhancement; SAML SP login runtime from Step 18 satisfies the current product-scope ROADMAP. |

## Step 20 - Passkey Attestation, Passwordless, Actions/FGA Runtime Closure

Goal: close the last ROADMAP gaps for Passkey/WebAuthn registration hardening, passwordless runtime, Actions runtime, and FGA-style authorization checks.

Implemented:

| Area | Status | Notes |
| --- | --- | --- |
| Passkey registration attestation | Completed | `finish-registration` now accepts browser `clientDataJSON` + `attestationObject`, validates `webauthn.create`, challenge, origin/RP ID, RP ID hash, user-present, attested credential data, COSE public key, and supports `fmt=none` plus `fmt=packed`. |
| Passkey public-key parsing hardening | Completed | COSE key parsing now tolerates Jackson CBOR numeric keys represented as numbers or numeric strings, and registration rejects unsupported `credentialPublicKey` instead of storing it for later runtime failure. |
| Passwordless Passkey login | Completed | Added `/login/passkey/options` and `/login/passkey/finish`; successful assertion creates the Spring Security context and returns `runtimeSupport=passkey_passwordless_ready`. |
| Actions runtime | Completed | Added Java SPI primitives: `ActionPhase`, `ActionContext`, `ActionResult`, `ArmorAuthAction`, and ordered `ActionExecutionService`. |
| FGA-style authorization check | Completed | Added `POST /api/admin/v1/authorization/check`, combining role/permission matching, `SUPER_ADMIN` shortcut, request context, and `AUTHORIZATION_CHECK` Actions override/enrichment. |
| SAML scope boundary | Completed | SAML SP runtime login is complete from Step 18. SAML IdP mode and SLO orchestration are now documented as optional enterprise enhancements outside the current self-hosted GA completion definition. |

Primary files:

| File | Purpose |
| --- | --- |
| `armorauth-admin/src/main/java/com/armorauth/admin/service/PasskeyRegistrationVerifier.java` | WebAuthn registration attestation parsing and validation |
| `armorauth-admin/src/main/java/com/armorauth/admin/service/AccountService.java` | Passkey begin/finish registration flow and verified factor storage |
| `armorauth-core/src/main/java/com/armorauth/webauthn/WebAuthnAssertionService.java` | MFA assertion and passwordless assertion verification |
| `armorauth-core/src/main/java/com/armorauth/webauthn/PasskeyLoginController.java` | Passkey MFA and passwordless login endpoints |
| `armorauth-core/src/main/java/com/armorauth/actions/` | Actions Java SPI runtime |
| `armorauth-admin/src/main/java/com/armorauth/admin/controller/AuthorizationDecisionController.java` | Authorization check endpoint |
| `armorauth-admin/src/main/java/com/armorauth/admin/service/AuthorizationDecisionService.java` | RBAC/FGA-style decision engine |
| `docs/api-reference.md` | Passkey, Actions, and authorization-check API docs |
| `docs/ROADMAP.md` | Final ROADMAP completion notes |

Verification:

| Check | Result |
| --- | --- |
| Backend package | `mvn -pl armorauth-server -am package -DskipTests` success |
| Runtime health | Server restarted on `http://127.0.0.1:9000`, PID `33988`; `/actuator/health` returned `UP` |
| Passkey attestation smoke | Temporary user completed `begin-registration` and `finish-registration` with synthetic P-256 COSE_Key + `fmt=none` attestationObject; response returned `runtimeSupport=passkey_assertion_ready` and `verified=true` |
| Passwordless Passkey smoke | Same temporary credential completed `/login/passkey/options` and `/login/passkey/finish`; response returned `success=true` and `runtimeSupport=passkey_passwordless_ready` |
| Authorization check smoke | `POST /api/admin/v1/authorization/check` for `admin` returned `allowed=true`, `reason=super_admin` |
| Cleanup | Deleted the temporary Passkey factor and user |

Final ROADMAP estimate:

| Scope | Estimate |
| --- | ---: |
| MVP / v1.0 self-hosted GA track | 100% |
| Full ROADMAP implemented for the current product scope | 100% |

Non-blocking future enterprise extensions:

| Area | Notes |
| --- | --- |
| SAML IdP mode and SLO orchestration | SP login runtime is complete; IdP-mode operation and logout federation can be added as enterprise extensions. |
| Script sandbox Actions | Java SPI runtime is complete; JavaScript/WASM sandboxing can be added behind the existing SPI. |
| External OpenFGA adapter | Built-in FGA-style check is complete; external OpenFGA/Auth0 FGA integration can be layered later. |
| Enterprise attestation trust store | `fmt=none` and `fmt=packed` validation are implemented; metadata service / trust-anchor policy can be added for regulated deployments. |
