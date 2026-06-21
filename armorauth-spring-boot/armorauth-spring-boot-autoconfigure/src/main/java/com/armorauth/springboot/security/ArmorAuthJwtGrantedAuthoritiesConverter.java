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
package com.armorauth.springboot.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.armorauth.autoconfigure.ArmorAuthResourceServerProperties;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;

/**
 * Maps ArmorAuth JWT claims to Spring Security authorities.
 *
 * @author fulin
 * @since 2026-06-21
 */
public class ArmorAuthJwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final ArmorAuthResourceServerProperties properties;

    public ArmorAuthJwtGrantedAuthoritiesConverter(ArmorAuthResourceServerProperties properties) {
        this.properties = properties;
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        addAuthorities(authorities, jwt, properties.getRoleClaims(), properties.getRolePrefix());
        addAuthorities(authorities, jwt, properties.getScopeClaims(), properties.getScopePrefix());
        addAuthorities(authorities, jwt, properties.getPermissionClaims(), properties.getPermissionPrefix());
        addAuthorities(authorities, jwt, properties.getOrganizationRoleClaims(), properties.getOrganizationRolePrefix());
        return authorities;
    }

    private void addAuthorities(List<GrantedAuthority> authorities, Jwt jwt, List<String> claimNames, String prefix) {
        if (claimNames == null) {
            return;
        }
        for (String claimName : claimNames) {
            for (String value : ArmorAuthClaimUtils.strings(jwt.getClaims(), claimName)) {
                authorities.add(new SimpleGrantedAuthority(applyPrefix(value, prefix)));
            }
        }
    }

    private String applyPrefix(String value, String prefix) {
        if (!StringUtils.hasText(prefix) || value.startsWith(prefix)) {
            return value;
        }
        return prefix + value;
    }
}
