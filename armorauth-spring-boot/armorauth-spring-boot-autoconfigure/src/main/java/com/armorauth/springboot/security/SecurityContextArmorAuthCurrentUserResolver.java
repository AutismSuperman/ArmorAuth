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
package com.armorauth.springboot.security;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Default current-user resolver backed by Spring SecurityContext.
 *
 * @author fulin
 * @since 2026-06-21
 */
public class SecurityContextArmorAuthCurrentUserResolver implements ArmorAuthCurrentUserResolver {

    @Override
    public ArmorAuthCurrentUser currentUser() {
        return resolve(SecurityContextHolder.getContext().getAuthentication());
    }

    @Override
    public ArmorAuthCurrentUser resolve(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ArmorAuthCurrentUser.anonymous();
        }
        Map<String, Object> claims = claims(authentication);
        String subject = firstText(claims, "sub", authentication.getName());
        String username = firstText(claims, "preferred_username", "username", "name", authentication.getName());
        return new ArmorAuthCurrentUser(
                subject,
                username,
                firstText(claims, "tenant_id"),
                ArmorAuthClaimUtils.strings(claims, "org_ids"),
                ArmorAuthClaimUtils.strings(claims, "org_roles"),
                ArmorAuthClaimUtils.strings(claims, "roles"),
                scopes(claims),
                ArmorAuthClaimUtils.strings(claims, "permissions"),
                Map.copyOf(claims));
    }

    private Map<String, Object> claims(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            Jwt token = jwtAuthenticationToken.getToken();
            return new LinkedHashMap<>(token.getClaims());
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof OAuth2User oauth2User) {
            return new LinkedHashMap<>(oauth2User.getAttributes());
        }
        if (authentication instanceof OAuth2AuthenticationToken oauth2AuthenticationToken
                && oauth2AuthenticationToken.getPrincipal() != null) {
            return new LinkedHashMap<>(oauth2AuthenticationToken.getPrincipal().getAttributes());
        }
        return Map.of("sub", authentication.getName(), "name", authentication.getName());
    }

    private List<String> scopes(Map<String, Object> claims) {
        List<String> scopes = ArmorAuthClaimUtils.strings(claims, "scope");
        if (!scopes.isEmpty()) {
            return scopes;
        }
        return ArmorAuthClaimUtils.strings(claims, "scp");
    }

    private String firstText(Map<String, Object> claims, String... names) {
        for (String name : names) {
            Object value = claims.get(name);
            if (value != null && !String.valueOf(value).isBlank()) {
                return String.valueOf(value);
            }
        }
        return null;
    }
}
