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

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for ArmorAuth resource server support.
 *
 * @author fulin
 * @since 2026-06-21
 */
@ConfigurationProperties(prefix = "armorauth.resource-server")
public class ArmorAuthResourceServerProperties {

    /**
     * Enable ArmorAuth's default resource server integration.
     */
    private boolean enabled;

    /**
     * Request matcher protected by the default resource server security chain.
     */
    private String securityMatcher = "/api/**";

    /**
     * Request patterns that remain public inside the protected matcher.
     */
    private List<String> permitAll = new ArrayList<>(List.of("/api/public/**"));

    /**
     * Whether CSRF protection should remain enabled.
     */
    private boolean csrfEnabled;

    /**
     * JWT claim used as the Spring Security principal name.
     */
    private String principalClaim = "sub";

    /**
     * Claims mapped to ROLE_* authorities.
     */
    private List<String> roleClaims = new ArrayList<>(List.of("roles"));

    /**
     * Claims mapped to SCOPE_* authorities.
     */
    private List<String> scopeClaims = new ArrayList<>(List.of("scope", "scp"));

    /**
     * Claims mapped to PERMISSION_* authorities.
     */
    private List<String> permissionClaims = new ArrayList<>(List.of("permissions"));

    /**
     * Claims mapped to ORG_ROLE_* authorities.
     */
    private List<String> organizationRoleClaims = new ArrayList<>(List.of("org_roles"));

    private String rolePrefix = "ROLE_";

    private String scopePrefix = "SCOPE_";

    private String permissionPrefix = "PERMISSION_";

    private String organizationRolePrefix = "ORG_ROLE_";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSecurityMatcher() {
        return securityMatcher;
    }

    public void setSecurityMatcher(String securityMatcher) {
        this.securityMatcher = securityMatcher;
    }

    public List<String> getPermitAll() {
        return permitAll;
    }

    public void setPermitAll(List<String> permitAll) {
        this.permitAll = permitAll;
    }

    public boolean isCsrfEnabled() {
        return csrfEnabled;
    }

    public void setCsrfEnabled(boolean csrfEnabled) {
        this.csrfEnabled = csrfEnabled;
    }

    public String getPrincipalClaim() {
        return principalClaim;
    }

    public void setPrincipalClaim(String principalClaim) {
        this.principalClaim = principalClaim;
    }

    public List<String> getRoleClaims() {
        return roleClaims;
    }

    public void setRoleClaims(List<String> roleClaims) {
        this.roleClaims = roleClaims;
    }

    public List<String> getScopeClaims() {
        return scopeClaims;
    }

    public void setScopeClaims(List<String> scopeClaims) {
        this.scopeClaims = scopeClaims;
    }

    public List<String> getPermissionClaims() {
        return permissionClaims;
    }

    public void setPermissionClaims(List<String> permissionClaims) {
        this.permissionClaims = permissionClaims;
    }

    public List<String> getOrganizationRoleClaims() {
        return organizationRoleClaims;
    }

    public void setOrganizationRoleClaims(List<String> organizationRoleClaims) {
        this.organizationRoleClaims = organizationRoleClaims;
    }

    public String getRolePrefix() {
        return rolePrefix;
    }

    public void setRolePrefix(String rolePrefix) {
        this.rolePrefix = rolePrefix;
    }

    public String getScopePrefix() {
        return scopePrefix;
    }

    public void setScopePrefix(String scopePrefix) {
        this.scopePrefix = scopePrefix;
    }

    public String getPermissionPrefix() {
        return permissionPrefix;
    }

    public void setPermissionPrefix(String permissionPrefix) {
        this.permissionPrefix = permissionPrefix;
    }

    public String getOrganizationRolePrefix() {
        return organizationRolePrefix;
    }

    public void setOrganizationRolePrefix(String organizationRolePrefix) {
        this.organizationRolePrefix = organizationRolePrefix;
    }
}
