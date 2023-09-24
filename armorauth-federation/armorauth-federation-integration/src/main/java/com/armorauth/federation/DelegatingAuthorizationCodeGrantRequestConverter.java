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

import com.armorauth.federation.endpoint.OAuth2AuthorizationCodeGrantRequestConverter;
import com.armorauth.federation.qq.endpoint.QqAuthorizationCodeGrantRequestConverter;
import com.armorauth.federation.wechat.endpoint.WechatAuthorizationCodeGrantRequestConverter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.RequestEntity;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequestEntityConverter;
import org.springframework.security.oauth2.client.registration.ClientRegistration;

import java.util.ArrayList;
import java.util.List;

public class DelegatingAuthorizationCodeGrantRequestConverter implements Converter<OAuth2AuthorizationCodeGrantRequest, RequestEntity<?>> {

    private final OAuth2AuthorizationCodeGrantRequestEntityConverter defaultConverter = new OAuth2AuthorizationCodeGrantRequestEntityConverter();

    private final List<OAuth2AuthorizationCodeGrantRequestConverter> authorizationCodeGrantRequestConverters;


    public DelegatingAuthorizationCodeGrantRequestConverter(List<OAuth2AuthorizationCodeGrantRequestConverter> authorizationCodeGrantRequestConverters) {
        this.authorizationCodeGrantRequestConverters = authorizationCodeGrantRequestConverters;
    }


    @Override
    public RequestEntity<?> convert(OAuth2AuthorizationCodeGrantRequest request) {
        ClientRegistration clientRegistration = request.getClientRegistration();
        String registrationId = clientRegistration.getRegistrationId();
        for (OAuth2AuthorizationCodeGrantRequestConverter converter : authorizationCodeGrantRequestConverters) {
            if (converter.supports(registrationId)) {
                return converter.convert(request);
            }
        }
        return defaultConverter.convert(request);
    }

    public void addAuthorizationCodeGrantRequestConverter(OAuth2AuthorizationCodeGrantRequestConverter auth2AuthorizationCodeGrantRequestConverter) {
        authorizationCodeGrantRequestConverters.add(auth2AuthorizationCodeGrantRequestConverter);
    }


}
