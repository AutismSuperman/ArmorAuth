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

import com.armorauth.springboot.client.ArmorAuthAdminRestClient;
import com.armorauth.springboot.client.ArmorAuthAdminRestClientCustomizer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * Auto-configuration for the lightweight ArmorAuth Admin API client.
 *
 * @author fulin
 * @since 2026-06-21
 */
@AutoConfiguration
@ConditionalOnClass(RestClient.class)
@ConditionalOnProperty(prefix = "armorauth.admin-client", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(ArmorAuthAdminClientProperties.class)
public class ArmorAuthAdminClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ArmorAuthAdminRestClient armorAuthAdminRestClient(
            ArmorAuthAdminClientProperties properties,
            ObjectProvider<RestClient.Builder> restClientBuilderProvider,
            ObjectProvider<ArmorAuthAdminRestClientCustomizer> customizers) {
        Assert.hasText(properties.getBaseUrl(), "armorauth.admin-client.base-url must not be empty");
        RestClient.Builder builder = restClientBuilderProvider.getIfAvailable(RestClient::builder)
                .baseUrl(trimTrailingSlash(properties.getBaseUrl()))
                .defaultHeaders(headers -> {
                    if (StringUtils.hasText(properties.getBearerToken())) {
                        headers.setBearerAuth(properties.getBearerToken());
                    }
                    else if (StringUtils.hasText(properties.getUsername())) {
                        headers.setBasicAuth(properties.getUsername(), properties.getPassword() == null ? "" : properties.getPassword());
                    }
                });
        customizers.orderedStream().forEach(customizer -> customizer.customize(builder));
        return new ArmorAuthAdminRestClient(builder.build());
    }

    private String trimTrailingSlash(String baseUrl) {
        String value = baseUrl.trim();
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
