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
package com.armorauth.configurers;

import com.armorauth.security.FederatedAuthenticationEntryPoint;
import com.armorauth.security.FederatedAuthenticationSuccessHandler;
import org.springframework.context.ApplicationContext;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;

import java.util.function.Consumer;

public class FederatedAuthorizationConfigurer extends AbstractIdentityConfigurer {


    private RequestMatcher requestMatcher;

    private String loginPageUrl = "/login";

    private String authorizationRequestUri;

    private ClientRegistrationRepository clientRegistrationRepository;


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

    public FederatedAuthorizationConfigurer clientRegistrationRepository(ClientRegistrationRepository clientRegistrationRepository) {
        Assert.notNull(clientRegistrationRepository, "clientRegistrationRepository cannot be empty");
        this.clientRegistrationRepository = clientRegistrationRepository;
        return this;
    }


    public FederatedAuthorizationConfigurer oauth2Login(Customizer<OAuth2LoginConfigurer<HttpSecurity>> oauth2LoginCustomizer) {
        this.oauth2LoginCustomizer = oauth2LoginCustomizer;
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


    @Override
    void init(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.setSharedObject(ClientRegistrationRepository.class, clientRegistrationRepository);
        ApplicationContext applicationContext = httpSecurity.getSharedObject(ApplicationContext.class);
        // FederatedAuthenticationEntryPoint
        ClientRegistrationRepository clientRegistrationRepository =
                applicationContext.getBean(ClientRegistrationRepository.class);
        FederatedAuthenticationEntryPoint authenticationEntryPoint =
                new FederatedAuthenticationEntryPoint(this.loginPageUrl, clientRegistrationRepository);
        if (this.authorizationRequestUri != null) {
            authenticationEntryPoint.setAuthorizationRequestUri(this.authorizationRequestUri);
        }
        // FederatedAuthenticationSuccessHandler
        FederatedAuthenticationSuccessHandler authenticationSuccessHandler =
                new FederatedAuthenticationSuccessHandler();
        if (this.oauth2UserHandler != null) {
            authenticationSuccessHandler.setOAuth2UserHandler(this.oauth2UserHandler);
        }
        if (this.oidcUserHandler != null) {
            authenticationSuccessHandler.setOidcUserHandler(this.oidcUserHandler);
        }

        httpSecurity.exceptionHandling(exceptionHandling ->
                        exceptionHandling.authenticationEntryPoint(authenticationEntryPoint)
                )
                .oauth2Login(oauth2Login -> oauth2Login
                        .loginPage(this.loginPageUrl)
                        .successHandler(authenticationSuccessHandler)
                );
    }

    @Override
    void configure(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.oauth2Login(oauth2LoginCustomizer);
    }


}
