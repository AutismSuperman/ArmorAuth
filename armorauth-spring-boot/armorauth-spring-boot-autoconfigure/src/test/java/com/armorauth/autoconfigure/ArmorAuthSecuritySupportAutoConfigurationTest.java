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

import com.armorauth.springboot.security.ArmorAuthCurrentUser;
import com.armorauth.springboot.security.ArmorAuthCurrentUserResolver;
import com.armorauth.springboot.security.ArmorAuthTokenRelayInterceptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;

class ArmorAuthSecuritySupportAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ArmorAuthSecuritySupportAutoConfiguration.class));

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsSecuritySupportBeans() {
        this.contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ArmorAuthCurrentUserResolver.class);
            assertThat(context).hasSingleBean(ArmorAuthTokenRelayInterceptor.class);
        });
    }

    @Test
    void resolvesCurrentUserFromJwtAuthentication() {
        this.contextRunner.run(context -> {
            Jwt jwt = Jwt.withTokenValue("token")
                    .header("alg", "none")
                    .claim("sub", "user-1")
                    .claim("preferred_username", "alice")
                    .claim("tenant_id", "tenant-demo")
                    .claim("org_ids", List.of("org-a", "org-b"))
                    .claim("org_roles", "owner admin")
                    .claim("roles", List.of("USER", "ADMIN"))
                    .claim("scope", "openid profile")
                    .claim("permissions", "message:read,message:write")
                    .build();
            SecurityContextHolder.getContext()
                    .setAuthentication(new JwtAuthenticationToken(jwt, AuthorityUtils.NO_AUTHORITIES, "user-1"));

            ArmorAuthCurrentUser user = context.getBean(ArmorAuthCurrentUserResolver.class).currentUser();

            assertThat(user.subject()).isEqualTo("user-1");
            assertThat(user.username()).isEqualTo("alice");
            assertThat(user.tenantId()).isEqualTo("tenant-demo");
            assertThat(user.organizationIds()).containsExactly("org-a", "org-b");
            assertThat(user.organizationRoles()).containsExactly("owner", "admin");
            assertThat(user.roles()).containsExactly("USER", "ADMIN");
            assertThat(user.scopes()).containsExactly("openid", "profile");
            assertThat(user.permissions()).containsExactly("message:read", "message:write");
        });
    }
}
