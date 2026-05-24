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
package com.armorauth.admin.config;

import com.armorauth.admin.service.AuditEventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Admin API安全配置
 * 使用securityMatchers将/api/admin/**请求路由到独立的FilterChain，
 * 配置HTTP Basic认证和角色权限
 *
 * @author fulin
 * @since 2026-05-23
 */
@EnableWebSecurity
@EnableMethodSecurity
@Configuration(proxyBeanMethods = false)
public class AdminSecurityConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 2)
    SecurityFilterChain adminSecurityFilterChain(HttpSecurity http,
                                                  UserDetailsService userDetailsService,
                                                  AccessDeniedHandler accessDeniedHandler) throws Exception {
        http.securityMatchers(securityMatchers -> securityMatchers
                        .requestMatchers("/api/admin/**", "/scim/v2/**"))
                .authorizeHttpRequests(authorize -> authorize
                        // SCIM directory provisioning - SUPER_ADMIN or USER_ADMIN
                        .requestMatchers("/scim/v2/**")
                            .hasAnyAuthority("ROLE_SUPER_ADMIN", "ROLE_USER_ADMIN")
                        // 审计日志 - SUPER_ADMIN 或 AUDIT_VIEWER
                        .requestMatchers(HttpMethod.GET, "/api/admin/v1/audit-events", "/api/admin/v1/audit-events/**")
                            .hasAnyAuthority("ROLE_SUPER_ADMIN", "ROLE_AUDIT_VIEWER")
                        // 用户管理 - 需要 SUPER_ADMIN 或 USER_ADMIN 角色
                        .requestMatchers(HttpMethod.GET, "/api/admin/v1/users", "/api/admin/v1/users/**")
                            .hasAnyAuthority("ROLE_SUPER_ADMIN", "ROLE_USER_ADMIN", "ROLE_AUDIT_VIEWER")
                        .requestMatchers("/api/admin/v1/users", "/api/admin/v1/users/**")
                            .hasAnyAuthority("ROLE_SUPER_ADMIN", "ROLE_USER_ADMIN")
                        // 角色管理 - 需要 SUPER_ADMIN 角色
                        .requestMatchers("/api/admin/v1/roles", "/api/admin/v1/roles/**",
                                "/api/admin/v1/role-bindings", "/api/admin/v1/role-bindings/**",
                                "/api/admin/v1/permissions", "/api/admin/v1/permissions/**",
                                "/api/admin/v1/authorization/check")
                            .hasAnyAuthority("ROLE_SUPER_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/admin/v1/identity-providers/*:sync-users")
                            .hasAnyAuthority("ROLE_SUPER_ADMIN", "ROLE_USER_ADMIN")
                        // 应用管理 - 需要 SUPER_ADMIN 或 APPLICATION_ADMIN 角色
                        .requestMatchers(HttpMethod.GET, "/api/admin/v1/applications", "/api/admin/v1/applications/**",
                                "/api/admin/v1/scopes", "/api/admin/v1/scopes/**",
                                "/api/admin/v1/login-policies", "/api/admin/v1/login-policies/**",
                                "/api/admin/v1/identity-providers", "/api/admin/v1/identity-providers/**")
                            .hasAnyAuthority("ROLE_SUPER_ADMIN", "ROLE_APPLICATION_ADMIN", "ROLE_AUDIT_VIEWER")
                        .requestMatchers("/api/admin/v1/applications", "/api/admin/v1/applications/**",
                                "/api/admin/v1/scopes", "/api/admin/v1/scopes/**",
                                "/api/admin/v1/login-policies", "/api/admin/v1/login-policies/**",
                                "/api/admin/v1/identity-providers", "/api/admin/v1/identity-providers/**")
                            .hasAnyAuthority("ROLE_SUPER_ADMIN", "ROLE_APPLICATION_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/admin/v1/token-statistics", "/api/admin/v1/token-statistics/**")
                            .hasAnyAuthority("ROLE_SUPER_ADMIN", "ROLE_APPLICATION_ADMIN", "ROLE_AUDIT_VIEWER")
                        .requestMatchers(HttpMethod.GET, "/api/admin/v1/federated-bindings", "/api/admin/v1/federated-bindings/**")
                            .hasAnyAuthority("ROLE_SUPER_ADMIN", "ROLE_APPLICATION_ADMIN", "ROLE_USER_ADMIN", "ROLE_AUDIT_VIEWER")
                        .requestMatchers("/api/admin/v1/federated-bindings", "/api/admin/v1/federated-bindings/**")
                            .hasAnyAuthority("ROLE_SUPER_ADMIN", "ROLE_USER_ADMIN")
                        // 其他管理接口 - 需要认证
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedHandler(accessDeniedHandler))
                .csrf(AbstractHttpConfigurer::disable)
                .userDetailsService(userDetailsService);
        return http.build();
    }

    @Bean
    AccessDeniedHandler accessDeniedHandler(AuditEventService auditEventService,
                                             ObjectMapper objectMapper) {
        return (HttpServletRequest request, HttpServletResponse response,
                org.springframework.security.access.AccessDeniedException accessDeniedException) -> {
            String principal = getPrincipalName();
            String ipAddress = getClientIp(request);
            auditEventService.record("ACCESS_DENIED", principal, "admin_api",
                    request.getRequestURI(), "权限不足", ipAddress);

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            if (request.getRequestURI().startsWith("/scim/v2/")) {
                response.setContentType("application/scim+json");
                response.setCharacterEncoding("UTF-8");
                objectMapper.writeValue(response.getWriter(), java.util.Map.of(
                        "schemas", java.util.List.of("urn:ietf:params:scim:api:messages:2.0:Error"),
                        "status", "403",
                        "detail", "Forbidden"));
                return;
            }

            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(),
                    java.util.Map.of("code", 403, "message", "权限不足"));
        };
    }

    private String getPrincipalName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "anonymous";
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty()) {
            return ip.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
