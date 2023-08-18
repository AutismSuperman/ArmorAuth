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


import com.armorauth.configurers.OAuth2FederatedLoginServerConfigurer;
import com.armorauth.configurers.web.OAuth2UserLoginFilterSecurityConfigurer;
import com.armorauth.data.repository.UserInfoRepository;
import com.armorauth.details.DelegateUserDetailsService;
import com.armorauth.federat.ExtendedOAuth2ClientPropertiesMapper;
import com.armorauth.security.FailureAuthenticationEntryPoint;
import com.armorauth.security.FederatedAuthenticationSuccessHandler;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientPropertiesMapper;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientPropertiesRegistrationAdapter;
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
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationEntryPointFailureHandler;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;

import java.util.*;

@EnableWebSecurity(debug = true)
@Configuration(proxyBeanMethods = false)
public class DefaultSecurityConfig {

    private static final String CUSTOM_LOGIN_PAGE = "/login";

    private static final String REMEMBER_ME_COOKIE_NAME = "armorauth-remember-me";


    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 1)
    SecurityFilterChain defaultSecurityFilterChain(
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

        // OAuth2FederatedLoginServerConfigurer Customizer
        http.apply(new OAuth2FederatedLoginServerConfigurer())
                .federatedAuthorization(federatedAuthorization -> federatedAuthorization
                        .loginPageUrl(CUSTOM_LOGIN_PAGE)
                )
        ;
        DefaultSecurityFilterChain build = http.build();
        System.out.println(http);
        return build;
    }

    private boolean verifyCaptchaMock(String account, String captcha) {
        return captcha.equals("1234");
    }

    //*********************************************UserDetailsService*********************************************//


    @Bean
    public DelegateUserDetailsService delegateUserDetailsService(UserInfoRepository userInfoRepository) {
        return new DelegateUserDetailsService(userInfoRepository);
    }


    //*********************************************ClientRegistration*********************************************//

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
        // TODO 保存认证信息 可以扩展
        return new JdbcOAuth2AuthorizedClientService(jdbcTemplate, clientRegistrationRepository);
    }


    //*********************************************SessionRegistry*********************************************//

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    //*********************************************WebSecurity*********************************************//

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
                .requestMatchers("/system/monitor")
                ;
    }


}
