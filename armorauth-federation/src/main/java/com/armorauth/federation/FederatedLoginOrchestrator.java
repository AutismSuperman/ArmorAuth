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
package com.armorauth.federation;

import com.armorauth.common.audit.UserRegistrationEvent;
import com.armorauth.data.entity.UserFederatedBinding;
import com.armorauth.data.entity.UserInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Service
public class FederatedLoginOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(FederatedLoginOrchestrator.class);

    private final ObjectMapper objectMapper;

    private final FederatedAccountService federatedAccountService;

    private final UserFederatedBindingService userFederatedBindingService;

    private final FederatedSessionContextRepository federatedSessionContextRepository;

    private final FederatedLoginCompletionService federatedLoginCompletionService;

    private final ApplicationEventPublisher eventPublisher;

    private final IdpAttributeMappingService idpAttributeMappingService;

    public FederatedLoginOrchestrator(ObjectMapper objectMapper,
                                      FederatedAccountService federatedAccountService,
                                      UserFederatedBindingService userFederatedBindingService,
                                      FederatedSessionContextRepository federatedSessionContextRepository,
                                      FederatedLoginCompletionService federatedLoginCompletionService,
                                      ApplicationEventPublisher eventPublisher,
                                      IdpAttributeMappingService idpAttributeMappingService) {
        this.objectMapper = objectMapper;
        this.federatedAccountService = federatedAccountService;
        this.userFederatedBindingService = userFederatedBindingService;
        this.federatedSessionContextRepository = federatedSessionContextRepository;
        this.federatedLoginCompletionService = federatedLoginCompletionService;
        this.eventPublisher = eventPublisher;
        this.idpAttributeMappingService = idpAttributeMappingService;
    }

    public boolean handleSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        if (!(authentication instanceof OAuth2AuthenticationToken oauth2AuthenticationToken)) {
            return false;
        }
        FederatedUserProfile federatedUserProfile = extractProfile(oauth2AuthenticationToken);
        return handleProfile(request, response, federatedUserProfile);
    }

    public boolean handleProfile(HttpServletRequest request,
                                 HttpServletResponse response,
                                 FederatedUserProfile federatedUserProfile)
            throws IOException, ServletException {
        java.time.Instant now = this.federatedAccountService.currentTimestamp();
        log.info(
                "Handling federated login success registrationId={} providerUserId={}",
                federatedUserProfile.registrationId(),
                abbreviateProviderUserId(federatedUserProfile.providerUserId())
        );

        Optional<UserFederatedBinding> existingBinding = this.userFederatedBindingService.findBinding(
                federatedUserProfile.registrationId(),
                federatedUserProfile.providerUserId()
        );
        if (existingBinding.isPresent()) {
            try {
                UserFederatedBinding binding = existingBinding.get();
                log.info(
                        "Found existing federated binding registrationId={} userId={}",
                        federatedUserProfile.registrationId(),
                        binding.getUserId()
                );
                UserInfo userInfo = this.federatedAccountService.getRequiredUser(binding.getUserId());
                this.userFederatedBindingService.touchLastLogin(binding, now);
                this.federatedSessionContextRepository.clearAll(request);
                this.federatedLoginCompletionService.complete(request, response, userInfo);
                return true;
            } catch (IllegalStateException ex) {
                log.warn(
                        "Failed to complete federated login with existing binding registrationId={} reason={}",
                        federatedUserProfile.registrationId(),
                        ex.getMessage()
                );
                return fail(request, response, ex.getMessage());
            }
        }

        FederatedAuthorizationContext authorizationContext = this.federatedSessionContextRepository
                .loadAuthorizationContext(request)
                .orElse(null);
        if (authorizationContext == null) {
            log.warn(
                    "Missing federated authorization context registrationId={} providerUserId={}",
                    federatedUserProfile.registrationId(),
                    abbreviateProviderUserId(federatedUserProfile.providerUserId())
            );
            return fail(request, response, "联合登录上下文已失效，请重新发起授权。");
        }
        if (!federatedUserProfile.registrationId().equals(authorizationContext.registrationId())) {
            log.warn(
                    "Federated authorization context mismatch expected={} actual={}",
                    authorizationContext.registrationId(),
                    federatedUserProfile.registrationId()
            );
            return fail(request, response, "联合登录上下文与当前第三方提供商不匹配。");
        }
        this.federatedSessionContextRepository.clearAuthorizationContext(request);
        log.info(
                "Loaded federated authorization context registrationId={} mode={}",
                authorizationContext.registrationId(),
                authorizationContext.mode()
        );

        if (authorizationContext.mode() == FederatedLoginMode.AUTO) {
            try {
                log.info("Auto register flow started for registrationId={}", federatedUserProfile.registrationId());
                UserInfo userInfo = this.federatedAccountService.createAutoRegisteredUser(federatedUserProfile, now);
                this.userFederatedBindingService.createOrUpdateBinding(userInfo, federatedUserProfile, now);
                this.federatedSessionContextRepository.clearPendingContext(request);
                // 应用身份源属性映射（组织和角色）
                try {
                    idpAttributeMappingService.applyAttributeMapping(
                            userInfo.getId(), federatedUserProfile.registrationId(),
                            federatedUserProfile.providerAttributes());
                } catch (Exception e) {
                    log.warn("IdP attribute mapping failed for userId={}: {}", userInfo.getId(), e.getMessage());
                }
                eventPublisher.publishEvent(new UserRegistrationEvent(
                        this, userInfo.getId(), userInfo.getUsername(), "federated"));
                this.federatedLoginCompletionService.complete(request, response, userInfo);
                return true;
            } catch (IllegalArgumentException | IllegalStateException ex) {
                log.warn(
                        "Auto register flow failed for registrationId={} reason={}",
                        federatedUserProfile.registrationId(),
                        ex.getMessage()
                );
                return fail(request, response, ex.getMessage());
            }
        }

        String suggestedUsername = this.federatedAccountService.generateAvailableUsername(
                federatedUserProfile.registrationId(),
                federatedUserProfile.providerUserId(),
                federatedUserProfile.displayName()
        );
        this.federatedSessionContextRepository.savePendingContext(
                request,
                federatedUserProfile.toPendingContext(suggestedUsername)
        );
        log.info(
                "Stored pending federated binding context registrationId={} suggestedUsername={}",
                federatedUserProfile.registrationId(),
                suggestedUsername
        );
        this.federatedLoginCompletionService.clearAuthentication(request, response);
        response.sendRedirect(request.getContextPath() + "/federated/confirm");
        return true;
    }

    private boolean fail(HttpServletRequest request, HttpServletResponse response, String message) throws IOException {
        log.warn("Federated login flow failed: {}", message);
        this.federatedSessionContextRepository.clearAll(request);
        this.federatedLoginCompletionService.clearAuthentication(request, response);
        request.getSession(true)
                .setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, new BadCredentialsException(message));
        response.sendRedirect(request.getContextPath() + "/login?error");
        return true;
    }

    private FederatedUserProfile extractProfile(OAuth2AuthenticationToken authenticationToken) {
        OAuth2User principal = authenticationToken.getPrincipal();
        Map<String, Object> attributes = principal.getAttributes();
        String registrationId = authenticationToken.getAuthorizedClientRegistrationId();
        String providerUserId = principal.getName();
        String displayName = firstNonBlank(attributes, "nickname", "name", "screen_name", "uname", "nick_name", "nick");
        if (!StringUtils.hasText(displayName)) {
            displayName = registrationId + "_" + abbreviateProviderUserId(providerUserId);
        }
        String providerUsername = firstNonBlank(
                attributes,
                "login",
                "username",
                "preferred_username",
                "nickname",
                "name",
                "screen_name",
                "uname",
                "nick_name",
                "nick",
                "open_id",
                "openId",
                "UserId",
                "OpenId"
        );
        String avatarUrl = firstNonBlank(
                attributes,
                "avatar_url",
                "picture",
                "avatar",
                "avatar_large",
                "profile_image_url",
                "portrait",
                "headimgurl",
                "figureurl_qq_2",
                "figureurl_2",
                "figureurl"
        );
        return new FederatedUserProfile(
                registrationId,
                providerUserId,
                providerUsername,
                displayName,
                avatarUrl,
                serializeAttributes(attributes)
        );
    }

    private String firstNonBlank(Map<String, Object> attributes, String... keys) {
        for (String key : keys) {
            Object value = attributes.get(key);
            if (value instanceof String text && StringUtils.hasText(text)) {
                return text.trim();
            }
        }
        return null;
    }

    private String serializeAttributes(Map<String, Object> attributes) {
        try {
            return this.objectMapper.writeValueAsString(attributes);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize federated provider attributes", ex);
            return "{}";
        }
    }

    private String abbreviateProviderUserId(String providerUserId) {
        String sanitized = providerUserId == null ? "" : providerUserId.replaceAll("[^A-Za-z0-9]", "");
        if (!StringUtils.hasText(sanitized)) {
            sanitized = "user";
        }
        return sanitized.substring(0, Math.min(8, sanitized.length()));
    }
}
