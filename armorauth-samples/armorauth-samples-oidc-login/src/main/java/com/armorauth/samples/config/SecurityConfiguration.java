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
package com.armorauth.samples.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

    /***
     * 自定义
     *
     * @param http http
     * @return SecurityFilterChain
     * @throws Exception exception
     */
    @Bean
    SecurityFilterChain customSecurityFilterChain(
            HttpSecurity http,
            LogoutSuccessHandler logoutSuccessHandler
    ) throws Exception {
        http.authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2Login -> oauth2Login
                        .loginPage("/oauth2/authorization/autism-client-oidc")
                )
                .logout(logout -> logout.logoutSuccessHandler(logoutSuccessHandler))
        ;
        return http.build();
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

            ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(
                    authenticationToken.getAuthorizedClientRegistrationId()
            );
            String redirectUri = resolvePostLogoutRedirectUri(request, clientRegistration);
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
        return UriComponentsBuilder.fromUriString(resolveAuthorizationServerBaseUri(clientRegistration))
                .path("/connect/logout")
                .build()
                .toUriString();
    }

    private String resolveAuthorizationServerBaseUri(ClientRegistration clientRegistration) {
        URI authorizationUri = URI.create(clientRegistration.getProviderDetails().getAuthorizationUri());
        return UriComponentsBuilder.newInstance()
                .scheme(authorizationUri.getScheme())
                .host(authorizationUri.getHost())
                .port(authorizationUri.getPort())
                .build()
                .toUriString();
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

    private String resolvePostLogoutRedirectUri(HttpServletRequest request, ClientRegistration clientRegistration) {
        if (clientRegistration == null || !StringUtils.hasText(clientRegistration.getRedirectUri())) {
            return resolvePostLogoutRedirectUri(request);
        }
        URI redirectUri = URI.create(clientRegistration.getRedirectUri());
        return UriComponentsBuilder.newInstance()
                .scheme(redirectUri.getScheme())
                .host(redirectUri.getHost())
                .port(redirectUri.getPort())
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
                .requestMatchers("/h2-console/**")
                .requestMatchers("/actuator/health")
                .requestMatchers("/system/monitor");
    }


}
