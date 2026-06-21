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
package com.armorauth.samples.springpkce.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

    private final String registrationId;

    public SecurityConfiguration(@Value("${armorauth.sample.registration-id:spring-pkce}") String registrationId) {
        this.registrationId = registrationId;
    }

    @Bean
    SecurityFilterChain customSecurityFilterChain(
            HttpSecurity http,
            OAuth2AuthorizationRequestResolver pkceResolver,
            LogoutSuccessHandler logoutSuccessHandler
    ) throws Exception {
        http.authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2Login -> oauth2Login
                        .authorizationEndpoint(authorizationEndpoint -> authorizationEndpoint
                                .authorizationRequestResolver(pkceResolver)
                        )
                        .loginPage("/oauth2/authorization/" + registrationId)
                )
                .logout(logout -> logout.logoutSuccessHandler(logoutSuccessHandler));
        return http.build();
    }

    @Bean
    OAuth2AuthorizationRequestResolver pkceResolver(ClientRegistrationRepository clientRegistrationRepository) {
        DefaultOAuth2AuthorizationRequestResolver resolver =
                new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, "/oauth2/authorization");
        resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
        return resolver;
    }

    @Bean
    LogoutSuccessHandler logoutSuccessHandler(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService
    ) {
        return (request, response, authentication) -> {
            if (!(authentication instanceof OAuth2AuthenticationToken authenticationToken)) {
                response.sendRedirect(resolvePostLogoutRedirectUri(request));
                return;
            }

            authorizedClientService.removeAuthorizedClient(
                    authenticationToken.getAuthorizedClientRegistrationId(),
                    authenticationToken.getName()
            );

            String redirectUri = resolvePostLogoutRedirectUri(request);
            ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(
                    authenticationToken.getAuthorizedClientRegistrationId()
            );
            if (clientRegistration == null || !(authenticationToken.getPrincipal() instanceof OidcUser oidcUser)) {
                response.sendRedirect(redirectUri);
                return;
            }

            response.sendRedirect(buildEndSessionEndpoint(clientRegistration, oidcUser, redirectUri));
        };
    }

    private String buildEndSessionEndpoint(
            ClientRegistration clientRegistration,
            OidcUser oidcUser,
            String postLogoutRedirectUri
    ) {
        return UriComponentsBuilder.fromUriString(resolveEndSessionEndpoint(clientRegistration))
                .queryParam("id_token_hint", oidcUser.getIdToken().getTokenValue())
                .queryParam("post_logout_redirect_uri", postLogoutRedirectUri)
                .build()
                .encode()
                .toUriString();
    }

    private String resolveEndSessionEndpoint(ClientRegistration clientRegistration) {
        Object endSessionEndpoint = clientRegistration.getProviderDetails()
                .getConfigurationMetadata()
                .get("end_session_endpoint");
        if (endSessionEndpoint instanceof String endpoint && StringUtils.hasText(endpoint)) {
            return endpoint;
        }
        return clientRegistration.getProviderDetails().getAuthorizationUri()
                .replace("/oauth2/authorize", "/connect/logout");
    }

    private String resolvePostLogoutRedirectUri(HttpServletRequest request) {
        return UriComponentsBuilder.newInstance()
                .scheme(request.getScheme())
                .host(request.getServerName())
                .port(request.getServerPort())
                .path(request.getContextPath())
                .path("/")
                .build()
                .toUriString();
    }

    @Bean
    WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring()
                .requestMatchers("/error")
                .requestMatchers("/favicon.ico")
                .requestMatchers("/static/**")
                .requestMatchers("/resources/**")
                .requestMatchers("/webjars/**")
                .requestMatchers("/actuator/health");
    }
}
