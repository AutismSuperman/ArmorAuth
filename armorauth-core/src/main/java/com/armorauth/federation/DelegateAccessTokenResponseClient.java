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
package com.armorauth.federation;

import com.armorauth.federation.converter.DelegatingOAuth2AuthorizationCodeGrantRequestEntityConverter;
import com.armorauth.federation.converter.OAuth2AccessTokenRestTemplate;
import com.armorauth.federation.qq.QqAccessTokenRestTemplate;
import com.armorauth.federation.wechat.WechatAccessTokenRestTemplate;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;

import java.util.ArrayList;
import java.util.List;

public class DelegateAccessTokenResponseClient implements OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> {

    private final DefaultAuthorizationCodeTokenResponseClient delegate = new DefaultAuthorizationCodeTokenResponseClient();

    private final List<OAuth2AccessTokenRestTemplate> restTemplates= new ArrayList<>();


    public DelegateAccessTokenResponseClient() {
        this.restTemplates.add(new WechatAccessTokenRestTemplate());
        this.restTemplates.add(new QqAccessTokenRestTemplate());
        this.delegate.setRequestEntityConverter(new DelegatingOAuth2AuthorizationCodeGrantRequestEntityConverter());
    }

    @Override
    public OAuth2AccessTokenResponse getTokenResponse(OAuth2AuthorizationCodeGrantRequest authorizationGrantRequest) {
        String registrationId = authorizationGrantRequest.getClientRegistration().getRegistrationId();
        restTemplates.stream().filter(f -> f.supports(registrationId)).findFirst().ifPresent(accessTokenRestTemplate -> {
            delegate.setRestOperations(accessTokenRestTemplate.getRestTemplate(authorizationGrantRequest));
        });
        return delegate.getTokenResponse(authorizationGrantRequest);
    }

    public void addAccessTokenRestTemplate(OAuth2AccessTokenRestTemplate restTemplate) {
        restTemplates.add(restTemplate);
    }

}
