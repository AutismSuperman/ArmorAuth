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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the lightweight ArmorAuth Admin API client.
 *
 * @author fulin
 * @since 2026-06-21
 */
@ConfigurationProperties(prefix = "armorauth.admin-client")
public class ArmorAuthAdminClientProperties {

    /**
     * Enable the default Admin API RestClient bean.
     */
    private boolean enabled;

    /**
     * ArmorAuth server base URL.
     */
    private String baseUrl;

    /**
     * Admin username for Basic authentication.
     */
    private String username;

    /**
     * Admin password for Basic authentication.
     */
    private String password;

    /**
     * Bearer token. Takes precedence over username/password when set.
     */
    private String bearerToken;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getBearerToken() {
        return bearerToken;
    }

    public void setBearerToken(String bearerToken) {
        this.bearerToken = bearerToken;
    }
}
