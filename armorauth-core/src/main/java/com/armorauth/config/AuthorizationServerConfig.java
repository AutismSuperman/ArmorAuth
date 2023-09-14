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
package com.armorauth.config;

import com.armorauth.authentication.DeviceClientAuthenticationProvider;
import com.armorauth.authorization.JpaOAuth2AuthorizationConsentService;
import com.armorauth.authorization.JpaOAuth2AuthorizationService;
import com.armorauth.authorization.client.JpaRegisteredClientRepository;
import com.armorauth.data.repository.AuthorizationConsentRepository;
import com.armorauth.data.repository.AuthorizationRepository;
import com.armorauth.data.repository.OAuth2ClientRepository;
import com.armorauth.jose.Jwks;
import com.armorauth.web.authentication.DeviceClientAuthenticationConverter;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

@Configuration(proxyBeanMethods = false)
public class AuthorizationServerConfig {

    private static final String CUSTOM_CONSENT_PAGE_URI = "/oauth2/consent";

    public static final String CUSTOM_ACTIVATE_PAGE = "/activate";

    private static final String CUSTOM_LOGIN_PAGE = "/login";


    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain authorizationServerSecurityFilterChain(
            HttpSecurity http,
            RegisteredClientRepository registeredClientRepository,
            AuthorizationServerSettings authorizationServerSettings

    ) throws Exception {
        // ExceptionHandlingConfigurer
        http.exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint(CUSTOM_LOGIN_PAGE),
                        new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                ))
                .csrf(AbstractHttpConfigurer::disable);
        // Apply default OAuth2AuthorizationServerConfiguration
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        // Custom  Device Client
        DeviceClientAuthenticationConverter deviceClientAuthenticationConverter =
                new DeviceClientAuthenticationConverter(
                        authorizationServerSettings.getDeviceAuthorizationEndpoint());
        DeviceClientAuthenticationProvider deviceClientAuthenticationProvider =
                new DeviceClientAuthenticationProvider(registeredClientRepository);
        // Custom OAuth2AuthorizationServerConfigurer
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .deviceAuthorizationEndpoint(deviceAuthorizationEndpoint ->
                        deviceAuthorizationEndpoint.verificationUri(CUSTOM_ACTIVATE_PAGE)
                )
                .deviceVerificationEndpoint(deviceVerificationEndpoint ->
                        deviceVerificationEndpoint.consentPage(CUSTOM_CONSENT_PAGE_URI)
                )
                .clientAuthentication(clientAuthentication ->
                        clientAuthentication
                                .authenticationConverter(deviceClientAuthenticationConverter)
                                .authenticationProvider(deviceClientAuthenticationProvider)
                )
                .authorizationEndpoint(authorizationEndpoint -> authorizationEndpoint
                        .consentPage(CUSTOM_CONSENT_PAGE_URI)
                )
                // Enable OpenID Connect 1.0 Provider support
                .oidc(Customizer.withDefaults());
        // OAuth2ResourceServerConfigurer
        http.oauth2ResourceServer(oauth2ResourceServer ->
                oauth2ResourceServer.jwt(Customizer.withDefaults()));
        return http.build();
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder().build();
    }


    //*********************************************Implement core services with JPA*********************************************//

    @Bean
    public RegisteredClientRepository registeredClientRepository(OAuth2ClientRepository oAuth2ClientRepository) {
        return new JpaRegisteredClientRepository(oAuth2ClientRepository);
    }

    @Bean
    public OAuth2AuthorizationService authorizationService(
            AuthorizationRepository authorizationRepository,
            RegisteredClientRepository registeredClientRepository) {
        return new JpaOAuth2AuthorizationService(authorizationRepository, registeredClientRepository);
    }

    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(
            AuthorizationConsentRepository authorizationConsentRepository,
            RegisteredClientRepository registeredClientRepository) {
        return new JpaOAuth2AuthorizationConsentService(authorizationConsentRepository, registeredClientRepository);
    }


    //*********************************************Jose*********************************************//

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        RSAKey rsaKey = Jwks.generateRsa();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }


}
