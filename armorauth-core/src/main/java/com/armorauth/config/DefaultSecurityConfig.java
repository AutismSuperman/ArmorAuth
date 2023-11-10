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


import com.armorauth.configurers.web.OAuth2UserLoginFilterSecurityConfigurer;
import com.armorauth.constant.ConfigBeanNameConstants;
import com.armorauth.data.repository.UserInfoRepository;
import com.armorauth.details.DelegateUserDetailsService;
import com.armorauth.federation.*;
import com.armorauth.federation.endpoint.OAuth2AccessTokenRestTemplateConverter;
import com.armorauth.federation.endpoint.OAuth2AuthorizationCodeGrantRequestConverter;
import com.armorauth.federation.gitee.user.GiteeOAuth2UserService;
import com.armorauth.federation.qq.endpoint.QqAccessTokenRestTemplateConverter;
import com.armorauth.federation.qq.endpoint.QqAuthorizationCodeGrantRequestConverter;
import com.armorauth.federation.web.FederatedAuthenticationEntryPoint;
import com.armorauth.federation.web.configurers.FederatedLoginConfigurer;
import com.armorauth.federation.web.converter.OAuth2AuthorizationRequestConverter;
import com.armorauth.federation.wechat.endpoint.WechatAccessTokenRestTemplateConverter;
import com.armorauth.federation.wechat.endpoint.WechatAuthorizationCodeGrantRequestConverter;
import com.armorauth.federation.wechat.web.converter.WechatAuthorizationRequestConverter;
import com.armorauth.security.FailureAuthenticationEntryPoint;
import com.armorauth.security.FederatedAuthenticationSuccessHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.oauth2.client.JdbcOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationEntryPointFailureHandler;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.armorauth.federation.ExtendedOAuth2ClientProvider.*;

@EnableWebSecurity(debug = true)
@Configuration(proxyBeanMethods = false)
public class DefaultSecurityConfig {

    private static final String CUSTOM_LOGIN_PAGE = "/login";

    private static final String REMEMBER_ME_COOKIE_NAME = "armorauth-remember-me";


