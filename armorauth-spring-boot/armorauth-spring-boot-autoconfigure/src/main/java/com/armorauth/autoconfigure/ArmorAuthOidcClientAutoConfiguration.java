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
package com.armorauth.autoconfigure;

import com.armorauth.springboot.security.ArmorAuthOidcClientHttpSecurityCustomizer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

/**
 * ArmorAuth OIDC client auto-configuration for relying services.
 *
 * @author fulin
 * @since 2026-05-25
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({ClientRegistrationRepository.class, SecurityFilterChain.class})
@ConditionalOnProperty(prefix = "armorauth.oidc-client", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(ArmorAuthOidcClientProperties.class)
public class ArmorAuthOidcClientAutoConfiguration {

    @Bean(name = "oidcClientSecurityFilterChain")
    @ConditionalOnMissingBean(name = "oidcClientSecurityFilterChain")
    @Order(Ordered.LOWEST_PRECEDENCE)
    public SecurityFilterChain oidcClientSecurityFilterChain(
            HttpSecurity http,
            ArmorAuthOidcClientProperties properties,
            ObjectProvider<ArmorAuthOidcClientHttpSecurityCustomizer> customizers) throws Exception {
        String[] permitAll = properties.getPermitAll() == null ? new String[0] : properties.getPermitAll().toArray(String[]::new);
        http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(permitAll).permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(oauth2 -> {
                    if (properties.getDefaultSuccessUrl() != null && !properties.getDefaultSuccessUrl().isBlank()) {
                        oauth2.defaultSuccessUrl(properties.getDefaultSuccessUrl());
                    }
                })
                .logout(logout -> {
                    if (properties.getLogoutSuccessUrl() != null && !properties.getLogoutSuccessUrl().isBlank()) {
                        logout.logoutSuccessUrl(properties.getLogoutSuccessUrl());
                    }
                });
        if (!properties.isCsrfEnabled()) {
            http.csrf(AbstractHttpConfigurer::disable);
        }
        for (ArmorAuthOidcClientHttpSecurityCustomizer customizer : customizers.orderedStream().toList()) {
            customizer.customize(http);
        }
        return http.build();
    }
}
