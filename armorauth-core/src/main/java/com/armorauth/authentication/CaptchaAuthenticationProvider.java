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
package com.armorauth.authentication;

import com.armorauth.captcha.GraphicCaptchaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.NullAuthoritiesMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.Collection;

/**
 * 验证码认证器
 *
 * @author fulin
 */
public class CaptchaAuthenticationProvider implements AuthenticationProvider, InitializingBean, MessageSourceAware {

    private static final Logger log = LoggerFactory.getLogger(CaptchaAuthenticationProvider.class);

    private final GrantedAuthoritiesMapper authoritiesMapper = new NullAuthoritiesMapper();
    private final UserDetailsService userDetailsService;
    private final CaptchaVerifyService captchaService;
    private final GraphicCaptchaService graphicCaptchaService;
    private MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();

    public CaptchaAuthenticationProvider(UserDetailsService userDetailsService, CaptchaVerifyService captchaService) {
        this(userDetailsService, captchaService,
                captchaService instanceof GraphicCaptchaService graphicCaptchaService ? graphicCaptchaService : null);
    }

    public CaptchaAuthenticationProvider(UserDetailsService userDetailsService, CaptchaVerifyService captchaService,
                                         GraphicCaptchaService graphicCaptchaService) {
        this.userDetailsService = userDetailsService;
        this.captchaService = captchaService;
        this.graphicCaptchaService = graphicCaptchaService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Assert.isInstanceOf(CaptchaAuthenticationToken.class, authentication,
                () -> messages.getMessage(
                        "CaptchaAuthenticationProvider.onlySupports",
                        "Only CaptchaAuthenticationToken is supported"));
        CaptchaAuthenticationToken unAuthenticationToken = (CaptchaAuthenticationToken) authentication;
        String phone = unAuthenticationToken.getName();
        String rawCode = (String) unAuthenticationToken.getCredentials();
        log.info("Processing captcha authentication for account={}", maskAccount(phone));
        // verifyCaptcha
        if (verifyCaptcha(unAuthenticationToken, phone, rawCode)) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(phone);
            log.info("Captcha authentication succeeded for account={}", maskAccount(phone));
            return createSuccessAuthentication(authentication, userDetails);
        } else {
            log.warn("Captcha authentication failed due to invalid captcha for account={}", maskAccount(phone));
            throw new BadCredentialsException("Captcha is not matched");
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return CaptchaAuthenticationToken.class.isAssignableFrom(authentication);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(userDetailsService, "userDetailsService must not be null");
        Assert.notNull(captchaService, "captchaService must not be null");
    }

    private boolean verifyCaptcha(CaptchaAuthenticationToken authenticationToken, String account, String rawCode) {
        String captchaId = authenticationToken.getCaptchaId();
        if (StringUtils.hasText(captchaId)) {
            if (graphicCaptchaService != null) {
                return graphicCaptchaService.verify(captchaId, rawCode);
            }
            if (captchaService instanceof GraphicCaptchaService captchaServiceAdapter) {
                return captchaServiceAdapter.verify(captchaId, rawCode);
            }
        }
        return captchaService.verifyCaptcha(account, rawCode);
    }

    @Override
    public void setMessageSource(MessageSource messageSource) {
        this.messages = new MessageSourceAccessor(messageSource);
    }

    /**
     * 认证成功将非授信凭据转为授信凭据.
     * 封装用户信息 角色信息。
     *
     * @param authentication the authentication
     * @param user           the user
     * @return the authentication
     */
    protected Authentication createSuccessAuthentication(Authentication authentication, UserDetails user) {
        Collection<? extends GrantedAuthority> authorities = authoritiesMapper.mapAuthorities(user.getAuthorities());
        CaptchaAuthenticationToken authenticationToken = new CaptchaAuthenticationToken(user, null, authorities);
        authenticationToken.setDetails(authentication.getDetails());
        return authenticationToken;
    }

    private String maskAccount(String account) {
        if (account == null || account.isBlank()) {
            return "unknown";
        }
        if (account.length() <= 4) {
            return account;
        }
        return account.substring(0, 2) + "***" + account.substring(account.length() - 2);
    }

}
