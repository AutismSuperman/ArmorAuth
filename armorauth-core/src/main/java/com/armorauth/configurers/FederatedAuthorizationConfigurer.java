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

import com.armorauth.federat.DelegateOAuth2AccessTokenResponseClient;
import com.armorauth.federat.DelegateOAuth2AuthorizationRequestResolver;
import com.armorauth.federat.DelegatingOAuth2UserService;
import com.armorauth.security.FederatedAuthenticationEntryPoint;
import com.armorauth.security.FederatedAuthenticationSuccessHandler;
import org.springframework.context.ApplicationContext;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;

import java.util.function.Consumer;

public class FederatedAuthorizationConfigurer extends AbstractIdentityConfigurer {


    private RequestMatcher requestMatcher;

    private String loginPageUrl = "/login";

    private String authorizationRequestUri = "/oauth2/authorization";

    private ClientRegistrationRepository clientRegistrationRepository;

    private OAuth2UserService<OAuth2UserRequest, OAuth2User> userService;

    private OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient;

    private OAuth2AuthorizationRequestResolver authorizationRequestResolver;

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


    /**
     * Sets the OAuth 2.0 service used for obtaining the user attributes of the
     * End-User from the UserInfo Endpoint.
     *
     * @param userService the OAuth 2.0 service used for obtaining the user attributes
     *                    of the End-User from the UserInfo Endpoint
     * @return the {@link OAuth2LoginConfigurer.UserInfoEndpointConfig} for further configuration
     *//*
    public FederatedAuthorizationConfigurer userService(OAuth2UserService<OAuth2UserRequest, OAuth2User> userService) {
        Assert.notNull(userService, "userService cannot be null");
        this.userService = userService;
        return this;
    }

    *//**
     * Sets the client used for requesting the access token credential from the Token
     * Endpoint.
     *
     * @param accessTokenResponseClient the client used for requesting the access
     *                                  token credential from the Token Endpoint
     * @return the {@link OAuth2LoginConfigurer.TokenEndpointConfig} for further configuration
     *//*
    public FederatedAuthorizationConfigurer accessTokenResponseClient(
            OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient) {
        Assert.notNull(accessTokenResponseClient, "accessTokenResponseClient cannot be null");
        this.accessTokenResponseClient = accessTokenResponseClient;
        return this;
    }

    */

    /**
     * Sets the resolver used for resolving {@link OAuth2AuthorizationRequest}'s.
     *
     * @param authorizationRequestResolver the resolver used for resolving
     *                                     {@link OAuth2AuthorizationRequest}'s
     * @return the {@link OAuth2LoginConfigurer.AuthorizationEndpointConfig} for further configuration
     * @since 5.1
     *//*
    public FederatedAuthorizationConfigurer authorizationRequestResolver(
            OAuth2AuthorizationRequestResolver authorizationRequestResolver) {
        Assert.notNull(authorizationRequestResolver, "authorizationRequestResolver cannot be null");
        this.authorizationRequestResolver = authorizationRequestResolver;
        return this;
    }*/
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


    @SuppressWarnings("unchecked")
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
        // Init Configurer
        OAuth2AuthorizedClientService authorizedClientService =
                applicationContext.getBean(OAuth2AuthorizedClientService.class);

        DelegateOAuth2AccessTokenResponseClient oAuth2AccessTokenResponseClient =
                new DelegateOAuth2AccessTokenResponseClient();

        DelegateOAuth2AuthorizationRequestResolver requestResolver =
                new DelegateOAuth2AuthorizationRequestResolver(
                        clientRegistrationRepository,
                        this.authorizationRequestUri
                );
        DelegatingOAuth2UserService userService = new DelegatingOAuth2UserService();
        ExceptionHandlingConfigurer<?> exceptionHandling = httpSecurity.getConfigurer(ExceptionHandlingConfigurer.class);
        exceptionHandling.authenticationEntryPoint(authenticationEntryPoint);
        httpSecurity.oauth2Login(
                oauth2Login -> oauth2Login
                        .loginPage(loginPageUrl)
                        .successHandler(authenticationSuccessHandler)
                        .clientRegistrationRepository(clientRegistrationRepository)
                        .authorizedClientService(authorizedClientService)
                        .tokenEndpoint(token -> token.accessTokenResponseClient(oAuth2AccessTokenResponseClient))
                        .authorizationEndpoint(authorization ->
                                authorization.authorizationRequestResolver(requestResolver)
                        )
                        .userInfoEndpoint(userInfo -> userInfo.userService(userService))
        );
    }

    @Override
    void configure(HttpSecurity httpSecurity) throws Exception {
        if (oauth2LoginCustomizer != null) {
            httpSecurity.oauth2Login(oauth2LoginCustomizer);
        }
    }


}
