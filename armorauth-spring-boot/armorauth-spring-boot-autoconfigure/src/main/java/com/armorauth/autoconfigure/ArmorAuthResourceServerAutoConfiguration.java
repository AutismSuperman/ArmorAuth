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

import com.armorauth.springboot.security.ArmorAuthJwtAuthenticationConverterCustomizer;
import com.armorauth.springboot.security.ArmorAuthJwtGrantedAuthoritiesConverter;
import com.armorauth.springboot.security.ArmorAuthResourceServerHttpSecurityCustomizer;
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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * ArmorAuth resource server auto-configuration for relying services.
 *
 * @author fulin
 * @since 2026-05-25
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({Jwt.class, JwtAuthenticationConverter.class, SecurityFilterChain.class})
@ConditionalOnProperty(prefix = "armorauth.resource-server", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(ArmorAuthResourceServerProperties.class)
public class ArmorAuthResourceServerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationConverter armorAuthJwtAuthenticationConverter(
            ArmorAuthResourceServerProperties properties,
            ObjectProvider<ArmorAuthJwtAuthenticationConverterCustomizer> customizers) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new ArmorAuthJwtGrantedAuthoritiesConverter(properties));
        converter.setPrincipalClaimName(properties.getPrincipalClaim());
        customizers.orderedStream().forEach(customizer -> customizer.customize(converter));
        return converter;
    }

    @Bean(name = "resourceServerSecurityFilterChain")
    @ConditionalOnMissingBean(name = "resourceServerSecurityFilterChain")
    @Order(Ordered.HIGHEST_PRECEDENCE + 20)
    public SecurityFilterChain resourceServerSecurityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            ArmorAuthResourceServerProperties properties,
            ObjectProvider<ArmorAuthResourceServerHttpSecurityCustomizer> customizers) throws Exception {
        String[] permitAll = properties.getPermitAll() == null ? new String[0] : properties.getPermitAll().toArray(String[]::new);
        http.securityMatcher(properties.getSecurityMatcher())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(permitAll).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
        if (!properties.isCsrfEnabled()) {
            http.csrf(AbstractHttpConfigurer::disable);
        }
        for (ArmorAuthResourceServerHttpSecurityCustomizer customizer : customizers.orderedStream().toList()) {
            customizer.customize(http);
        }
        return http.build();
    }

}
