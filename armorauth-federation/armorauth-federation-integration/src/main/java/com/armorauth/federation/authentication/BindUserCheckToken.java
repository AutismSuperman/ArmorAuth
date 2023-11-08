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
package com.armorauth.federation.authentication;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.util.Assert;

public class BindUserCheckToken extends AbstractAuthenticationToken {

    private final OAuth2User principal;

    private ClientRegistration clientRegistration;


    public BindUserCheckToken(OAuth2User principal, ClientRegistration clientRegistration) {
        super(null);
        Assert.notNull(principal, "principal cannot be null");
        Assert.notNull(clientRegistration, "clientRegistration cannot be null");
        this.principal = principal;
        this.clientRegistration = clientRegistration;
        this.setAuthenticated(false);
    }


    @Override
    public OAuth2User getPrincipal() {
        return principal;
    }

    @Override
    public Object getCredentials() {
        return principal;
    }

    public ClientRegistration getClientRegistration() {
        return clientRegistration;
    }
}
