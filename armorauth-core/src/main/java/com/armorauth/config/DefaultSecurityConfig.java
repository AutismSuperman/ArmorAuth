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


import com.armorauth.configurers.IdentityAuthorizationServerConfigurer;
import com.armorauth.configurers.web.Oauth2UserLoginFilterSecurityConfigurer;
import com.armorauth.data.repository.UserInfoRepository;
import com.armorauth.details.DelegateUserDetailsService;
import com.armorauth.security.FailureAuthenticationEntryPoint;
import com.armorauth.security.FederatedAuthenticationSuccessHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientPropertiesRegistrationAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.oauth2.client.JdbcOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationEntryPointFailureHandler;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;

import java.util.ArrayList;
import java.util.List;

@EnableWebSecurity(debug = true)
@Configuration(proxyBeanMethods = false)
public class DefaultSecurityConfig {


    private static final String CUSTOM_LOGIN_PAGE = "/login";


    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 1)
    SecurityFilterChain defaultSecurityFilterChain(
            HttpSecurity http,
            DelegateUserDetailsService delegateUserDetailsService,
            @Qualifier("authorizationServerSecurityFilterChain") SecurityFilterChain securityFilterChain
    )
            throws Exception {
        DefaultSecurityFilterChain authorizationServerFilterChain = (DefaultSecurityFilterChain) securityFilterChain;
        IdentityAuthorizationServerConfigurer identityAuthorizationServerConfigurer =
                new IdentityAuthorizationServerConfigurer();
        AuthenticationEntryPointFailureHandler authenticationFailureHandler =
                new AuthenticationEntryPointFailureHandler(new FailureAuthenticationEntryPoint());
        FederatedAuthenticationSuccessHandler federatedAuthenticationSuccessHandler =
                new FederatedAuthenticationSuccessHandler();
        http.requestMatcher(
                        new AndRequestMatcher(
                                new NegatedRequestMatcher(authorizationServerFilterChain.getRequestMatcher())
                        ))
                .authorizeRequests(authorizeRequests ->
                        authorizeRequests.anyRequest().authenticated()
                )
                .csrf().disable()
                .userDetailsService(delegateUserDetailsService)
                .formLogin(formLogin -> formLogin
                        .loginPage(CUSTOM_LOGIN_PAGE).permitAll()
                        .successHandler(federatedAuthenticationSuccessHandler)
                        .failureHandler(authenticationFailureHandler)
                )
                .rememberMe(rememberMe -> rememberMe
                        .rememberMeCookieName("armorauth-remember-me")
                        .userDetailsService(delegateUserDetailsService)
                );
        // OAuth2UserLoginFilterSecurityConfigurer Customizer
        http.apply(new Oauth2UserLoginFilterSecurityConfigurer<>())
                .captchaLogin(captchaLogin -> captchaLogin
                        .captchaVerifyService(this::verifyCaptchaMock)
                        .userDetailsService(delegateUserDetailsService)
                        .successHandler(federatedAuthenticationSuccessHandler)
                        .failureHandler(authenticationFailureHandler)
                )
        ;
        // AuthorizationServerConfigurer Customizer
        http.apply(identityAuthorizationServerConfigurer);
        http.getConfigurer(IdentityAuthorizationServerConfigurer.class)
                .federatedAuthorization(federatedAuthorization -> federatedAuthorization
                        .loginPageUrl(CUSTOM_LOGIN_PAGE)
                )
        ;
        return http.build();
    }

    private boolean verifyCaptchaMock(String account, String captcha) {
        return captcha.equals("1234");
    }


    @Bean
    public DelegateUserDetailsService delegateUserDetailsService(UserInfoRepository userInfoRepository) {
        return new DelegateUserDetailsService(userInfoRepository);
    }


    @Bean
    public InMemoryClientRegistrationRepository clientRegistrationRepository(@Autowired(required = false) OAuth2ClientProperties properties) {
        // TODO 数据库扩展
        InMemoryClientRegistrationRepository clientRegistrations;
        List<ClientRegistration> registrations = new ArrayList<>(
                OAuth2ClientPropertiesRegistrationAdapter.getClientRegistrations(properties).values());
        clientRegistrations = new InMemoryClientRegistrationRepository(registrations);
        return clientRegistrations;
    }

    @Bean
    public OAuth2AuthorizedClientService authorizedClientService(
            JdbcTemplate jdbcTemplate,
            ClientRegistrationRepository clientRegistrationRepository) {
        // 保存认证信息
        return new JdbcOAuth2AuthorizedClientService(jdbcTemplate, clientRegistrationRepository);
    }


    @Bean
    WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring()
                .antMatchers("/error")
                .antMatchers("/favicon.ico")
                .antMatchers("/static/**")
                .antMatchers("/resources/**")
                .antMatchers("/webjars/**")
                .antMatchers("/h2-console/**")
                .antMatchers("/actuator/health")
                .antMatchers("/system/monitor")
                ;
    }


}
