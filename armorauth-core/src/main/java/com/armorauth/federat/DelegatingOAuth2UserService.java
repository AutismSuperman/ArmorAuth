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
package com.armorauth.federat;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collections;
import java.util.Map;

public class DelegatingOAuth2UserService<R extends OAuth2UserRequest, U extends OAuth2User> implements OAuth2UserService<R, U> {

    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> defaultOAuth2UserService = new DefaultOAuth2UserService();


    private final Map<String, OAuth2UserService<R, U>> userServiceMap;

    public DelegatingOAuth2UserService(Map<String, OAuth2UserService<R, U>> userServiceMap) {
        this.userServiceMap = Collections.unmodifiableMap(userServiceMap);
    }


    @SuppressWarnings("unchecked")
    @Override
    public U loadUser(R userRequest) throws OAuth2AuthenticationException {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserService<R, U> oAuth2UserService = userServiceMap.get(registrationId);
        if (oAuth2UserService == null) {
            oAuth2UserService = (OAuth2UserService<R, U>) defaultOAuth2UserService;
        }
        return oAuth2UserService.loadUser(userRequest);
    }


}