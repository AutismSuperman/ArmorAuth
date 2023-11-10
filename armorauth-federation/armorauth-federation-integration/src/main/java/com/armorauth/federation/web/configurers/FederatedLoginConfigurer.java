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
package com.armorauth.federation.web.configurers;

import com.armorauth.federation.authentication.BindUserCheckProvider;
import com.armorauth.federation.authentication.BindUserCheckService;
import com.armorauth.federation.authentication.DefaultBindUserCheckService;
import com.armorauth.federation.web.FederatedAuthorizationRequestRedirectFilter;
import com.armorauth.federation.web.FederatedLoginAuthenticationFilter;
import com.armorauth.federation.web.HttpSecurityFilterOrderRegistrationUtils;
import com.armorauth.federation.web.OAuth2ClientConfigurerUtils;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractAuthenticationFilterConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationProvider;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.oidc.authentication.OidcAuthorizationCodeAuthenticationProvider;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.web.*;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FederatedLoginConfigurer
        extends AbstractAuthenticationFilterConfigurer<HttpSecurity, FederatedLoginConfigurer, FederatedLoginAuthenticationFilter> {


    private String loginPage;
    private final AuthorizationEndpointConfig authorizationEndpointConfig = new AuthorizationEndpointConfig();
    private final TokenEndpointConfig tokenEndpointConfig = new TokenEndpointConfig();
    private final RedirectionEndpointConfig redirectionEndpointConfig = new RedirectionEndpointConfig();
    private final UserInfoEndpointConfig userInfoEndpointConfig = new UserInfoEndpointConfig();
    private RequestMatcher endpointsMatcher;
    private String loginProcessingUrl = FederatedLoginAuthenticationFilter.DEFAULT_FILTER_PROCESSES_URI;


    /**
     * Sets the repository of client registrations.
     *
     * @param clientRegistrationRepository the repository of client registrations
     * @return the {@link OAuth2LoginConfigurer} for further configuration
     */
    public FederatedLoginConfigurer clientRegistrationRepository(
            ClientRegistrationRepository clientRegistrationRepository) {
        Assert.notNull(clientRegistrationRepository, "clientRegistrationRepository cannot be null");
        this.getBuilder().setSharedObject(ClientRegistrationRepository.class, clientRegistrationRepository);
        return this;
    }

    /**
     * Sets the repository for authorized client(s).
     *
     * @param authorizedClientRepository the authorized client repository
     * @return the {@link OAuth2LoginConfigurer} for further configuration
     * @since 5.1
     */
    public FederatedLoginConfigurer authorizedClientRepository(
            OAuth2AuthorizedClientRepository authorizedClientRepository) {
        Assert.notNull(authorizedClientRepository, "authorizedClientRepository cannot be null");
        this.getBuilder().setSharedObject(OAuth2AuthorizedClientRepository.class, authorizedClientRepository);
        return this;
    }

    /**
     * Sets the service for authorized client(s).
     *
     * @param authorizedClientService the authorized client service
     * @return the {@link OAuth2LoginConfigurer} for further configuration
     */
    public FederatedLoginConfigurer authorizedClientService(OAuth2AuthorizedClientService authorizedClientService) {
        Assert.notNull(authorizedClientService, "authorizedClientService cannot be null");
        this.authorizedClientRepository(
                new AuthenticatedPrincipalOAuth2AuthorizedClientRepository(authorizedClientService));
        return this;
    }

    @Override
    public FederatedLoginConfigurer loginPage(String loginPage) {
        Assert.hasText(loginPage, "loginPage cannot be empty");
        this.loginPage = loginPage;
        return this;
    }


    @Override
    public FederatedLoginConfigurer loginProcessingUrl(String loginProcessingUrl) {
        Assert.hasText(loginProcessingUrl, "loginProcessingUrl cannot be empty");
        this.loginProcessingUrl = loginProcessingUrl;
        return this;
    }


    /**
     * Configures the Authorization Server's Authorization Endpoint.
     *
     * @param authorizationEndpointCustomizer the {@link Customizer} to provide more
     *                                        options for the {@link OAuth2LoginConfigurer.AuthorizationEndpointConfig}
     * @return the {@link OAuth2LoginConfigurer} for further customizations
     */
    public FederatedLoginConfigurer authorizationEndpoint(
            Customizer<AuthorizationEndpointConfig> authorizationEndpointCustomizer) {
        authorizationEndpointCustomizer.customize(this.authorizationEndpointConfig);
        return this;
    }

    /**
     * Configures the Authorization Server's Token Endpoint.
     *
     * @param tokenEndpointCustomizer the {@link Customizer} to provide more options for
     *                                the {@link OAuth2LoginConfigurer.TokenEndpointConfig}
     * @return the {@link OAuth2LoginConfigurer} for further customizations
     */
    public FederatedLoginConfigurer tokenEndpoint(Customizer<TokenEndpointConfig> tokenEndpointCustomizer) {
        tokenEndpointCustomizer.customize(this.tokenEndpointConfig);
        return this;
    }

    /**
     * Configures the Client's Redirection Endpoint.
     *
     * @param redirectionEndpointCustomizer the {@link Customizer} to provide more options
     *                                      for the {@link OAuth2LoginConfigurer.RedirectionEndpointConfig}
     * @return the {@link OAuth2LoginConfigurer} for further customizations
     */
    public FederatedLoginConfigurer redirectionEndpoint(
            Customizer<RedirectionEndpointConfig> redirectionEndpointCustomizer) {
        redirectionEndpointCustomizer.customize(this.redirectionEndpointConfig);
        return this;
    }

    /**
     * Configures the Authorization Server's UserInfo Endpoint.
     *
     * @param userInfoEndpointCustomizer the {@link Customizer} to provide more options
     *                                   for the {@link OAuth2LoginConfigurer.UserInfoEndpointConfig}
     * @return the {@link OAuth2LoginConfigurer} for further customizations
     */
    public FederatedLoginConfigurer userInfoEndpoint(Customizer<UserInfoEndpointConfig> userInfoEndpointCustomizer) {
        userInfoEndpointCustomizer.customize(this.userInfoEndpointConfig);
        return this;
    }


    public RequestMatcher getEndpointsMatcher() {
        // Return a deferred RequestMatcher
        // since endpointsMatcher is constructed in init(HttpSecurity).
        return (request) -> this.endpointsMatcher.matches(request);
    }

    @Override
    public void init(HttpSecurity http) throws Exception {
        List<RequestMatcher> requestMatchers = new ArrayList<>();
        FederatedLoginAuthenticationFilter authenticationFilter = new FederatedLoginAuthenticationFilter(
                OAuth2ClientConfigurerUtils.getClientRegistrationRepository(this.getBuilder()),
                OAuth2ClientConfigurerUtils.getAuthorizedClientRepository(this.getBuilder()), this.loginProcessingUrl);
        authenticationFilter.setSecurityContextHolderStrategy(getSecurityContextHolderStrategy());
        this.setAuthenticationFilter(authenticationFilter);
        super.loginProcessingUrl(this.loginProcessingUrl);
        // add login processing url to request matchers
        requestMatchers.add(new AntPathRequestMatcher(this.loginProcessingUrl));
        if (this.userInfoEndpointConfig.bindUserPage != null) {
            // Set custom bind user page
            authenticationFilter.setBindUserPage(this.userInfoEndpointConfig.bindUserPage);
            // add login processing url to request matchers
            requestMatchers.add(new AntPathRequestMatcher(this.loginProcessingUrl));
        }
        if (this.loginPage != null) {
            // Set custom login page
            super.loginPage(this.loginPage);
            super.init(http);
        }
        OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient = this.tokenEndpointConfig.accessTokenResponseClient;
        if (accessTokenResponseClient == null) {
            accessTokenResponseClient = new DefaultAuthorizationCodeTokenResponseClient();
        }
        OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService = getOAuth2UserService();
        //可能要替换掉 OAuth2LoginAuthenticationProvider
        OAuth2LoginAuthenticationProvider oauth2LoginAuthenticationProvider = new OAuth2LoginAuthenticationProvider(
                accessTokenResponseClient, oauth2UserService);
        GrantedAuthoritiesMapper userAuthoritiesMapper = this.getGrantedAuthoritiesMapper();
        if (userAuthoritiesMapper != null) {
            oauth2LoginAuthenticationProvider.setAuthoritiesMapper(userAuthoritiesMapper);
        }
        http.authenticationProvider(this.postProcess(oauth2LoginAuthenticationProvider));
        BindUserCheckService bindUserCheckService = getBindUserCheckService();
        BindUserCheckProvider bindUserCheckProvider =
                new BindUserCheckProvider(bindUserCheckService);
        http.authenticationProvider(this.postProcess(bindUserCheckProvider));
        // check OIDC enable
        boolean oidcAuthenticationProviderEnabled = ClassUtils
                .isPresent("org.springframework.security.oauth2.jwt.JwtDecoder", this.getClass().getClassLoader());
        if (oidcAuthenticationProviderEnabled) {
            OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService = getOidcUserService();
            OidcAuthorizationCodeAuthenticationProvider oidcAuthorizationCodeAuthenticationProvider = new OidcAuthorizationCodeAuthenticationProvider(
                    accessTokenResponseClient, oidcUserService);
            JwtDecoderFactory<ClientRegistration> jwtDecoderFactory = this.getJwtDecoderFactoryBean();
            if (jwtDecoderFactory != null) {
                oidcAuthorizationCodeAuthenticationProvider.setJwtDecoderFactory(jwtDecoderFactory);
            }
            if (userAuthoritiesMapper != null) {
                oidcAuthorizationCodeAuthenticationProvider.setAuthoritiesMapper(userAuthoritiesMapper);
            }
            http.authenticationProvider(this.postProcess(oidcAuthorizationCodeAuthenticationProvider));
        } else {
            http.authenticationProvider(new OidcAuthenticationRequestChecker());
        }

        String authorizationRequestBaseUri = this.authorizationEndpointConfig.authorizationRequestBaseUri;
        if (authorizationRequestBaseUri == null) {
            authorizationRequestBaseUri = FederatedAuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI;
        }
        // add login processing url to request matchers
        requestMatchers.add(new AntPathRequestMatcher(authorizationRequestBaseUri + "/**"));
        this.endpointsMatcher = new OrRequestMatcher(requestMatchers);
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        // Configure OAuth2AuthorizationRequestRedirectFilter
        FederatedAuthorizationRequestRedirectFilter authorizationRequestFilter;
        if (this.authorizationEndpointConfig.authorizationRequestResolver != null) {
            authorizationRequestFilter = new FederatedAuthorizationRequestRedirectFilter(
                    this.authorizationEndpointConfig.authorizationRequestResolver);
        } else {
            String authorizationRequestBaseUri = this.authorizationEndpointConfig.authorizationRequestBaseUri;
            if (authorizationRequestBaseUri == null) {
                authorizationRequestBaseUri = FederatedAuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI;
            }
            authorizationRequestFilter = new FederatedAuthorizationRequestRedirectFilter(
                    OAuth2ClientConfigurerUtils.getClientRegistrationRepository(this.getBuilder()),
                    authorizationRequestBaseUri);
        }
        if (this.authorizationEndpointConfig.authorizationRequestRepository != null) {
            authorizationRequestFilter
                    .setAuthorizationRequestRepository(this.authorizationEndpointConfig.authorizationRequestRepository);
        }
        if (this.authorizationEndpointConfig.authorizationRedirectStrategy != null) {
            authorizationRequestFilter
                    .setAuthorizationRedirectStrategy(this.authorizationEndpointConfig.authorizationRedirectStrategy);
        }
        RequestCache requestCache = http.getSharedObject(RequestCache.class);
        if (requestCache != null) {
            authorizationRequestFilter.setRequestCache(requestCache);
        }
        HttpSecurityFilterOrderRegistrationUtils.putIntendedFilterBefore(http,
                authorizationRequestFilter, OAuth2AuthorizationRequestRedirectFilter.class);
        http.addFilter(this.postProcess(authorizationRequestFilter));
        FederatedLoginAuthenticationFilter authenticationFilter = this.getAuthenticationFilter();
        if (this.redirectionEndpointConfig.authorizationResponseBaseUri != null) {
            authenticationFilter.setFilterProcessesUrl(this.redirectionEndpointConfig.authorizationResponseBaseUri);
        }
        if (this.authorizationEndpointConfig.authorizationRequestRepository != null) {
            authenticationFilter
                    .setAuthorizationRequestRepository(this.authorizationEndpointConfig.authorizationRequestRepository);
        }
        HttpSecurityFilterOrderRegistrationUtils.putIntendedFilterBefore(http, authenticationFilter, OAuth2LoginAuthenticationFilter.class);
        super.configure(http);
    }


    private OAuth2UserService<OAuth2UserRequest, OAuth2User> getOAuth2UserService() {
        if (this.userInfoEndpointConfig.userService != null) {
            return this.userInfoEndpointConfig.userService;
        }
        ResolvableType type = ResolvableType.forClassWithGenerics(OAuth2UserService.class, OAuth2UserRequest.class,
                OAuth2User.class);
        OAuth2UserService<OAuth2UserRequest, OAuth2User> bean = getBeanOrNull(type);
        return (bean != null) ? bean : new DefaultOAuth2UserService();
    }

    private BindUserCheckService getBindUserCheckService() {
        if (this.userInfoEndpointConfig.bindUserCheckService != null) {
            return this.userInfoEndpointConfig.bindUserCheckService;
        }
        ResolvableType type = ResolvableType.forClass(BindUserCheckService.class);
        BindUserCheckService bean = getBeanOrNull(type);
        return (bean != null) ? bean : new DefaultBindUserCheckService();
    }

    private OAuth2UserService<OidcUserRequest, OidcUser> getOidcUserService() {
        if (this.userInfoEndpointConfig.oidcUserService != null) {
            return this.userInfoEndpointConfig.oidcUserService;
        }
        ResolvableType type = ResolvableType.forClassWithGenerics(OAuth2UserService.class, OidcUserRequest.class,
                OidcUser.class);
        OAuth2UserService<OidcUserRequest, OidcUser> bean = getBeanOrNull(type);
        return (bean != null) ? bean : new OidcUserService();
    }

    @SuppressWarnings("unchecked")
    private JwtDecoderFactory<ClientRegistration> getJwtDecoderFactoryBean() {
        ResolvableType type = ResolvableType.forClassWithGenerics(JwtDecoderFactory.class, ClientRegistration.class);
        String[] names = this.getBuilder().getSharedObject(ApplicationContext.class).getBeanNamesForType(type);
        if (names.length > 1) {
            throw new NoUniqueBeanDefinitionException(type, names);
        }
        if (names.length == 1) {
            return (JwtDecoderFactory<ClientRegistration>) this.getBuilder().getSharedObject(ApplicationContext.class)
                    .getBean(names[0]);
        }
        return null;
    }

    private GrantedAuthoritiesMapper getGrantedAuthoritiesMapper() {
        GrantedAuthoritiesMapper grantedAuthoritiesMapper = this.getBuilder()
                .getSharedObject(GrantedAuthoritiesMapper.class);
        if (grantedAuthoritiesMapper == null) {
            grantedAuthoritiesMapper = this.getGrantedAuthoritiesMapperBean();
            if (grantedAuthoritiesMapper != null) {
                this.getBuilder().setSharedObject(GrantedAuthoritiesMapper.class, grantedAuthoritiesMapper);
            }
        }
        return grantedAuthoritiesMapper;
    }

    private GrantedAuthoritiesMapper getGrantedAuthoritiesMapperBean() {
        Map<String, GrantedAuthoritiesMapper> grantedAuthoritiesMapperMap = BeanFactoryUtils
                .beansOfTypeIncludingAncestors(this.getBuilder().getSharedObject(ApplicationContext.class),
                        GrantedAuthoritiesMapper.class);
        return (!grantedAuthoritiesMapperMap.isEmpty() ? grantedAuthoritiesMapperMap.values().iterator().next() : null);
    }


    @SuppressWarnings("unchecked")
    private <T> T getBeanOrNull(ResolvableType type) {
        ApplicationContext context = getBuilder().getSharedObject(ApplicationContext.class);
        if (context != null) {
            String[] names = context.getBeanNamesForType(type);
            if (names.length == 1) {
                return (T) context.getBean(names[0]);
            }
        }
        return null;
    }

    @Override
    protected RequestMatcher createLoginProcessingUrlMatcher(String loginProcessingUrl) {
        return new AntPathRequestMatcher(loginProcessingUrl);
    }

    private static class OidcAuthenticationRequestChecker implements AuthenticationProvider {

        @Override
        public Authentication authenticate(Authentication authentication) throws AuthenticationException {
            OAuth2LoginAuthenticationToken authorizationCodeAuthentication = (OAuth2LoginAuthenticationToken) authentication;
            OAuth2AuthorizationRequest authorizationRequest = authorizationCodeAuthentication.getAuthorizationExchange()
                    .getAuthorizationRequest();
            if (authorizationRequest.getScopes().contains(OidcScopes.OPENID)) {
                // Section 3.1.2.1 Authentication Request -
                // https://openid.net/specs/openid-connect-core-1_0.html#AuthRequest scope
                // REQUIRED. OpenID Connect requests MUST contain the "openid" scope
                // value.
                OAuth2Error oauth2Error = new OAuth2Error("oidc_provider_not_configured",
                        "An OpenID Connect Authentication Provider has not been configured. "
                                + "Check to ensure you include the dependency 'spring-security-oauth2-jose'.",
                        null);
                throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
            }
            return null;
        }

        @Override
        public boolean supports(Class<?> authentication) {
            return OAuth2LoginAuthenticationToken.class.isAssignableFrom(authentication);
        }

    }

    public final class AuthorizationEndpointConfig {

        private String authorizationRequestBaseUri;

        private OAuth2AuthorizationRequestResolver authorizationRequestResolver;

        private AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository;

        private RedirectStrategy authorizationRedirectStrategy;

        private AuthorizationEndpointConfig() {
        }

        /**
         * Sets the base {@code URI} used for authorization requests.
         *
         * @param authorizationRequestBaseUri the base {@code URI} used for authorization
         *                                    requests
         * @return the {@link OAuth2LoginConfigurer.AuthorizationEndpointConfig} for further configuration
         */
        public AuthorizationEndpointConfig baseUri(String authorizationRequestBaseUri) {
            Assert.hasText(authorizationRequestBaseUri, "authorizationRequestBaseUri cannot be empty");
            this.authorizationRequestBaseUri = authorizationRequestBaseUri;
            return this;
        }

        /**
         * Sets the resolver used for resolving {@link OAuth2AuthorizationRequest}'s.
         *
         * @param authorizationRequestResolver the resolver used for resolving
         *                                     {@link OAuth2AuthorizationRequest}'s
         * @return the {@link OAuth2LoginConfigurer.AuthorizationEndpointConfig} for further configuration
         * @since 5.1
         */
        public AuthorizationEndpointConfig authorizationRequestResolver(
                OAuth2AuthorizationRequestResolver authorizationRequestResolver) {
            Assert.notNull(authorizationRequestResolver, "authorizationRequestResolver cannot be null");
            this.authorizationRequestResolver = authorizationRequestResolver;
            return this;
        }

        /**
         * Sets the repository used for storing {@link OAuth2AuthorizationRequest}'s.
         *
         * @param authorizationRequestRepository the repository used for storing
         *                                       {@link OAuth2AuthorizationRequest}'s
         * @return the {@link OAuth2LoginConfigurer.AuthorizationEndpointConfig} for further configuration
         */
        public AuthorizationEndpointConfig authorizationRequestRepository(
                AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository) {
            Assert.notNull(authorizationRequestRepository, "authorizationRequestRepository cannot be null");
            this.authorizationRequestRepository = authorizationRequestRepository;
            return this;
        }

        /**
         * Sets the redirect strategy for Authorization Endpoint redirect URI.
         *
         * @param authorizationRedirectStrategy the redirect strategy
         * @return the {@link OAuth2LoginConfigurer.AuthorizationEndpointConfig} for further configuration
         */
        public AuthorizationEndpointConfig authorizationRedirectStrategy(
                RedirectStrategy authorizationRedirectStrategy) {
            this.authorizationRedirectStrategy = authorizationRedirectStrategy;
            return this;
        }


    }

    public final class TokenEndpointConfig {

        private OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient;

        private TokenEndpointConfig() {
        }

        /**
         * Sets the client used for requesting the access token credential from the Token
         * Endpoint.
         *
         * @param accessTokenResponseClient the client used for requesting the access
         *                                  token credential from the Token Endpoint
         * @return the {@link OAuth2LoginConfigurer.TokenEndpointConfig} for further configuration
         */
        public TokenEndpointConfig accessTokenResponseClient(
                OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient) {
            Assert.notNull(accessTokenResponseClient, "accessTokenResponseClient cannot be null");
            this.accessTokenResponseClient = accessTokenResponseClient;
            return this;
        }

    }

    public final class RedirectionEndpointConfig {

        private String authorizationResponseBaseUri;

        private RedirectionEndpointConfig() {
        }

        /**
         * Sets the {@code URI} where the authorization response will be processed.
         *
         * @param authorizationResponseBaseUri the {@code URI} where the authorization
         *                                     response will be processed
         * @return the {@link OAuth2LoginConfigurer.RedirectionEndpointConfig} for further configuration
         */
        public RedirectionEndpointConfig baseUri(String authorizationResponseBaseUri) {
            Assert.hasText(authorizationResponseBaseUri, "authorizationResponseBaseUri cannot be empty");
            this.authorizationResponseBaseUri = authorizationResponseBaseUri;
            return this;
        }


    }

    public final class UserInfoEndpointConfig {

        private OAuth2UserService<OAuth2UserRequest, OAuth2User> userService;

        private OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService;

        private String bindUserPage;

        private BindUserCheckService bindUserCheckService;


        private UserInfoEndpointConfig() {
        }

        /**
         * Sets the OAuth 2.0 service used for obtaining the user attributes of the
         * End-User from the UserInfo Endpoint.
         *
         * @param userService the OAuth 2.0 service used for obtaining the user attributes
         *                    of the End-User from the UserInfo Endpoint
         * @return the {@link OAuth2LoginConfigurer.UserInfoEndpointConfig} for further configuration
         */
        public UserInfoEndpointConfig userService(OAuth2UserService<OAuth2UserRequest, OAuth2User> userService) {
            Assert.notNull(userService, "userService cannot be null");
            this.userService = userService;
            return this;
        }

        /**
         * Sets the OpenID Connect 1.0 service used for obtaining the user attributes of
         * the End-User from the UserInfo Endpoint.
         *
         * @param oidcUserService the OpenID Connect 1.0 service used for obtaining the
         *                        user attributes of the End-User from the UserInfo Endpoint
         * @return the {@link OAuth2LoginConfigurer.UserInfoEndpointConfig} for further configuration
         */
        public UserInfoEndpointConfig oidcUserService(OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService) {
            Assert.notNull(oidcUserService, "oidcUserService cannot be null");
            this.oidcUserService = oidcUserService;
            return this;
        }

        /**
         * Sets the {@link GrantedAuthoritiesMapper} used for mapping
         * {@link OAuth2User#getAuthorities()}.
         *
         * @param userAuthoritiesMapper the {@link GrantedAuthoritiesMapper} used for
         *                              mapping the user's authorities
         * @return the {@link OAuth2LoginConfigurer.UserInfoEndpointConfig} for further configuration
         */
        public UserInfoEndpointConfig userAuthoritiesMapper(GrantedAuthoritiesMapper userAuthoritiesMapper) {
            Assert.notNull(userAuthoritiesMapper, "userAuthoritiesMapper cannot be null");
            FederatedLoginConfigurer.this.getBuilder().setSharedObject(GrantedAuthoritiesMapper.class,
                    userAuthoritiesMapper);
            return this;
        }

        public UserInfoEndpointConfig bindUserCheckService(BindUserCheckService bindUserCheckService) {
            Assert.notNull(bindUserCheckService, "bindUserCheckService cannot be null");
            this.bindUserCheckService = bindUserCheckService;
            return this;
        }

        public UserInfoEndpointConfig bindUserPage(String bindUserPage) {
            Assert.hasText(bindUserPage, "bindUserPage cannot be null");
            this.bindUserPage = bindUserPage;
            return this;
        }

    }
}
