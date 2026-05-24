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

import java.net.URI;
import java.net.URISyntaxException;

import com.armorauth.authentication.CaptchaVerifyService;
import com.armorauth.authentication.MfaAuthenticationSuccessHandler;
import com.armorauth.authentication.MfaPolicyService;
import com.armorauth.authentication.SmsOtpService;
import com.armorauth.captcha.GraphicCaptchaService;
import com.armorauth.configurers.web.CaptchaLoginConfigurer;
import com.armorauth.configurers.web.MfaLoginConfigurer;
import com.armorauth.crypto.SecretCryptoService;
import com.armorauth.data.repository.AuthFactorRepository;
import com.armorauth.data.repository.UserInfoRepository;
import com.armorauth.data.repository.UserRoleRepository;
import com.armorauth.details.DelegateUserDetailsService;
import com.armorauth.mfa.TotpService;
import com.armorauth.security.AuditEventPublisher;
import com.armorauth.security.LoginLockoutService;
import com.armorauth.security.SecurityAuditUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.util.StringUtils;

@EnableWebSecurity
@Configuration(proxyBeanMethods = false)
public class DefaultSecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(DefaultSecurityConfig.class);

    private static final String CUSTOM_LOGIN_PAGE = "/login";

    private static final String REMEMBER_ME_COOKIE_NAME = "armorauth-remember-me";

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 1)
    SecurityFilterChain defaultSecurityFilterChain(
            HttpSecurity http,
            DelegateUserDetailsService delegateUserDetailsService,
            @Qualifier("formAuthenticationSuccessHandler") AuthenticationSuccessHandler authenticationSuccessHandler,
            SecurityContextRepository securityContextRepository,
            ObjectProvider<CaptchaVerifyService> captchaVerifyServiceProvider,
            ObjectProvider<GraphicCaptchaService> graphicCaptchaServiceProvider,
            ObjectProvider<SmsOtpService> smsOtpServiceProvider,
            ObjectProvider<AuthFactorRepository> authFactorRepositoryProvider,
            ObjectProvider<UserInfoRepository> userInfoRepositoryProvider,
            ObjectProvider<TotpService> totpServiceProvider,
            ObjectProvider<SecretCryptoService> secretCryptoServiceProvider,
            ObjectProvider<MfaPolicyService> mfaPolicyServiceProvider,
            RequestCache requestCache,
            AuditEventPublisher auditEventPublisher,
            LoginLockoutService loginLockoutService,
            PasswordEncoder passwordEncoder,
            ObjectProvider<ArmorAuthSecurityCustomizer> securityCustomizers) throws Exception {
        SimpleUrlAuthenticationFailureHandler delegateFailureHandler =
                new SimpleUrlAuthenticationFailureHandler(CUSTOM_LOGIN_PAGE + "?error");
        SimpleUrlAuthenticationFailureHandler graphicCaptchaFailureHandler =
                new SimpleUrlAuthenticationFailureHandler(CUSTOM_LOGIN_PAGE + "?error&tab=graphic");
        SimpleUrlAuthenticationFailureHandler smsCaptchaFailureHandler =
                new SimpleUrlAuthenticationFailureHandler(CUSTOM_LOGIN_PAGE + "?error&tab=sms");
        AuthenticationFailureHandler authenticationFailureHandler = (request, response, exception) -> {
            String username = request.getParameter("username");
            if (!StringUtils.hasText(username)) {
                username = request.getParameter("account");
            }
            if (StringUtils.hasText(username)) {
                loginLockoutService.recordFailure(username);
            }
            auditEventPublisher.publishLoginFailure(
                    StringUtils.hasText(username) ? username : "unknown",
                    SecurityAuditUtils.getRemoteAddress(request),
                    "认证失败: " + exception.getMessage());
            if (request.getRequestURI().endsWith("/login/captcha")) {
                if (StringUtils.hasText(request.getParameter("captchaId"))) {
                    graphicCaptchaFailureHandler.onAuthenticationFailure(request, response, exception);
                }
                else {
                    smsCaptchaFailureHandler.onAuthenticationFailure(request, response, exception);
                }
            }
            else {
                delegateFailureHandler.onAuthenticationFailure(request, response, exception);
            }
        };

        // 如果配置了 MFA，使用 MFA 感知的成功处理器
        final AuthFactorRepository authFactorRepository = authFactorRepositoryProvider.getIfAvailable();
        final UserInfoRepository userInfoRepository = userInfoRepositoryProvider.getIfAvailable();
        final MfaPolicyService mfaPolicyService = mfaPolicyServiceProvider.getIfAvailable();
        final AuthenticationSuccessHandler effectiveSuccessHandler =
                authFactorRepository != null
                        ? new MfaAuthenticationSuccessHandler(authFactorRepository, authenticationSuccessHandler,
                                mfaPolicyService, userInfoRepository)
                        : authenticationSuccessHandler;

        http.securityMatchers(securityMatchers -> securityMatchers
                        .requestMatchers(request -> !request.getRequestURI().startsWith("/api/admin")
                                && !request.getRequestURI().startsWith("/scim/v2")))
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        .requestMatchers("/login").permitAll()
                        .requestMatchers("/login/mfa").permitAll()
                        .requestMatchers("/login/passkey/options").permitAll()
                        .requestMatchers("/login/passkey/finish").permitAll()
                        .requestMatchers("/login/passkey/assertion/options").permitAll()
                        .requestMatchers("/login/passkey/assertion/finish").permitAll()
                        .requestMatchers("/login/captcha").permitAll()
                        .requestMatchers("/login/captcha/send").permitAll()
                        .requestMatchers("/login/captcha/image").permitAll()
                        .requestMatchers("/login/captcha/info").permitAll()
                        .requestMatchers("/federated/confirm").permitAll()
                        .requestMatchers("/federated/confirm/create").permitAll()
                        .requestMatchers("/federated/confirm/bind").permitAll()
                        .requestMatchers("/saml2/authorization/**").permitAll()
                        .requestMatchers("/saml2/authenticate/**").permitAll()
                        .requestMatchers("/login/saml2/sso/**").permitAll()
                        .requestMatchers("/saml2/service-provider-metadata/**").permitAll()
                        .anyRequest().authenticated())
                .csrf(AbstractHttpConfigurer::disable)
                .requestCache(requestCacheConfigurer -> requestCacheConfigurer.requestCache(requestCache))
                .logout(logout -> logout
                    .addLogoutHandler((request, response, authentication) -> {
                        if (authentication != null) {
                            auditEventPublisher.publishLogout(
                                    authentication.getName(),
                                    SecurityAuditUtils.getRemoteAddress(request),
                                    "用户登出: " + authentication.getName());
                        }
                    })
                    .logoutSuccessHandler((request, response, authentication) -> {
                        log.info("Logout succeeded username={} remoteAddress={} uri={}",
                                SecurityAuditUtils.getAuthenticationName(authentication),
                                SecurityAuditUtils.getRemoteAddress(request), request.getRequestURI());
                        response.sendRedirect(request.getContextPath() + CUSTOM_LOGIN_PAGE + "?logout");
                    }))
                .securityContext(securityContext -> securityContext.securityContextRepository(securityContextRepository))
                .userDetailsService(delegateUserDetailsService);

        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider(delegateUserDetailsService);
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder);
        http.authenticationProvider(daoAuthenticationProvider);

        http.formLogin(formLogin -> formLogin
                        .loginPage(CUSTOM_LOGIN_PAGE)
                        .successHandler(effectiveSuccessHandler)
                        .failureHandler(authenticationFailureHandler)
                        .permitAll())
                .rememberMe(rememberMe -> rememberMe
                        .rememberMeCookieName(REMEMBER_ME_COOKIE_NAME)
                        .userDetailsService(delegateUserDetailsService));

        GraphicCaptchaService graphicCaptchaService = graphicCaptchaServiceProvider.getIfAvailable();
        SmsOtpService smsOtpService = smsOtpServiceProvider.getIfAvailable();
        CaptchaVerifyService captchaVerifyService = smsOtpService != null
                ? smsOtpService::verifyOtp
                : captchaVerifyServiceProvider.getIfAvailable();
        if (captchaVerifyService != null) {
            http.with(new CaptchaLoginConfigurer<>(), captchaLogin -> captchaLogin
                    .loginPage(CUSTOM_LOGIN_PAGE)
                    .captchaVerifyService(captchaVerifyService)
                    .graphicCaptchaService(graphicCaptchaService)
                    .userDetailsService(delegateUserDetailsService)
                    .securityContextRepository(securityContextRepository)
                    .successHandler(effectiveSuccessHandler)
                    .failureHandler(authenticationFailureHandler));
        }

        // MFA 认证链
        TotpService totpService = totpServiceProvider.getIfAvailable();
        SecretCryptoService secretCryptoService = secretCryptoServiceProvider.getIfAvailable();
        if (authFactorRepository != null && totpService != null) {
            http.with(new MfaLoginConfigurer<>(), mfaLogin -> mfaLogin
                    .userDetailsService(delegateUserDetailsService)
                    .authFactorRepository(authFactorRepository)
                    .userInfoRepository(userInfoRepository)
                    .totpService(totpService)
                    .secretCryptoService(secretCryptoService)
                    .securityContextRepository(securityContextRepository)
                    .successHandler(authenticationSuccessHandler)
                    .failureHandler(new SimpleUrlAuthenticationFailureHandler("/login/mfa?error")));
        }

        for (ArmorAuthSecurityCustomizer securityCustomizer : securityCustomizers.orderedStream().toList()) {
            securityCustomizer.customize(http);
        }

        return http.build();
    }

    @Bean
    public DelegateUserDetailsService delegateUserDetailsService(UserInfoRepository userInfoRepository,
                                                                  UserRoleRepository userRoleRepository,
                                                                  LoginLockoutService loginLockoutService) {
        DelegateUserDetailsService service = new DelegateUserDetailsService(userInfoRepository, userRoleRepository);
        service.setLoginLockoutService(loginLockoutService);
        return service;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Bean
    public RequestCache requestCache() {
        HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
        requestCache.setRequestMatcher(request -> HttpMethod.GET.matches(request.getMethod())
                && isCacheableRequestPath(request.getRequestURI(), request.getContextPath()));
        return requestCache;
    }

    @Bean
    public AuthenticationSuccessHandler formAuthenticationSuccessHandler(RequestCache requestCache,
                                                                          AuditEventPublisher auditEventPublisher,
                                                                          LoginLockoutService loginLockoutService) {
        SavedRequestAwareAuthenticationSuccessHandler successHandler =
                new SavedRequestAwareAuthenticationSuccessHandler();
        successHandler.setDefaultTargetUrl("/");
        successHandler.setRequestCache(requestCache);
        return (request, response, authentication) -> {
            SavedRequest savedRequest = requestCache.getRequest(request, response);
            String targetUrl = "/";
            if (savedRequest != null && !isCacheableRedirectUrl(savedRequest.getRedirectUrl(), request.getContextPath())) {
                requestCache.removeRequest(request, response);
                savedRequest = null;
            }
            if (savedRequest != null) {
                targetUrl = savedRequest.getRedirectUrl();
            }
            log.info("Login succeeded username={} remoteAddress={} uri={} target={}",
                    SecurityAuditUtils.getAuthenticationName(authentication),
                    SecurityAuditUtils.getRemoteAddress(request), request.getRequestURI(), targetUrl);
            loginLockoutService.recordSuccess(authentication.getName());
            auditEventPublisher.publishLoginSuccess(
                    authentication.getName(),
                    SecurityAuditUtils.getRemoteAddress(request),
                    "认证成功: " + authentication.getName());
            successHandler.onAuthenticationSuccess(request, response, authentication);
        };
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public AuditEventPublisher auditEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        return new AuditEventPublisher(applicationEventPublisher);
    }

    @Bean
    WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring()
                .requestMatchers("/error")
                .requestMatchers("/favicon.ico")
                .requestMatchers("/favicon.svg")
                .requestMatchers("/static/**")
                .requestMatchers("/resources/**")
                .requestMatchers("/assets/**")
                .requestMatchers("/brand/**")
                .requestMatchers("/oauth/**")
                .requestMatchers("/webjars/**")
                .requestMatchers("/h2-console/**")
                .requestMatchers("/actuator/health")
                .requestMatchers("/system/monitor");
    }

    private static boolean isCacheableRedirectUrl(String redirectUrl, String contextPath) {
        try {
            URI uri = new URI(redirectUrl);
            return isCacheableRequestPath(uri.getPath(), contextPath);
        } catch (URISyntaxException ex) {
            return false;
        }
    }

    private static boolean isCacheableRequestPath(String requestUri, String contextPath) {
        if (requestUri == null || requestUri.isBlank()) {
            return false;
        }
        String path = requestUri;
        if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        return !path.startsWith("/.well-known/")
                && !path.equals(CUSTOM_LOGIN_PAGE)
                && !path.startsWith(CUSTOM_LOGIN_PAGE + "/")
                && !path.startsWith("/assets/")
                && !path.startsWith("/brand/")
                && !path.equals("/favicon.ico")
                && !path.equals("/favicon.svg")
                && !path.equals("/error");
    }

}
