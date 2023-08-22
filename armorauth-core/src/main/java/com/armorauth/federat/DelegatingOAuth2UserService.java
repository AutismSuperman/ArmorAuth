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

import com.armorauth.federat.gitee.GiteeOAuth2UserService;
import com.armorauth.federat.qq.QqOAuth2UserService;
import com.armorauth.federat.wechat.WechatOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DelegatingOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> defaultOAuth2UserService = new DefaultOAuth2UserService();

    private final Map<String, OAuth2UserService<OAuth2UserRequest, OAuth2User>> userServices = new HashMap<>(16);

    public DelegatingOAuth2UserService() {
        this.userServices.put(ExtendedOAuth2ClientProvider.GITEE.name(), new GiteeOAuth2UserService());
        this.userServices.put(ExtendedOAuth2ClientProvider.QQ.name(), new QqOAuth2UserService());
        this.userServices.put(ExtendedOAuth2ClientProvider.WECHAT.name(), new WechatOAuth2UserService());
    }


    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService = userServices.get(registrationId.toUpperCase());
        if (oAuth2UserService == null) {
            oAuth2UserService = defaultOAuth2UserService;
        }
        return oAuth2UserService.loadUser(userRequest);
    }

    public void addOAuth2UserService(String registrationId, OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService) {
        this.userServices.put(registrationId.toUpperCase(), oAuth2UserService);
    }

}