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

import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CaptchaAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    public static final String SPRING_SECURITY_FORM_ACCOUNT_KEY = "account";

    public static final String SPRING_SECURITY_FORM_CAPTCHA_KEY = "captcha";

    private static final AntPathRequestMatcher DEFAULT_ANT_PATH_REQUEST_MATCHER = new AntPathRequestMatcher("/login/captcha",
            "POST");

    private String accountParameter = SPRING_SECURITY_FORM_ACCOUNT_KEY;

    private String captchaParameter = SPRING_SECURITY_FORM_CAPTCHA_KEY;

    private Converter<HttpServletRequest, CaptchaAuthenticationToken> captchaAuthenticationTokenConverter;

    private boolean postOnly = true;


    public CaptchaAuthenticationFilter() {
        super(DEFAULT_ANT_PATH_REQUEST_MATCHER);
        this.captchaAuthenticationTokenConverter = defaultConverter();
    }

    public CaptchaAuthenticationFilter(AuthenticationManager authenticationManager) {
        super(DEFAULT_ANT_PATH_REQUEST_MATCHER, authenticationManager);
        this.captchaAuthenticationTokenConverter = defaultConverter();
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        if (this.postOnly && !HttpMethod.POST.matches(request.getMethod())) {
            throw new AuthenticationServiceException("Authentication method not supported: " + request.getMethod());
        }
        CaptchaAuthenticationToken authRequest = captchaAuthenticationTokenConverter.convert(request);
        // Allow subclasses to set the "details" property
        assert authRequest != null;
        setDetails(request, authRequest);
        return this.getAuthenticationManager().authenticate(authRequest);
    }


    private Converter<HttpServletRequest, CaptchaAuthenticationToken> defaultConverter() {
        return request -> {
            String account = request.getParameter(this.accountParameter);
            account = (account != null) ? account.trim() : "";
            String captcha = request.getParameter(this.captchaParameter);
            captcha = (captcha != null) ? captcha.trim() : "";
            return new CaptchaAuthenticationToken(account, captcha);
        };
    }


    protected void setDetails(HttpServletRequest request, CaptchaAuthenticationToken authRequest) {
        authRequest.setDetails(this.authenticationDetailsSource.buildDetails(request));
    }

    public void setAccountParameter(String accountParameter) {
        Assert.hasText(accountParameter, "account parameter must not be empty or null");
        this.accountParameter = accountParameter;
    }

    public void setCaptchaParameter(String captchaParameter) {
        Assert.hasText(captchaParameter, "captcha parameter must not be empty or null");
        this.captchaParameter = captchaParameter;
    }

    public void setConverter(Converter<HttpServletRequest, CaptchaAuthenticationToken> converter) {
        Assert.notNull(converter, "Converter must not be null");
        this.captchaAuthenticationTokenConverter = converter;
    }

    public void setPostOnly(boolean postOnly) {
        this.postOnly = postOnly;
    }

    public final String getAccountParameter() {
        return this.accountParameter;
    }

    public final String getCaptchaParameter() {
        return this.captchaParameter;
    }
}