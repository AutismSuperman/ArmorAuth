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
package com.armorauth.configurers.web;

import com.armorauth.authentication.MfaAuthenticationFilter;
import com.armorauth.authentication.MfaAuthenticationProvider;
import com.armorauth.crypto.SecretCryptoService;
import com.armorauth.data.repository.AuthFactorRepository;
import com.armorauth.data.repository.UserInfoRepository;
import com.armorauth.mfa.TotpService;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;

/**
 * MFA 登录配置器
 * <p>
 * 将 MfaAuthenticationFilter 和 MfaAuthenticationProvider 注入到安全过滤器链中
 *
 * @author fulin
 * @since 2026-05-23
 */
public class MfaLoginConfigurer<H extends HttpSecurityBuilder<H>>
        extends AbstractHttpConfigurer<MfaLoginConfigurer<H>, H> {

    private MfaAuthenticationFilter mfaFilter;
    private UserDetailsService userDetailsService;
    private AuthFactorRepository authFactorRepository;
    private UserInfoRepository userInfoRepository;
    private TotpService totpService;
    private SecretCryptoService secretCryptoService;
    private SecurityContextRepository securityContextRepository;
    private AuthenticationSuccessHandler successHandler;
    private AuthenticationFailureHandler failureHandler;

    public MfaLoginConfigurer<H> userDetailsService(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
        return this;
    }

    public MfaLoginConfigurer<H> authFactorRepository(AuthFactorRepository authFactorRepository) {
        this.authFactorRepository = authFactorRepository;
        return this;
    }

    public MfaLoginConfigurer<H> userInfoRepository(UserInfoRepository userInfoRepository) {
        this.userInfoRepository = userInfoRepository;
        return this;
    }

    public MfaLoginConfigurer<H> totpService(TotpService totpService) {
        this.totpService = totpService;
        return this;
    }

    public MfaLoginConfigurer<H> secretCryptoService(SecretCryptoService secretCryptoService) {
        this.secretCryptoService = secretCryptoService;
        return this;
    }

    public MfaLoginConfigurer<H> securityContextRepository(SecurityContextRepository securityContextRepository) {
        this.securityContextRepository = securityContextRepository;
        return this;
    }

    public MfaLoginConfigurer<H> successHandler(AuthenticationSuccessHandler successHandler) {
        this.successHandler = successHandler;
        return this;
    }

    public MfaLoginConfigurer<H> failureHandler(AuthenticationFailureHandler failureHandler) {
        this.failureHandler = failureHandler;
        return this;
    }

    @Override
    public void configure(H http) {
        MfaAuthenticationProvider mfaProvider = new MfaAuthenticationProvider(
                userDetailsService, authFactorRepository, totpService, userInfoRepository, secretCryptoService);
        http.authenticationProvider(mfaProvider);

        mfaFilter = new MfaAuthenticationFilter();
        mfaFilter.setAuthenticationManager(http.getSharedObject(org.springframework.security.authentication.AuthenticationManager.class));
        if (successHandler != null) {
            mfaFilter.setAuthenticationSuccessHandler(successHandler);
        }
        if (failureHandler != null) {
            mfaFilter.setAuthenticationFailureHandler(failureHandler);
        }
        if (securityContextRepository != null) {
            mfaFilter.setSecurityContextRepository(securityContextRepository);
        }
        SecurityContextHolderStrategy securityContextHolderStrategy =
                http.getSharedObject(SecurityContextHolderStrategy.class);
        if (securityContextHolderStrategy == null) {
            securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();
        }
        mfaFilter.setSecurityContextHolderStrategy(securityContextHolderStrategy);

        MfaAuthenticationFilter filter = postProcess(mfaFilter);
        http.addFilterAfter(filter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
    }

    private static final PathPatternRequestMatcher MFA_MATCHER =
            PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/login/mfa");
}
