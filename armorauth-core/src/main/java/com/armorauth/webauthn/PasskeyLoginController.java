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
package com.armorauth.webauthn;

import com.armorauth.authentication.MfaAuthenticationSuccessHandler;
import com.armorauth.common.exception.BusinessException;
import com.armorauth.common.response.ApiResponse;
import com.armorauth.security.AuditEventPublisher;
import com.armorauth.security.LoginLockoutService;
import com.armorauth.security.SecurityAuditUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PasskeyLoginController {

    private final WebAuthnAssertionService webAuthnAssertionService;
    private final SecurityContextRepository securityContextRepository;
    private final RequestCache requestCache;
    private final AuditEventPublisher auditEventPublisher;
    private final LoginLockoutService loginLockoutService;
    private final UserDetailsService userDetailsService;

    public PasskeyLoginController(WebAuthnAssertionService webAuthnAssertionService,
                                  SecurityContextRepository securityContextRepository,
                                  RequestCache requestCache,
                                  AuditEventPublisher auditEventPublisher,
                                  LoginLockoutService loginLockoutService,
                                  UserDetailsService userDetailsService) {
        this.webAuthnAssertionService = webAuthnAssertionService;
        this.securityContextRepository = securityContextRepository;
        this.requestCache = requestCache;
        this.auditEventPublisher = auditEventPublisher;
        this.loginLockoutService = loginLockoutService;
        this.userDetailsService = userDetailsService;
    }

    @PostMapping("/login/passkey/options")
    public ResponseEntity<ApiResponse<WebAuthnAssertionDTO.AssertionOptionsResponse>> beginPasswordlessAssertion(
            @RequestBody(required = false) WebAuthnAssertionDTO.PasswordlessOptionsRequest optionsRequest,
            HttpServletRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(
                    webAuthnAssertionService.beginPasswordlessAssertion(optionsRequest, request)));
        } catch (BusinessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getCode(), ex.getMessage()));
        }
    }

    @PostMapping("/login/passkey/finish")
    public ResponseEntity<ApiResponse<WebAuthnAssertionDTO.AssertionFinishResponse>> finishPasswordlessAssertion(
            @RequestBody WebAuthnAssertionDTO.AssertionFinishRequest assertionRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        String username = null;
        try {
            WebAuthnAssertionDTO.VerifiedPasswordlessAssertion verified =
                    webAuthnAssertionService.finishPasswordlessAssertion(assertionRequest, request);
            username = verified.username();
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                    userDetails, null, userDetails.getAuthorities());
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.getContextHolderStrategy().setContext(context);
            securityContextRepository.saveContext(context, request, response);
            String redirectUrl = resolveRedirectUrl(request, response);
            loginLockoutService.recordSuccess(username);
            auditEventPublisher.publishLoginSuccess(username, SecurityAuditUtils.getRemoteAddress(request),
                    "Passkey passwordless login success, factorId=" + verified.factorId());
            return ResponseEntity.ok(ApiResponse.ok(new WebAuthnAssertionDTO.AssertionFinishResponse(
                    true, redirectUrl, verified.factorId(), "passkey_passwordless_ready")));
        } catch (BusinessException ex) {
            webAuthnAssertionService.clearPasswordlessSessionChallenge(request.getSession(false));
            if (username != null) {
                auditEventPublisher.publishLoginFailure(username, SecurityAuditUtils.getRemoteAddress(request),
                        "Passkey passwordless login failure: " + ex.getMessage());
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getCode(), ex.getMessage()));
        }
    }

    @PostMapping("/login/passkey/assertion/options")
    public ResponseEntity<ApiResponse<WebAuthnAssertionDTO.AssertionOptionsResponse>> beginAssertion(
            HttpServletRequest request) {
        try {
            String username = pendingPrincipal(request);
            return ResponseEntity.ok(ApiResponse.ok(webAuthnAssertionService.beginAssertion(username, request)));
        } catch (BusinessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getCode(), ex.getMessage()));
        }
    }

    @PostMapping("/login/passkey/assertion/finish")
    public ResponseEntity<ApiResponse<WebAuthnAssertionDTO.AssertionFinishResponse>> finishAssertion(
            @RequestBody WebAuthnAssertionDTO.AssertionFinishRequest assertionRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        String username = null;
        try {
            username = pendingPrincipal(request);
            WebAuthnAssertionDTO.VerifiedAssertion verified =
                    webAuthnAssertionService.finishAssertion(username, assertionRequest, request);
            Authentication authentication = pendingAuthentication(request);
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.getContextHolderStrategy().setContext(context);
            securityContextRepository.saveContext(context, request, response);
            clearPendingMfa(request.getSession(false));
            String redirectUrl = resolveRedirectUrl(request, response);
            loginLockoutService.recordSuccess(username);
            auditEventPublisher.publishLoginSuccess(username, SecurityAuditUtils.getRemoteAddress(request),
                    "认证成功: " + username);
            auditEventPublisher.publishMfaSuccess(username, SecurityAuditUtils.getRemoteAddress(request),
                    "Passkey assertion success, factorId=" + verified.factorId());
            return ResponseEntity.ok(ApiResponse.ok(new WebAuthnAssertionDTO.AssertionFinishResponse(
                    true, redirectUrl, verified.factorId(), "passkey_assertion_ready")));
        } catch (BusinessException ex) {
            webAuthnAssertionService.clearSessionChallenge(request.getSession(false));
            if (username != null) {
                auditEventPublisher.publishMfaFailure(username, SecurityAuditUtils.getRemoteAddress(request),
                        "Passkey assertion failure: " + ex.getMessage());
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getCode(), ex.getMessage()));
        }
    }

    private String pendingPrincipal(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new BusinessException("没有待验证登录会话");
        }
        Object principal = session.getAttribute(MfaAuthenticationSuccessHandler.PENDING_MFA_PRINCIPAL);
        if (!(principal instanceof String username) || username.isBlank()) {
            throw new BusinessException("没有待验证登录用户");
        }
        return username;
    }

    private Authentication pendingAuthentication(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new BusinessException("没有待验证登录会话");
        }
        Object authentication = session.getAttribute(MfaAuthenticationSuccessHandler.PENDING_MFA_AUTHENTICATION);
        if (!(authentication instanceof Authentication pendingAuthentication)) {
            throw new BusinessException("待验证登录会话已失效");
        }
        return pendingAuthentication;
    }

    private void clearPendingMfa(HttpSession session) {
        if (session == null) {
            return;
        }
        session.removeAttribute(MfaAuthenticationSuccessHandler.PENDING_MFA_AUTHENTICATION);
        session.removeAttribute(MfaAuthenticationSuccessHandler.PENDING_MFA_PRINCIPAL);
        session.removeAttribute(MfaAuthenticationSuccessHandler.MFA_BINDING_REQUIRED);
    }

    private String resolveRedirectUrl(HttpServletRequest request, HttpServletResponse response) {
        SavedRequest savedRequest = requestCache.getRequest(request, response);
        String fallback = request.getContextPath().isBlank() ? "/" : request.getContextPath() + "/";
        if (savedRequest == null) {
            return fallback;
        }
        String redirectUrl = savedRequest.getRedirectUrl();
        requestCache.removeRequest(request, response);
        String expectedOrigin = webAuthnAssertionService.resolveOrigin(request);
        if (redirectUrl == null || redirectUrl.isBlank()) {
            return fallback;
        }
        String contextPrefix = request.getContextPath().isBlank() ? "/" : request.getContextPath() + "/";
        if (redirectUrl.startsWith(expectedOrigin)
                || (redirectUrl.startsWith(contextPrefix) && !redirectUrl.startsWith("//"))) {
            return redirectUrl;
        }
        return fallback;
    }
}
