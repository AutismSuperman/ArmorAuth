/*
 * Copyright (c) 2023-present ArmorAuth. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.armorauth;

import com.armorauth.data.repository.OAuth2ClientRepository;
import com.armorauth.data.repository.AuditEventRepository;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("oauth-e2e-test")
class OAuth2OidcFlowE2eTest {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";
    private static final String CONFIDENTIAL_CLIENT_ID = "f62ac251-36d7-42c8-9f75-c31c90111bd4";
    private static final String CONFIDENTIAL_CLIENT_SECRET = "secret";
    private static final String CONFIDENTIAL_CLIENT_REDIRECT_URI = "http://armorauth-demo:8083/login/oauth2/code/autism";
    private static final String DCR_REGISTRAR_CLIENT_ID = "dcr-registrar";
    private static final String DCR_REGISTRAR_CLIENT_SECRET = "secret";
    private static final String TENANT_CLIENT_ID = "acme-client";
    private static final String TENANT_CLIENT_SECRET = "secret";
    private static final String PUBLIC_CLIENT_ID = "spring-pkce";
    private static final String PUBLIC_CLIENT_REDIRECT_URI = "http://localhost:8086/login/oauth2/code/spring-pkce";
    private static final String DEVICE_CLIENT_ID = "8ee3a98e-89a8-438d-a314-1ef9df815279";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @LocalServerPort
    private int port;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private OAuth2ClientRepository oAuth2ClientRepository;

    @Test
    void clientCredentialsIssuesBearerAccessToken() throws Exception {
        HttpClient http = newHttpClient();

        HttpResponse<String> response = postForm(http, "/oauth2/token", Map.of(
                "grant_type", "client_credentials",
                "scope", "message.read"
        ), basicAuth(CONFIDENTIAL_CLIENT_ID, CONFIDENTIAL_CLIENT_SECRET));

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        JsonNode body = readJson(response);
        assertThat(body.path("access_token").asText()).isNotBlank();
        assertThat(body.path("token_type").asText()).isEqualToIgnoringCase("Bearer");
        assertThat(body.path("scope").asText()).contains("message.read");
    }

    @Test
    void authorizationCodeWithPkceIssuesOidcTokens() throws Exception {
        HttpClient browser = newHttpClient();
        JsonNode token = loginAndExchangePublicPkceToken(browser, "openid profile email message.read");

        assertThat(token.path("access_token").asText()).isNotBlank();
        assertThat(token.path("id_token").asText()).isNotBlank();
        assertThat(token.path("scope").asText()).contains("openid", "profile", "email", "message.read");

        HttpResponse<String> userInfo = getWithBearer(newHttpClient(), "/userinfo", token.path("access_token").asText());
        assertThat(userInfo.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(readJson(userInfo).path("sub").asText()).isEqualTo(ADMIN_USERNAME);
    }

    @Test
    void discoveryAndJwksExposeProtocolMetadata() throws Exception {
        HttpClient http = newHttpClient();
        String issuer = issuer();

        HttpResponse<String> oidcDiscovery = get(http, uri("/.well-known/openid-configuration"));
        assertThat(oidcDiscovery.statusCode()).isEqualTo(HttpStatus.OK.value());
        JsonNode oidc = readJson(oidcDiscovery);
        assertThat(oidc.path("issuer").asText()).isEqualTo(issuer);
        assertThat(oidc.path("authorization_endpoint").asText()).isEqualTo(issuer + "/oauth2/authorize");
        assertThat(oidc.path("token_endpoint").asText()).isEqualTo(issuer + "/oauth2/token");
        assertThat(oidc.path("jwks_uri").asText()).isEqualTo(issuer + "/oauth2/jwks");
        assertThat(oidc.path("userinfo_endpoint").asText()).isEqualTo(issuer + "/userinfo");
        assertThat(oidc.path("revocation_endpoint").asText()).isEqualTo(issuer + "/oauth2/revoke");
        assertThat(oidc.path("introspection_endpoint").asText()).isEqualTo(issuer + "/oauth2/introspect");
        assertThat(oidc.path("end_session_endpoint").asText()).isEqualTo(issuer + "/connect/logout");
        assertThat(oidc.path("pushed_authorization_request_endpoint").asText()).isEqualTo(issuer + "/oauth2/par");
        assertThat(oidc.path("registration_endpoint").asText()).isEqualTo(issuer + "/connect/register");
        assertThat(jsonArrayValues(oidc, "grant_types_supported"))
                .contains("authorization_code", "client_credentials", "refresh_token");
        assertThat(jsonArrayValues(oidc, "code_challenge_methods_supported")).contains("S256");

        HttpResponse<String> oauthDiscovery = get(http, uri("/.well-known/oauth-authorization-server"));
        assertThat(oauthDiscovery.statusCode()).isEqualTo(HttpStatus.OK.value());
        JsonNode oauth = readJson(oauthDiscovery);
        assertThat(oauth.path("issuer").asText()).isEqualTo(issuer);
        assertThat(oauth.path("jwks_uri").asText()).isEqualTo(issuer + "/oauth2/jwks");
        assertThat(oauth.path("revocation_endpoint").asText()).isEqualTo(issuer + "/oauth2/revoke");
        assertThat(oauth.path("introspection_endpoint").asText()).isEqualTo(issuer + "/oauth2/introspect");
        assertThat(oauth.path("pushed_authorization_request_endpoint").asText()).isEqualTo(issuer + "/oauth2/par");

        HttpResponse<String> jwksResponse = get(http, uri("/oauth2/jwks"));
        assertThat(jwksResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
        JsonNode jwks = readJson(jwksResponse);
        assertThat(jwks.path("keys").size()).isGreaterThan(0);
        JsonNode publicKey = jwks.path("keys").get(0);
        assertThat(publicKey.path("kid").asText()).isNotBlank();
        assertThat(publicKey.path("kty").asText()).isEqualTo("RSA");
        assertThat(publicKey.path("n").asText()).isNotBlank();
        assertThat(publicKey.path("e").asText()).isNotBlank();
        assertThat(publicKey.has("d")).isFalse();
    }

    @Test
    void pushedAuthorizationRequestCompletesConfidentialAuthorizationCodeFlow() throws Exception {
        HttpClient browser = newHttpClient();
        String state = "state-" + System.nanoTime();

        HttpResponse<String> par = postForm(newHttpClient(), "/oauth2/par", Map.of(
                "response_type", "code",
                "client_id", CONFIDENTIAL_CLIENT_ID,
                "redirect_uri", CONFIDENTIAL_CLIENT_REDIRECT_URI,
                "scope", "message.read",
                "state", state
        ), basicAuth(CONFIDENTIAL_CLIENT_ID, CONFIDENTIAL_CLIENT_SECRET));
        assertThat(par.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        JsonNode parBody = readJson(par);
        String requestUri = parBody.path("request_uri").asText();
        assertThat(requestUri).isNotBlank();
        assertThat(parBody.path("expires_in").asLong()).isGreaterThan(0);

        HttpResponse<String> authorizationStart = get(browser, uri("/oauth2/authorize", Map.of(
                "client_id", CONFIDENTIAL_CLIENT_ID,
                "request_uri", requestUri
        )));
        assertThat(authorizationStart.statusCode()).isEqualTo(HttpStatus.FOUND.value());
        assertThat(location(authorizationStart).getPath()).isEqualTo("/login");

        HttpResponse<String> login = postForm(browser, "/login", Map.of(
                "username", ADMIN_USERNAME,
                "password", ADMIN_PASSWORD
        ), null);
        assertThat(login.statusCode()).isEqualTo(HttpStatus.FOUND.value());

        URI callback = completeConfidentialAuthorizationAfterLogin(
                browser, location(login), CONFIDENTIAL_CLIENT_ID, "message.read");
        assertThat(callback.toString()).startsWith(CONFIDENTIAL_CLIENT_REDIRECT_URI);
        Map<String, String> callbackParams = queryParams(callback);
        assertThat(callbackParams.get("state")).isEqualTo(state);
        assertThat(callbackParams.get("code")).isNotBlank();

        HttpResponse<String> tokenResponse = postForm(newHttpClient(), "/oauth2/token", Map.of(
                "grant_type", "authorization_code",
                "code", callbackParams.get("code"),
                "redirect_uri", CONFIDENTIAL_CLIENT_REDIRECT_URI
        ), basicAuth(CONFIDENTIAL_CLIENT_ID, CONFIDENTIAL_CLIENT_SECRET));
        assertThat(tokenResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
        JsonNode token = readJson(tokenResponse);
        assertThat(token.path("access_token").asText()).isNotBlank();
        assertThat(token.path("refresh_token").asText()).isNotBlank();
    }

    @Test
    void tenantIssuerDiscoveryJwksAndClientCredentialsAreScoped() throws Exception {
        HttpClient http = newHttpClient();
        String tenantIssuer = issuer() + "/t/acme";

        HttpResponse<String> tenantDiscovery = get(http, uri("/t/acme/.well-known/openid-configuration"));
        assertThat(tenantDiscovery.statusCode()).isEqualTo(HttpStatus.OK.value());
        JsonNode tenantMetadata = readJson(tenantDiscovery);
        assertThat(tenantMetadata.path("issuer").asText()).isEqualTo(tenantIssuer);
        assertThat(tenantMetadata.path("token_endpoint").asText()).isEqualTo(tenantIssuer + "/oauth2/token");
        assertThat(tenantMetadata.path("jwks_uri").asText()).isEqualTo(tenantIssuer + "/oauth2/jwks");

        HttpResponse<String> missingTenant = get(http, uri("/t/missing/.well-known/openid-configuration"));
        assertThat(missingTenant.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());

        JsonNode rootJwks = readJson(get(http, uri("/oauth2/jwks")));
        JsonNode tenantJwks = readJson(get(http, uri("/t/acme/oauth2/jwks")));
        assertThat(rootJwks.path("keys").get(0).path("kid").asText()).isNotBlank();
        assertThat(tenantJwks.path("keys").get(0).path("kid").asText()).isNotBlank();
        assertThat(tenantJwks.path("keys").get(0).path("kid").asText())
                .isNotEqualTo(rootJwks.path("keys").get(0).path("kid").asText());

        HttpResponse<String> tenantToken = postForm(newHttpClient(), "/t/acme/oauth2/token", Map.of(
                "grant_type", "client_credentials",
                "scope", "message.read"
        ), basicAuth(TENANT_CLIENT_ID, TENANT_CLIENT_SECRET));
        assertThat(tenantToken.statusCode()).as(tenantToken.body()).isEqualTo(HttpStatus.OK.value());
        JsonNode token = readJson(tenantToken);
        assertThat(token.path("access_token").asText()).isNotBlank();
        assertThat(jwtClaims(token.path("access_token").asText()).path("iss").asText()).isEqualTo(tenantIssuer);

        HttpResponse<String> defaultClientOnTenantIssuer = postForm(newHttpClient(), "/t/acme/oauth2/token", Map.of(
                "grant_type", "client_credentials",
                "scope", "message.read"
        ), basicAuth(CONFIDENTIAL_CLIENT_ID, CONFIDENTIAL_CLIENT_SECRET));
        assertThat(defaultClientOnTenantIssuer.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());

        HttpResponse<String> tenantClientOnDefaultIssuer = postForm(newHttpClient(), "/oauth2/token", Map.of(
                "grant_type", "client_credentials",
                "scope", "message.read"
        ), basicAuth(TENANT_CLIENT_ID, TENANT_CLIENT_SECRET));
        assertThat(tenantClientOnDefaultIssuer.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void dpopProofIssuesSenderConstrainedAccessToken() throws Exception {
        DPoPProof proof = dpopProof("POST", uri("/oauth2/token"));

        HttpResponse<String> response = postForm(newHttpClient(), "/oauth2/token", Map.of(
                "grant_type", "client_credentials",
                "scope", "message.read"
        ), basicAuth(CONFIDENTIAL_CLIENT_ID, CONFIDENTIAL_CLIENT_SECRET), Map.of("DPoP", proof.value()));

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        JsonNode token = readJson(response);
        assertThat(token.path("access_token").asText()).isNotBlank();
        assertThat(token.path("token_type").asText()).isEqualTo("DPoP");
        JsonNode accessTokenClaims = jwtClaims(token.path("access_token").asText());
        assertThat(accessTokenClaims.path("cnf").path("jkt").asText()).isEqualTo(proof.jwkThumbprint());
    }

    @Test
    void dpopProofRejectsWrongHttpMethod() throws Exception {
        DPoPProof proof = dpopProof("GET", uri("/oauth2/token"));

        HttpResponse<String> response = postForm(newHttpClient(), "/oauth2/token", Map.of(
                "grant_type", "client_credentials",
                "scope", "message.read"
        ), basicAuth(CONFIDENTIAL_CLIENT_ID, CONFIDENTIAL_CLIENT_SECRET), Map.of("DPoP", proof.value()));

        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(readJson(response).path("error").asText()).isEqualTo("invalid_dpop_proof");
    }

    @Test
    void dynamicClientRegistrationCreatesReadableClientUsableForClientCredentials() throws Exception {
        JsonNode registrarToken = readJson(postForm(newHttpClient(), "/oauth2/token", Map.of(
                "grant_type", "client_credentials",
                "scope", "client.create"
        ), basicAuth(DCR_REGISTRAR_CLIENT_ID, DCR_REGISTRAR_CLIENT_SECRET)));
        String initialAccessToken = registrarToken.path("access_token").asText();
        assertThat(initialAccessToken).isNotBlank();

        Map<String, Object> registrationRequest = Map.of(
                "client_name", "DCR E2E Client",
                "redirect_uris", List.of("http://localhost:3000/dcr/callback"),
                "grant_types", List.of("authorization_code", "client_credentials"),
                "scope", "message.read"
        );

        HttpResponse<String> unauthorized = postJson(newHttpClient(), "/connect/register", registrationRequest, null);
        assertThat(unauthorized.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());

        HttpResponse<String> registration = postJson(
                newHttpClient(), "/connect/register", registrationRequest, initialAccessToken);
        assertThat(registration.statusCode()).as(registration.body()).isEqualTo(HttpStatus.CREATED.value());
        JsonNode registeredClient = readJson(registration);
        String clientId = registeredClient.path("client_id").asText();
        String clientSecret = registeredClient.path("client_secret").asText();
        String registrationAccessToken = registeredClient.path("registration_access_token").asText();
        String registrationClientUri = registeredClient.path("registration_client_uri").asText();
        assertThat(clientId).isNotBlank();
        assertThat(clientSecret).isNotBlank();
        assertThat(registrationAccessToken).isNotBlank();
        assertThat(registrationClientUri).startsWith(issuer() + "/connect/register");
        assertThat(oAuth2ClientRepository.findOAuth2ClientByClientId(clientId))
                .hasValueSatisfying(client -> {
                    assertThat(client.getTenantId()).isEqualTo("tenant-default");
                    assertThat(client.getRegistrationSource()).isEqualTo("DYNAMIC_REGISTRATION");
                });
        assertThat(auditEventRepository.findByEventType("CLIENT_REGISTRATION_CREATED", PageRequest.of(0, 10)).getContent())
                .anySatisfy(event -> {
                    assertThat(event.getPrincipalName()).isEqualTo(DCR_REGISTRAR_CLIENT_ID);
                    assertThat(event.getResourceId()).isEqualTo(clientId);
                });

        HttpResponse<String> read = getWithBearer(newHttpClient(), URI.create(registrationClientUri), registrationAccessToken);
        assertThat(read.statusCode()).isEqualTo(HttpStatus.OK.value());
        JsonNode readBody = readJson(read);
        assertThat(readBody.path("client_id").asText()).isEqualTo(clientId);
        assertThat(readBody.path("client_name").asText()).isEqualTo("DCR E2E Client");

        HttpResponse<String> clientCredentials = postForm(newHttpClient(), "/oauth2/token", Map.of(
                "grant_type", "client_credentials",
                "scope", "message.read"
        ), basicAuth(clientId, clientSecret));
        assertThat(clientCredentials.statusCode()).isEqualTo(HttpStatus.OK.value());
        JsonNode token = readJson(clientCredentials);
        assertThat(token.path("access_token").asText()).isNotBlank();
        assertThat(token.path("scope").asText()).contains("message.read");
    }

    @Test
    void introspectionReflectsRevocationState() throws Exception {
        HttpClient http = newHttpClient();
        JsonNode token = issueClientCredentialsToken(http);
        String accessToken = token.path("access_token").asText();

        HttpResponse<String> active = postForm(newHttpClient(), "/oauth2/introspect", Map.of(
                "token", accessToken
        ), basicAuth(CONFIDENTIAL_CLIENT_ID, CONFIDENTIAL_CLIENT_SECRET));
        assertThat(active.statusCode()).isEqualTo(HttpStatus.OK.value());
        JsonNode activeBody = readJson(active);
        assertThat(activeBody.path("active").asBoolean()).isTrue();
        assertThat(activeBody.path("client_id").asText()).isEqualTo(CONFIDENTIAL_CLIENT_ID);
        assertThat(activeBody.path("scope").asText()).contains("message.read");

        HttpResponse<String> revoke = postForm(newHttpClient(), "/oauth2/revoke", Map.of(
                "token", accessToken,
                "token_type_hint", "access_token"
        ), basicAuth(CONFIDENTIAL_CLIENT_ID, CONFIDENTIAL_CLIENT_SECRET));
        assertThat(revoke.statusCode()).isEqualTo(HttpStatus.OK.value());

        HttpResponse<String> inactive = postForm(newHttpClient(), "/oauth2/introspect", Map.of(
                "token", accessToken
        ), basicAuth(CONFIDENTIAL_CLIENT_ID, CONFIDENTIAL_CLIENT_SECRET));
        assertThat(inactive.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(readJson(inactive).path("active").asBoolean()).isFalse();
    }

    @Test
    void oidcLogoutAcceptsIdTokenHintAndRegisteredPostLogoutRedirect() throws Exception {
        JsonNode token = loginAndExchangePublicPkceToken(newHttpClient(), "openid profile email");
        String logoutState = "logout-" + System.nanoTime();

        HttpResponse<String> logout = get(newHttpClient(), uri("/connect/logout", Map.of(
                "id_token_hint", token.path("id_token").asText(),
                "client_id", PUBLIC_CLIENT_ID,
                "post_logout_redirect_uri", "http://localhost:8086/",
                "state", logoutState
        )));
        assertThat(logout.statusCode()).isEqualTo(HttpStatus.FOUND.value());
        URI redirect = location(logout);
        assertThat(redirect.toString()).startsWith("http://localhost:8086/");
        assertThat(queryParams(redirect).get("state")).isEqualTo(logoutState);
    }

    private JsonNode loginAndExchangePublicPkceToken(HttpClient browser, String scope) throws Exception {
        String state = "state-" + System.nanoTime();
        String codeVerifier = "codex-code-verifier-0123456789abcdefghijklmnopqrstuvwxyz";

        URI authorizationUri = uri("/oauth2/authorize", Map.of(
                "response_type", "code",
                "client_id", PUBLIC_CLIENT_ID,
                "redirect_uri", PUBLIC_CLIENT_REDIRECT_URI,
                "scope", scope,
                "state", state,
                "code_challenge", codeChallenge(codeVerifier),
                "code_challenge_method", "S256"
        ));

        HttpResponse<String> authorizationStart = get(browser, authorizationUri);
        assertThat(authorizationStart.statusCode()).isEqualTo(HttpStatus.FOUND.value());
        assertThat(location(authorizationStart).getPath()).isEqualTo("/login");

        HttpResponse<String> login = postForm(browser, "/login", Map.of(
                "username", ADMIN_USERNAME,
                "password", ADMIN_PASSWORD
        ), null);
        assertThat(login.statusCode()).isEqualTo(HttpStatus.FOUND.value());

        HttpResponse<String> authorizationComplete = get(browser, resolve(location(login)));
        assertThat(authorizationComplete.statusCode()).isEqualTo(HttpStatus.FOUND.value());

        URI callback = location(authorizationComplete);
        assertThat(callback.toString()).startsWith(PUBLIC_CLIENT_REDIRECT_URI);
        Map<String, String> callbackParams = queryParams(callback);
        assertThat(callbackParams.get("state")).isEqualTo(state);
        assertThat(callbackParams.get("code")).isNotBlank();

        return readJson(postForm(newHttpClient(), "/oauth2/token", Map.of(
                "grant_type", "authorization_code",
                "client_id", PUBLIC_CLIENT_ID,
                "code", callbackParams.get("code"),
                "redirect_uri", PUBLIC_CLIENT_REDIRECT_URI,
                "code_verifier", codeVerifier
        ), null));
    }

    @Test
    void confidentialAuthorizationCodeCanRefreshAccessToken() throws Exception {
        HttpClient browser = newHttpClient();
        String state = "state-" + System.nanoTime();

        URI authorizationUri = uri("/oauth2/authorize", Map.of(
                "response_type", "code",
                "client_id", CONFIDENTIAL_CLIENT_ID,
                "redirect_uri", CONFIDENTIAL_CLIENT_REDIRECT_URI,
                "scope", "message.read",
                "state", state
        ));

        HttpResponse<String> authorizationStart = get(browser, authorizationUri);
        assertThat(authorizationStart.statusCode()).isEqualTo(HttpStatus.FOUND.value());
        assertThat(location(authorizationStart).getPath()).isEqualTo("/login");

        HttpResponse<String> login = postForm(browser, "/login", Map.of(
                "username", ADMIN_USERNAME,
                "password", ADMIN_PASSWORD
        ), null);
        assertThat(login.statusCode()).isEqualTo(HttpStatus.FOUND.value());

        URI callback = completeConfidentialAuthorizationAfterLogin(
                browser, location(login), CONFIDENTIAL_CLIENT_ID, "message.read");
        assertThat(callback.toString()).startsWith(CONFIDENTIAL_CLIENT_REDIRECT_URI);
        Map<String, String> callbackParams = queryParams(callback);
        assertThat(callbackParams.get("state")).isEqualTo(state);
        assertThat(callbackParams.get("code")).isNotBlank();

        HttpResponse<String> tokenResponse = postForm(newHttpClient(), "/oauth2/token", Map.of(
                "grant_type", "authorization_code",
                "code", callbackParams.get("code"),
                "redirect_uri", CONFIDENTIAL_CLIENT_REDIRECT_URI
        ), basicAuth(CONFIDENTIAL_CLIENT_ID, CONFIDENTIAL_CLIENT_SECRET));
        assertThat(tokenResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
        JsonNode token = readJson(tokenResponse);
        assertThat(token.path("access_token").asText()).isNotBlank();
        assertThat(token.path("refresh_token").asText()).isNotBlank();

        HttpResponse<String> refresh = postForm(newHttpClient(), "/oauth2/token", Map.of(
                "grant_type", "refresh_token",
                "refresh_token", token.path("refresh_token").asText()
        ), basicAuth(CONFIDENTIAL_CLIENT_ID, CONFIDENTIAL_CLIENT_SECRET));
        assertThat(refresh.statusCode()).isEqualTo(HttpStatus.OK.value());
        JsonNode refreshedToken = readJson(refresh);
        assertThat(refreshedToken.path("access_token").asText()).isNotBlank();
    }

    @Test
    void deviceAuthorizationFlowCanBeApprovedAndExchangedForToken() throws Exception {
        HttpClient device = newHttpClient();

        HttpResponse<String> authorization = postForm(device, "/oauth2/device_authorization", Map.of(
                "client_id", DEVICE_CLIENT_ID,
                "scope", "message.read"
        ), null);
        assertThat(authorization.statusCode()).isEqualTo(HttpStatus.OK.value());

        JsonNode body = readJson(authorization);
        assertThat(body.path("device_code").asText()).isNotBlank();
        assertThat(body.path("user_code").asText()).isNotBlank();
        assertThat(body.path("verification_uri").asText()).endsWith("/activate");

        HttpResponse<String> pending = postForm(device, "/oauth2/token", Map.of(
                "grant_type", "urn:ietf:params:oauth:grant-type:device_code",
                "client_id", DEVICE_CLIENT_ID,
                "device_code", body.path("device_code").asText()
        ), null);
        assertThat(pending.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(readJson(pending).path("error").asText()).isEqualTo("authorization_pending");

        HttpClient browser = newHttpClient();
        HttpResponse<String> activateStart = get(browser, uri("/activate", Map.of(
                "user_code", body.path("user_code").asText()
        )));
        assertThat(activateStart.statusCode()).isEqualTo(HttpStatus.FOUND.value());
        assertThat(location(activateStart).getPath()).isEqualTo("/login");

        HttpResponse<String> login = postForm(browser, "/login", Map.of(
                "username", ADMIN_USERNAME,
                "password", ADMIN_PASSWORD
        ), null);
        assertThat(login.statusCode()).isEqualTo(HttpStatus.FOUND.value());

        HttpResponse<String> activateRedirect = get(browser, resolve(location(login)));
        assertThat(activateRedirect.statusCode()).isEqualTo(HttpStatus.FOUND.value());
        URI deviceVerificationUri = resolve(location(activateRedirect));
        assertThat(deviceVerificationUri.getPath()).isEqualTo("/oauth2/device_verification");

        HttpResponse<String> consentRedirect = get(browser, deviceVerificationUri);
        assertThat(consentRedirect.statusCode()).isEqualTo(HttpStatus.FOUND.value());
        URI consentUri = resolve(location(consentRedirect));
        assertThat(consentUri.getPath()).isEqualTo("/consent");

        HttpResponse<String> consentPage = get(browser, consentUri);
        assertThat(consentPage.statusCode()).isEqualTo(HttpStatus.OK.value());
        Map<String, String> consentParams = queryParams(consentUri);

        HttpResponse<String> activated = postForm(browser, "/oauth2/device_verification", Map.of(
                "client_id", DEVICE_CLIENT_ID,
                "state", consentParams.get("state"),
                "user_code", body.path("user_code").asText(),
                "scope", "message.read"
        ), null);
        assertThat(activated.statusCode()).isEqualTo(HttpStatus.FOUND.value());
        assertThat(location(activated).getPath()).isEqualTo("/activated");

        HttpResponse<String> token = postForm(device, "/oauth2/token", Map.of(
                "grant_type", "urn:ietf:params:oauth:grant-type:device_code",
                "client_id", DEVICE_CLIENT_ID,
                "device_code", body.path("device_code").asText()
        ), null);
        assertThat(token.statusCode()).isEqualTo(HttpStatus.OK.value());
        JsonNode tokenBody = readJson(token);
        assertThat(tokenBody.path("access_token").asText()).isNotBlank();
        assertThat(tokenBody.path("token_type").asText()).isEqualToIgnoringCase("Bearer");
    }

    private HttpClient newHttpClient() {
        CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        return HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    private JsonNode issueClientCredentialsToken(HttpClient http) throws Exception {
        HttpResponse<String> response = postForm(http, "/oauth2/token", Map.of(
                "grant_type", "client_credentials",
                "scope", "message.read"
        ), basicAuth(CONFIDENTIAL_CLIENT_ID, CONFIDENTIAL_CLIENT_SECRET));
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        return readJson(response);
    }

    private URI completeConfidentialAuthorizationAfterLogin(HttpClient browser, URI authorizationUri,
                                                           String clientId, String scope) throws Exception {
        HttpResponse<String> next = get(browser, resolve(authorizationUri));
        assertThat(next.statusCode()).isEqualTo(HttpStatus.FOUND.value());
        URI nextUri = resolve(location(next));
        if (!"/consent".equals(nextUri.getPath())) {
            return nextUri;
        }

        HttpResponse<String> consentPage = get(browser, nextUri);
        assertThat(consentPage.statusCode()).isEqualTo(HttpStatus.OK.value());
        Map<String, String> consentParams = queryParams(nextUri);

        HttpResponse<String> authorizationComplete = postForm(browser, "/oauth2/authorize", Map.of(
                "client_id", clientId,
                "state", consentParams.get("state"),
                "scope", scope
        ), null);
        assertThat(authorizationComplete.statusCode()).isEqualTo(HttpStatus.FOUND.value());
        return location(authorizationComplete);
    }

    private HttpResponse<String> get(HttpClient http, URI uri) throws Exception {
        return http.send(HttpRequest.newBuilder(uri)
                .GET()
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> getWithBearer(HttpClient http, String path, String accessToken) throws Exception {
        return http.send(HttpRequest.newBuilder(uri(path))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .GET()
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> getWithBearer(HttpClient http, URI uri, String accessToken) throws Exception {
        return http.send(HttpRequest.newBuilder(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .GET()
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postForm(HttpClient http, String path, Map<String, String> form,
                                          String authorizationHeader) throws Exception {
        return postForm(http, path, form, authorizationHeader, Map.of());
    }

    private HttpResponse<String> postForm(HttpClient http, String path, Map<String, String> form,
                                          String authorizationHeader, Map<String, String> headers) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(formEncode(form)));
        if (authorizationHeader != null) {
            builder.header(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }
        headers.forEach(builder::header);
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postJson(HttpClient http, String path, Object body, String bearerToken) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
        if (bearerToken != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
        }
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode readJson(HttpResponse<String> response) throws Exception {
        return objectMapper.readTree(response.body());
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }

    private String issuer() {
        return "http://127.0.0.1:" + port;
    }

    private URI uri(String path, Map<String, String> query) {
        return URI.create(uri(path) + "?" + formEncode(query));
    }

    private URI resolve(URI uri) {
        if (uri.isAbsolute()) {
            return uri;
        }
        return URI.create("http://127.0.0.1:" + port).resolve(uri);
    }

    private URI location(HttpResponse<?> response) {
        return URI.create(response.headers().firstValue(HttpHeaders.LOCATION).orElseThrow());
    }

    private String basicAuth(String username, String password) {
        String credentials = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private String codeChallenge(String verifier) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(verifier.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

    private DPoPProof dpopProof(String method, URI uri) throws Exception {
        ECKey ecKey = new ECKeyGenerator(Curve.P_256)
                .keyID(UUID.randomUUID().toString())
                .generate();
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                .type(new JOSEObjectType("dpop+jwt"))
                .jwk(ecKey.toPublicJWK())
                .build();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(Instant.now()))
                .claim("htm", method)
                .claim("htu", uri.toString())
                .build();
        SignedJWT signedJwt = new SignedJWT(header, claims);
        signedJwt.sign(new ECDSASigner(ecKey));
        return new DPoPProof(signedJwt.serialize(), ecKey.toPublicJWK().computeThumbprint().toString());
    }

    private JsonNode jwtClaims(String jwt) throws Exception {
        String[] parts = jwt.split("\\.");
        assertThat(parts).hasSizeGreaterThanOrEqualTo(2);
        return objectMapper.readTree(Base64.getUrlDecoder().decode(parts[1]));
    }

    private String formEncode(Map<String, String> values) {
        StringBuilder builder = new StringBuilder();
        values.forEach((key, value) -> {
            if (!builder.isEmpty()) {
                builder.append('&');
            }
            builder.append(URLEncoder.encode(key, StandardCharsets.UTF_8));
            builder.append('=');
            builder.append(URLEncoder.encode(value, StandardCharsets.UTF_8));
        });
        return builder.toString();
    }

    private Map<String, String> queryParams(URI uri) {
        Map<String, String> params = new LinkedHashMap<>();
        String query = uri.getRawQuery();
        assertThat(query).isNotBlank();
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            params.put(urlDecode(parts[0]), parts.length > 1 ? urlDecode(parts[1]) : "");
        }
        return params;
    }

    private Set<String> jsonArrayValues(JsonNode json, String field) {
        Set<String> values = new LinkedHashSet<>();
        json.path(field).forEach(value -> values.add(value.asText()));
        return values;
    }

    private String urlDecode(String value) {
        return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private record DPoPProof(String value, String jwkThumbprint) {
    }
}
