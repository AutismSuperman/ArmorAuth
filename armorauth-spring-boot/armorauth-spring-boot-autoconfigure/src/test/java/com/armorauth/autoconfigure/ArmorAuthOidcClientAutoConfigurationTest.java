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

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;

import static org.assertj.core.api.Assertions.assertThat;

class ArmorAuthOidcClientAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ArmorAuthOidcClientAutoConfiguration.class));

    @Test
    void backsOffByDefault() {
        this.contextRunner
                .withUserConfiguration(CustomSecurityFilterChainConfiguration.class)
                .run(context -> assertThat(context).doesNotHaveBean("oidcClientSecurityFilterChain"));
    }

    @Test
    void backsOffFromCustomSecurityFilterChain() {
        this.contextRunner
                .withPropertyValues("armorauth.oidc-client.enabled=true")
                .withUserConfiguration(CustomSecurityFilterChainConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(SecurityFilterChain.class);
                    assertThat(context).doesNotHaveBean("oidcClientSecurityFilterChain");
                });
    }

    @Test
    void createsDefaultSecurityFilterChainWhenEnabled() {
        this.contextRunner
                .withPropertyValues("armorauth.oidc-client.enabled=true")
                .withUserConfiguration(ClientRegistrationRepositoryConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(SecurityFilterChain.class);
                    assertThat(context).hasBean("oidcClientSecurityFilterChain");
                });
    }

    @Test
    void backsOffWhenClientRegistrationRepositoryClassIsMissing() {
        this.contextRunner
                .withPropertyValues("armorauth.oidc-client.enabled=true")
                .withClassLoader(new FilteredClassLoader(ClientRegistrationRepository.class))
                .withUserConfiguration(CustomSecurityFilterChainConfiguration.class)
                .run(context -> assertThat(context).doesNotHaveBean("oidcClientSecurityFilterChain"));
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomSecurityFilterChainConfiguration {

        @Bean
        SecurityFilterChain customSecurityFilterChain() {
            return new DefaultSecurityFilterChain(AnyRequestMatcher.INSTANCE);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebSecurity
    static class ClientRegistrationRepositoryConfiguration {

        @Bean
        ClientRegistrationRepository clientRegistrationRepository() {
            ClientRegistration registration = ClientRegistration.withRegistrationId("armorauth")
                    .clientId("client")
                    .clientSecret("secret")
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                    .authorizationUri("http://localhost:9000/oauth2/authorize")
                    .tokenUri("http://localhost:9000/oauth2/token")
                    .userInfoUri("http://localhost:9000/userinfo")
                    .userNameAttributeName("sub")
                    .scope("openid", "profile")
                    .clientName("ArmorAuth")
                    .build();
            return new InMemoryClientRegistrationRepository(registration);
        }
    }
}
