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

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

public class BindUserCheckProvider implements AuthenticationProvider {


    private final BindUserCheckService bindUserCheckService;

    public BindUserCheckProvider(BindUserCheckService bindUserCheckService) {
        this.bindUserCheckService = bindUserCheckService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        //check if the user is bind
        //1.需要绑定的话，就返回一个FederatedBindUserAuthenticationToken认证为false
        //1.不需要绑定的话，就返回一个FederatedBindUserAuthenticationToken认证为true
        BindUserCheckToken bindUserCheckToken = (BindUserCheckToken) authentication;
        String userNameAttributeName = bindUserCheckToken.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
        Boolean requireBindUser = bindUserCheckService.requireBindUser(
                bindUserCheckToken.getPrincipal().getAttributes().get(userNameAttributeName).toString(),
                bindUserCheckToken.getClientRegistration().getRegistrationId()
        );
        bindUserCheckToken.setAuthenticated(requireBindUser);
        return bindUserCheckToken;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return BindUserCheckToken.class.isAssignableFrom(authentication);
    }

}
