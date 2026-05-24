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
package com.armorauth.federation.configurer;

import com.armorauth.federation.DelegateAccessTokenResponseClient;
import com.armorauth.federation.DelegatingOAuth2AuthorizationRequestResolver;
import com.armorauth.federation.DelegatingOAuth2UserService;
import com.armorauth.federation.FederatedLoginMode;
import com.armorauth.federation.FederatedSessionContextRepository;
import com.armorauth.federation.config.FederationProperties;
import com.armorauth.federation.provider.FederatedOAuth2ProviderRegistry;
import com.armorauth.federation.security.FederatedAuthenticationEntryPoint;
import com.armorauth.federation.security.FederatedAuthenticationSuccessHandler;
import com.armorauth.federation.security.FederatedSamlAuthenticationSuccessHandler;
import org.springframework.context.ApplicationContext;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.util.Assert;

import java.util.function.Consumer;

public class FederatedAuthorizationConfigurer extends AbstractIdentityConfigurer {

    private String loginPageUrl = "/login";

    private String authorizationRequestUri = "/oauth2/authorization";

    private Consumer<OAuth2User> oauth2UserHandler;

    private Consumer<OidcUser> oidcUserHandler;

    private Customizer<OAuth2LoginConfigurer<HttpSecurity>> oauth2LoginCustomizer;

    FederatedAuthorizationConfigurer(ObjectPostProcessor<Object> objectPostProcessor) {
        super(objectPostProcessor);
    }

    public FederatedAuthorizationConfigurer loginPageUrl(String loginPageUrl) {
        Assert.hasText(loginPageUrl, "loginPageUrl cannot be empty");
        this.loginPageUrl = loginPageUrl;
        return this;
    }

    public FederatedAuthorizationConfigurer authorizationRequestUri(String authorizationRequestUri) {
        Assert.hasText(authorizationRequestUri, "authorizationRequestUri cannot be empty");
        this.authorizationRequestUri = authorizationRequestUri;
        return this;
    }

    public FederatedAuthorizationConfigurer oauth2UserHandler(Consumer<OAuth2User> oauth2UserHandler) {
        Assert.notNull(oauth2UserHandler, "oauth2UserHandler cannot be null");
        this.oauth2UserHandler = oauth2UserHandler;
        return this;
    }

    public FederatedAuthorizationConfigurer oidcUserHandler(Consumer<OidcUser> oidcUserHandler) {
        Assert.notNull(oidcUserHandler, "oidcUserHandler cannot be null");
        this.oidcUserHandler = oidcUserHandler;
        return this;
    }

    public FederatedAuthorizationConfigurer oauth2Login(Customizer<OAuth2LoginConfigurer<HttpSecurity>> oauth2LoginCustomizer) {
        this.oauth2LoginCustomizer = oauth2LoginCustomizer;
        return this;
    }

    @Override
    void init(HttpSecurity httpSecurity) {
        ApplicationContext applicationContext = httpSecurity.getSharedObject(ApplicationContext.class);
        ClientRegistrationRepository clientRegistrationRepository =
                applicationContext.getBean(ClientRegistrationRepository.class);
        FederatedOAuth2ProviderRegistry providerRegistry =
                applicationContext.getBean(FederatedOAuth2ProviderRegistry.class);
        FederatedAuthenticationEntryPoint authenticationEntryPoint =
                new FederatedAuthenticationEntryPoint(this.loginPageUrl, clientRegistrationRepository);
        authenticationEntryPoint.setAuthorizationRequestUri(this.authorizationRequestUri);

        FederatedAuthenticationSuccessHandler authenticationSuccessHandler =
                applicationContext.getBean(FederatedAuthenticationSuccessHandler.class);
        if (this.oauth2UserHandler != null) {
            authenticationSuccessHandler.setOAuth2UserHandler(this.oauth2UserHandler);
        }
        if (this.oidcUserHandler != null) {
            authenticationSuccessHandler.setOidcUserHandler(this.oidcUserHandler);
        }

        OAuth2AuthorizedClientService authorizedClientService =
                applicationContext.getBean(OAuth2AuthorizedClientService.class);
        FederationProperties federationProperties = applicationContext.getBean(FederationProperties.class);
        FederatedLoginMode defaultLoginMode =
                FederatedLoginMode.resolveConfiguredDefault(federationProperties.getDefaultLoginMode());

        DelegatingOAuth2AuthorizationRequestResolver requestResolver =
                new DelegatingOAuth2AuthorizationRequestResolver(
                        clientRegistrationRepository,
                        this.authorizationRequestUri,
                        applicationContext.getBean(FederatedSessionContextRepository.class),
                        defaultLoginMode,
                        providerRegistry
                );
        DelegatingOAuth2UserService userService = new DelegatingOAuth2UserService(providerRegistry);
        DelegateAccessTokenResponseClient accessTokenResponseClient =
                new DelegateAccessTokenResponseClient(providerRegistry);

        ExceptionHandlingConfigurer<?> exceptionHandling = httpSecurity.getConfigurer(ExceptionHandlingConfigurer.class);
        exceptionHandling.authenticationEntryPoint(authenticationEntryPoint);
        try {
            httpSecurity.oauth2Login(oauth2Login -> oauth2Login
                    .loginPage(this.loginPageUrl)
                    .successHandler(authenticationSuccessHandler)
                    .clientRegistrationRepository(clientRegistrationRepository)
                    .authorizedClientService(authorizedClientService)
                    .tokenEndpoint(token -> token.accessTokenResponseClient(accessTokenResponseClient))
                    .authorizationEndpoint(authorization ->
                            authorization.authorizationRequestResolver(requestResolver))
                    .userInfoEndpoint(userInfo -> userInfo.userService(userService)));
            RelyingPartyRegistrationRepository relyingPartyRegistrationRepository =
                    applicationContext.getBean(RelyingPartyRegistrationRepository.class);
            FederatedSamlAuthenticationSuccessHandler samlAuthenticationSuccessHandler =
                    applicationContext.getBean(FederatedSamlAuthenticationSuccessHandler.class);
            httpSecurity.saml2Login(saml2Login -> saml2Login
                    .loginPage(this.loginPageUrl)
                    .relyingPartyRegistrationRepository(relyingPartyRegistrationRepository)
                    .successHandler(samlAuthenticationSuccessHandler));
            httpSecurity.saml2Metadata(metadata ->
                    metadata.metadataUrl("/saml2/service-provider-metadata/{registrationId}"));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to configure federated login", ex);
        }
    }

    @Override
    void configure(HttpSecurity httpSecurity) {
        if (this.oauth2LoginCustomizer == null) {
            return;
        }
        try {
            httpSecurity.oauth2Login(this.oauth2LoginCustomizer);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to apply oauth2Login customizer", ex);
        }
    }

}
