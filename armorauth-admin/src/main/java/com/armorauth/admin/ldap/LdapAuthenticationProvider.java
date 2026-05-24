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
package com.armorauth.admin.ldap;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class LdapAuthenticationProvider implements AuthenticationProvider {

    private final LdapLiveAuthenticationService ldapLiveAuthenticationService;

    public LdapAuthenticationProvider(LdapLiveAuthenticationService ldapLiveAuthenticationService) {
        this.ldapLiveAuthenticationService = ldapLiveAuthenticationService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        Object credentials = authentication.getCredentials();
        String password = credentials == null ? "" : credentials.toString();
        if (!hasText(username) || !hasText(password)) {
            return null;
        }

        Optional<UserDetails> userDetails = ldapLiveAuthenticationService.authenticate(username, password);
        if (userDetails.isEmpty()) {
            return null;
        }
        UserDetails principal = userDetails.get();
        UsernamePasswordAuthenticationToken result =
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
        result.setDetails(authentication.getDetails());
        return result;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
