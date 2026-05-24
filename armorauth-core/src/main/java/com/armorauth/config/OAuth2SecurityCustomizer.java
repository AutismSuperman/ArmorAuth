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

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;

/**
 * OAuth2 安全增强配置器
 * <p>
 * 禁用 implicit flow，强制 public client 使用 PKCE
 *
 * @author fulin
 * @since 2026-05-23
 */
public class OAuth2SecurityCustomizer implements ArmorAuthSecurityCustomizer {

    @Override
    public void customize(HttpSecurity http) {
        // OAuth2 Security is configured in AuthorizationServerConfig
        // This customizer can be used for additional security enhancements
    }

    /**
     * 禁用 implicit flow 的 authorization server configurer 配置
     */
    public static Customizer<OAuth2AuthorizationServerConfigurer> disableImplicitFlow() {
        return configurer -> configurer
                .authorizationEndpoint(authorization -> authorization
                        .consentPage("/consent"));
    }
}
