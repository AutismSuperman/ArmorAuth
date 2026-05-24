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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.http.HttpMethod;

/**
 * MFA 认证过滤器
 * <p>
 * 拦截 POST /login/mfa 请求，提取 MFA 验证码进行二次认证
 *
 * @author fulin
 * @since 2026-05-23
 */
public class MfaAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    public static final String MFA_CODE_PARAMETER = "mfaCode";
    public static final String MFA_FACTOR_ID_PARAMETER = "factorId";
    public static final String MFA_PRINCIPAL_PARAMETER = "principal";

    private static final PathPatternRequestMatcher DEFAULT_MATCHER =
            PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/login/mfa");

    private boolean postOnly = true;

    public MfaAuthenticationFilter() {
        super(DEFAULT_MATCHER);
    }

    public MfaAuthenticationFilter(AuthenticationManager authenticationManager) {
        super(DEFAULT_MATCHER, authenticationManager);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        if (this.postOnly && !HttpMethod.POST.matches(request.getMethod())) {
            throw new AuthenticationServiceException("Authentication method not supported: " + request.getMethod());
        }

        String principal = obtainPrincipal(request);
        String mfaCode = obtainMfaCode(request);
        String factorId = obtainFactorId(request);

        principal = (principal != null) ? principal.trim() : "";
        mfaCode = (mfaCode != null) ? mfaCode.trim() : "";

        MfaAuthenticationToken authRequest = new MfaAuthenticationToken(principal, mfaCode, factorId);
        setDetails(request, authRequest);
        return this.getAuthenticationManager().authenticate(authRequest);
    }

    protected String obtainPrincipal(HttpServletRequest request) {
        return request.getParameter(MFA_PRINCIPAL_PARAMETER);
    }

    protected String obtainMfaCode(HttpServletRequest request) {
        return request.getParameter(MFA_CODE_PARAMETER);
    }

    protected String obtainFactorId(HttpServletRequest request) {
        return request.getParameter(MFA_FACTOR_ID_PARAMETER);
    }

    protected void setDetails(HttpServletRequest request, MfaAuthenticationToken authRequest) {
        authRequest.setDetails(this.authenticationDetailsSource.buildDetails(request));
    }
}
