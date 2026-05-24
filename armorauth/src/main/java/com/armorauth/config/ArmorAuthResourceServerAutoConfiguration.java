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
package com.armorauth.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ArmorAuth Resource Server 自动配置
 * <p>
 * 为接入 ArmorAuth 的资源服务器提供默认安全配置，包括：
 * - JWT 解析和角色映射
 * - 默认的 SecurityFilterChain（保护 /api/** 端点）
 * - 从 ID Token 的 roles claim 提取权限
 *
 * @author fulin
 * @since 2026-05-23
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Jwt.class)
@ConditionalOnProperty(prefix = "armorauth.resource-server", name = "enabled", havingValue = "true", matchIfMissing = false)
public class ArmorAuthResourceServerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationConverter armorAuthJwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new RolesClaimJwtGrantedAuthoritiesConverter());
        return converter;
    }

    @Bean
    @ConditionalOnMissingBean(name = "resourceServerSecurityFilterChain")
    public SecurityFilterChain resourceServerSecurityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http.securityMatcher("/api/**")
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/public/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    /**
     * 从 JWT 的 roles claim 提取权限
     */
    public static class RolesClaimJwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            Collection<GrantedAuthority> authorities = new ArrayList<>();

            Object rolesClaim = jwt.getClaim("roles");
            if (rolesClaim instanceof List<?> roles) {
                for (Object role : roles) {
                    if (role instanceof String roleStr) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + roleStr));
                    }
                }
            }

            String scope = jwt.getClaimAsString("scope");
            if (scope != null) {
                authorities.addAll(
                        Arrays.stream(scope.split(" "))
                                .map(s -> new SimpleGrantedAuthority("SCOPE_" + s))
                                .collect(Collectors.toList())
                );
            }

            return authorities;
        }
    }
}
