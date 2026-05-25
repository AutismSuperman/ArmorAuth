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

import com.armorauth.data.repository.AuthFactorRepository;
import com.armorauth.data.repository.UserInfoRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

/**
 * MFA 感知的认证成功处理器
 * <p>
 * 综合判断是否需要 MFA：
 * 1. 用户主动绑定了 MFA 因子
 * 2. 应用级别要求 MFA（OAuth2Client.mfaRequired）
 * 3. 用户角色要求 MFA（SUPER_ADMIN / TENANT_ADMIN）
 *
 * @author fulin
 * @since 2026-05-23
 */
public class MfaAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    public static final String PENDING_MFA_AUTHENTICATION = "PENDING_MFA_AUTHENTICATION";
    public static final String PENDING_MFA_PRINCIPAL = "PENDING_MFA_PRINCIPAL";
    public static final String MFA_BINDING_REQUIRED = "MFA_BINDING_REQUIRED";

    private static final Set<String> RUNTIME_MFA_FACTOR_TYPES = Set.of("TOTP", "WEBAUTHN");

    private final AuthFactorRepository authFactorRepository;
    private final AuthenticationSuccessHandler delegate;
    private final MfaPolicyService mfaPolicyService;
    private final UserInfoRepository userInfoRepository;
    private final RequestCache requestCache;

    public MfaAuthenticationSuccessHandler(AuthFactorRepository authFactorRepository,
                                           AuthenticationSuccessHandler delegate) {
        this(authFactorRepository, delegate, null, null);
    }

    public MfaAuthenticationSuccessHandler(AuthFactorRepository authFactorRepository,
                                           AuthenticationSuccessHandler delegate,
                                           MfaPolicyService mfaPolicyService) {
        this(authFactorRepository, delegate, mfaPolicyService, null);
    }

    public MfaAuthenticationSuccessHandler(AuthFactorRepository authFactorRepository,
                                           AuthenticationSuccessHandler delegate,
                                           MfaPolicyService mfaPolicyService,
                                           UserInfoRepository userInfoRepository) {
        this(authFactorRepository, delegate, mfaPolicyService, userInfoRepository, null);
    }

    public MfaAuthenticationSuccessHandler(AuthFactorRepository authFactorRepository,
                                           AuthenticationSuccessHandler delegate,
                                           MfaPolicyService mfaPolicyService,
                                           UserInfoRepository userInfoRepository,
                                           RequestCache requestCache) {
        this.authFactorRepository = authFactorRepository;
        this.delegate = delegate;
        this.mfaPolicyService = mfaPolicyService;
        this.userInfoRepository = userInfoRepository;
        this.requestCache = requestCache;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String username = authentication.getName();
        Optional<String> userId = resolveUserId(username);

        // 检查用户是否配置了已验证且启用的 MFA 因子
        boolean hasUserMfa = userId
                .map(id -> authFactorRepository.findByUserIdAndEnabledTrue(id).stream()
                        .anyMatch(f -> Boolean.TRUE.equals(f.getVerified())
                                && RUNTIME_MFA_FACTOR_TYPES.contains(f.getFactorType())))
                .orElse(false);

        // 检查策略级 MFA 要求（应用级别或角色级别）
        boolean policyRequiresMfa = false;
        if (mfaPolicyService != null) {
            String clientId = resolveClientId(request, response);
            policyRequiresMfa = userId
                    .map(id -> mfaPolicyService.requiresMfa(id, clientId))
                    .orElse(false);
        }

        if (hasUserMfa || policyRequiresMfa) {
            // 如果策略要求 MFA 但用户没有绑定因子，需要先引导绑定
            if (policyRequiresMfa && !hasUserMfa) {
                // 用户没有 MFA 因子但策略要求 MFA，跳转到 MFA 绑定引导页
                request.getSession(true).setAttribute(PENDING_MFA_AUTHENTICATION, authentication);
                request.getSession(true).setAttribute(PENDING_MFA_PRINCIPAL, username);
                request.getSession(true).setAttribute(MFA_BINDING_REQUIRED, true);
                response.sendRedirect(request.getContextPath() + "/login/mfa");
                return;
            }
            // 将认证信息存入 session，MFA 验证完成后恢复
            request.getSession(true).setAttribute(PENDING_MFA_AUTHENTICATION, authentication);
            request.getSession(true).setAttribute(PENDING_MFA_PRINCIPAL, username);
            response.sendRedirect(request.getContextPath() + "/login/mfa");
        } else {
            delegate.onAuthenticationSuccess(request, response, authentication);
        }
    }

    private Optional<String> resolveUserId(String username) {
        if (userInfoRepository == null) {
            return Optional.of(username);
        }
        return userInfoRepository.findByUsername(username)
                .map(user -> Optional.of(user.getId()))
                .orElseGet(() -> Optional.of(username));
    }

    private String resolveClientId(HttpServletRequest request, HttpServletResponse response) {
        String clientId = request.getParameter("client_id");
        if (StringUtils.hasText(clientId)) {
            return clientId;
        }
        if (requestCache == null) {
            return null;
        }
        SavedRequest savedRequest = requestCache.getRequest(request, response);
        if (savedRequest == null || !StringUtils.hasText(savedRequest.getRedirectUrl())) {
            return null;
        }
        try {
            return UriComponentsBuilder.fromUriString(savedRequest.getRedirectUrl())
                    .build()
                    .getQueryParams()
                    .getFirst("client_id");
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