    @Bean(name = ConfigBeanNameConstants.DEFAULT_SECURITY_FILTER_CHAIN)
    @Order(Ordered.HIGHEST_PRECEDENCE + 2)
    public SecurityFilterChain defaultSecurityFilterChain(
            HttpSecurity http,
            DelegateUserDetailsService delegateUserDetailsService) throws Exception {
        AuthenticationEntryPointFailureHandler authenticationFailureHandler =
                new AuthenticationEntryPointFailureHandler(new FailureAuthenticationEntryPoint());
        FederatedAuthenticationSuccessHandler federatedAuthenticationSuccessHandler =
                new FederatedAuthenticationSuccessHandler();
        http.authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        .anyRequest().authenticated()
                )
                .csrf(AbstractHttpConfigurer::disable)
                .userDetailsService(delegateUserDetailsService);
        // OAuth2UserLoginFilterSecurityConfigurer Customizer
        http.apply(new OAuth2UserLoginFilterSecurityConfigurer())
                .formLogin(formLogin -> formLogin
                        .loginPage(CUSTOM_LOGIN_PAGE).permitAll()
                        .successHandler(federatedAuthenticationSuccessHandler)
                        .failureHandler(authenticationFailureHandler)
                )
                .captchaLogin(captchaLogin -> captchaLogin
                        .captchaVerifyService(this::verifyCaptchaMock)
                        .userDetailsService(delegateUserDetailsService)
                        .successHandler(federatedAuthenticationSuccessHandler)
                        .failureHandler(authenticationFailureHandler)
                )
                .rememberMe(rememberMe -> rememberMe
                        .rememberMeCookieName(REMEMBER_ME_COOKIE_NAME)
                        .userDetailsService(delegateUserDetailsService)
                );
        return http.build();
    }

    private boolean verifyCaptchaMock(String account, String captcha) {
        return captcha.equals("1234");
    }

    //*********************************************UserDetailsService*********************************************//


    @Bean
    public DelegateUserDetailsService delegateUserDetailsService(UserInfoRepository userInfoRepository) {
        return new DelegateUserDetailsService(userInfoRepository);
    }


    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }


    //*********************************************SessionRegistry*********************************************//

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring()
                .requestMatchers("/error")
                .requestMatchers("/favicon.ico")
                .requestMatchers("/static/**")
                .requestMatchers("/resources/**")
                .requestMatchers("/assets/**")
                .requestMatchers("/webjars/**")
                .requestMatchers("/h2-console/**")
                .requestMatchers("/actuator/health")
                .requestMatchers("/system/monitor")
                ;
    }

    //*********************************************WebSecurity*********************************************//


    @Configuration(proxyBeanMethods = false)
    public static class FederatedLoginConfig {

        //*********************************************FederatedLoginConfigurer*********************************************//

        @Bean
        @Order(Ordered.HIGHEST_PRECEDENCE + 1)
        public SecurityFilterChain federatedSecurityFilterChain(HttpSecurity http,
                                                                ClientRegistrationRepository clientRegistrationRepository
        ) throws Exception {

            FederatedLoginConfigurer federatedLoginConfigurer = new FederatedLoginConfigurer();
            RequestMatcher endpointsMatcher = federatedLoginConfigurer.getEndpointsMatcher();
            http.securityMatcher(endpointsMatcher);
            http.apply(federatedLoginConfigurer);
            FederatedAuthenticationEntryPoint authenticationEntryPoint =
                    new FederatedAuthenticationEntryPoint(CUSTOM_LOGIN_PAGE, clientRegistrationRepository);
            http.exceptionHandling(exceptionHandling ->
                    exceptionHandling
                            .authenticationEntryPoint(authenticationEntryPoint)
            );
            //OAuth 授权地址转换 OAuth2AuthorizationRequestConverter
            List<OAuth2AuthorizationRequestConverter> authorizationRequestConverters = new ArrayList<>();
            authorizationRequestConverters.add(new WechatAuthorizationRequestConverter());
            DelegatingAuthorizationRequestResolver delegatingAuthorizationRequestResolver =
                    new DelegatingAuthorizationRequestResolver(clientRegistrationRepository, authorizationRequestConverters);
            //OAuth 请求AccessToken的RestTemplate转换 OAuth2AccessTokenRestTemplateConverter
            List<OAuth2AccessTokenRestTemplateConverter> restTemplates = new ArrayList<>();
            List<OAuth2AuthorizationCodeGrantRequestConverter> authorizationCodeGrantRequestConverters = new ArrayList<>();
            restTemplates.add(new WechatAccessTokenRestTemplateConverter());
            authorizationCodeGrantRequestConverters.add(new WechatAuthorizationCodeGrantRequestConverter());
            restTemplates.add(new QqAccessTokenRestTemplateConverter());
            authorizationCodeGrantRequestConverters.add(new QqAuthorizationCodeGrantRequestConverter());
            DelegatingAccessTokenResponseClient accessTokenResponseClient = new DelegatingAccessTokenResponseClient(
                    restTemplates,
                    authorizationCodeGrantRequestConverters
            );
            //OAuth 查询用户信息 UserService
            Map<String, OAuth2UserService<OAuth2UserRequest, OAuth2User>> userServices = new HashMap<>();
            userServices.put(ExtendedOAuth2ClientProvider.getNameLowerCase(GITEE), new GiteeOAuth2UserService());
            userServices.put(ExtendedOAuth2ClientProvider.getNameLowerCase(QQ), new GiteeOAuth2UserService());
            userServices.put(ExtendedOAuth2ClientProvider.getNameLowerCase(WECHAT), new GiteeOAuth2UserService());
            DelegatingOAuth2UserService delegatingOAuth2UserService = new DelegatingOAuth2UserService(userServices);
            //OAuth2LoginConfigurer
            http.getConfigurer(FederatedLoginConfigurer.class)
                    .loginPage(CUSTOM_LOGIN_PAGE)
                    .authorizationEndpoint(authorizationEndpoint -> authorizationEndpoint
                            .authorizationRequestResolver(delegatingAuthorizationRequestResolver)
                    )
                    .tokenEndpoint(tokenEndpoint -> tokenEndpoint
                            .accessTokenResponseClient(accessTokenResponseClient)
                    )
                    .userInfoEndpoint(userInfoEndpoint -> userInfoEndpoint
                            .userService(delegatingOAuth2UserService)
                            .bindUserPage("/bind")
                    )
            ;
            return http.build();
        }


        @Bean
        public ClientRegistrationRepository clientRegistrationRepository(@Autowired(required = false) OAuth2ClientProperties properties) {
            InMemoryClientRegistrationRepository clientRegistrations;
            ExtendedOAuth2ClientPropertiesMapper extendedOAuth2ClientPropertiesMapper = new ExtendedOAuth2ClientPropertiesMapper(properties);
            Map<String, ClientRegistration> extendedClientRegistrations = extendedOAuth2ClientPropertiesMapper.asClientRegistrations();
            clientRegistrations = new InMemoryClientRegistrationRepository(extendedClientRegistrations);
            return clientRegistrations;
        }

        @Bean
        public OAuth2AuthorizedClientService authorizedClientService(
                JdbcTemplate jdbcTemplate,
                ClientRegistrationRepository clientRegistrationRepository) {
            return new JdbcOAuth2AuthorizedClientService(jdbcTemplate, clientRegistrationRepository);
        }
    }

}
