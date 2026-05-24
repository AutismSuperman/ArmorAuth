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
package com.armorauth.federation.security;

import com.armorauth.federation.FederatedLoginOrchestrator;
import com.armorauth.federation.FederatedUserProfile;
import com.armorauth.security.SecurityAuditUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FederatedSamlAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(FederatedSamlAuthenticationSuccessHandler.class);

    private final SavedRequestAwareAuthenticationSuccessHandler delegate =
            new SavedRequestAwareAuthenticationSuccessHandler();

    private final ObjectMapper objectMapper;

    private final FederatedLoginOrchestrator federatedLoginOrchestrator;

    public FederatedSamlAuthenticationSuccessHandler(RequestCache requestCache,
                                                     ObjectMapper objectMapper,
                                                     FederatedLoginOrchestrator federatedLoginOrchestrator) {
        this.delegate.setDefaultTargetUrl("/");
        this.delegate.setRequestCache(requestCache);
        this.objectMapper = objectMapper;
        this.federatedLoginOrchestrator = federatedLoginOrchestrator;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {
        Object principal = authentication.getPrincipal();
        log.info("SAML login succeeded username={} remoteAddress={} uri={} principalType={}",
                SecurityAuditUtils.getAuthenticationName(authentication),
                SecurityAuditUtils.getRemoteAddress(request),
                request.getRequestURI(),
                principal != null ? principal.getClass().getSimpleName() : "unknown");

        if (authentication instanceof Saml2Authentication
                && principal instanceof Saml2AuthenticatedPrincipal samlPrincipal) {
            FederatedUserProfile profile = extractProfile(samlPrincipal);
            if (this.federatedLoginOrchestrator.handleProfile(request, response, profile)) {
                return;
            }
        }
        this.delegate.onAuthenticationSuccess(request, response, authentication);
    }

    private FederatedUserProfile extractProfile(Saml2AuthenticatedPrincipal principal) {
        Map<String, Object> attributes = normalizeAttributes(principal.getAttributes());
        String registrationId = principal.getRelyingPartyRegistrationId();
        String providerUserId = principal.getName();
        String displayName = firstNonBlank(
                attributes, "displayName", "cn", "name", "givenName", "mail", "email", "uid");
        if (!StringUtils.hasText(displayName)) {
            displayName = registrationId + "_" + abbreviate(providerUserId);
        }
        String providerUsername = firstNonBlank(
                attributes, "uid", "username", "preferred_username", "userPrincipalName", "mail", "email", "cn");
        if (!StringUtils.hasText(providerUsername)) {
            providerUsername = providerUserId;
        }
        return new FederatedUserProfile(
                registrationId,
                providerUserId,
                providerUsername,
                displayName,
                null,
                serializeAttributes(attributes)
        );
    }

    private Map<String, Object> normalizeAttributes(Map<String, List<Object>> attributes) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        attributes.forEach((name, values) -> {
            if (values == null || values.isEmpty()) {
                normalized.put(name, null);
            } else if (values.size() == 1) {
                normalized.put(name, values.getFirst());
            } else {
                normalized.put(name, values);
            }
        });
        return normalized;
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
            log.warn("Failed to serialize SAML attributes", ex);
            return "{}";
        }
    }

    private String abbreviate(String value) {
        String sanitized = value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "");
        if (!StringUtils.hasText(sanitized)) {
            sanitized = "user";
        }
        return sanitized.substring(0, Math.min(8, sanitized.length()));
    }
}
