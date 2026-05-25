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

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;

import static org.assertj.core.api.Assertions.assertThat;

class ArmorAuthResourceServerAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ArmorAuthResourceServerAutoConfiguration.class));

    @Test
    void backsOffByDefault() {
        this.contextRunner
                .withUserConfiguration(CustomSecurityFilterChainConfiguration.class)
                .run(context -> assertThat(context).doesNotHaveBean(JwtAuthenticationConverter.class));
    }

    @Test
    void createsJwtAuthenticationConverterWhenEnabled() {
        this.contextRunner
                .withPropertyValues("armorauth.resource-server.enabled=true")
                .withUserConfiguration(CustomSecurityFilterChainConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(JwtAuthenticationConverter.class);

                    Jwt jwt = Jwt.withTokenValue("token")
                            .header("alg", "none")
                            .claim("roles", List.of("admin", "auditor"))
                            .claim("scope", "openid profile")
                            .build();

                    JwtAuthenticationConverter converter = context.getBean(JwtAuthenticationConverter.class);
                    assertThat(converter.convert(jwt).getAuthorities())
                            .extracting("authority")
                            .contains("ROLE_admin", "ROLE_auditor", "SCOPE_openid", "SCOPE_profile");
                });
    }

    @Test
    void createsDefaultSecurityFilterChainWhenEnabled() {
        this.contextRunner
                .withPropertyValues("armorauth.resource-server.enabled=true")
                .withUserConfiguration(JwtDecoderConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(SecurityFilterChain.class);
                    assertThat(context).hasBean("resourceServerSecurityFilterChain");
                });
    }

    @Test
    void backsOffFromCustomJwtAuthenticationConverter() {
        this.contextRunner
                .withPropertyValues("armorauth.resource-server.enabled=true")
                .withUserConfiguration(CustomJwtAuthenticationConverterConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(JwtAuthenticationConverter.class);
                    assertThat(context.getBean(JwtAuthenticationConverter.class))
                            .isSameAs(context.getBean("customJwtAuthenticationConverter"));
                });
    }

    @Test
    void backsOffFromCustomSecurityFilterChain() {
        this.contextRunner
                .withPropertyValues("armorauth.resource-server.enabled=true")
                .withUserConfiguration(CustomSecurityFilterChainConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(SecurityFilterChain.class);
                    assertThat(context).doesNotHaveBean("resourceServerSecurityFilterChain");
                });
    }

    @Test
    void backsOffWhenJwtClassIsMissing() {
        this.contextRunner
                .withPropertyValues("armorauth.resource-server.enabled=true")
                .withClassLoader(new FilteredClassLoader(Jwt.class))
                .withUserConfiguration(CustomSecurityFilterChainConfiguration.class)
                .run(context -> assertThat(context).doesNotHaveBean(JwtAuthenticationConverter.class));
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomSecurityFilterChainConfiguration {

        @Bean
        SecurityFilterChain customSecurityFilterChain() {
            return new DefaultSecurityFilterChain(AnyRequestMatcher.INSTANCE);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomJwtAuthenticationConverterConfiguration {

        @Bean
        SecurityFilterChain customSecurityFilterChain() {
            return new DefaultSecurityFilterChain(AnyRequestMatcher.INSTANCE);
        }

        @Bean
        JwtAuthenticationConverter customJwtAuthenticationConverter() {
            return new JwtAuthenticationConverter();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebSecurity
    static class JwtDecoderConfiguration {

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .claim("sub", "subject")
                    .build();
        }
    }
}
