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
package com.armorauth.federation.qq;

import com.armorauth.federation.ExtendedOAuth2ClientProvider;
import com.armorauth.federation.converter.OAuth2AuthorizationCodeGrantRequestConverter;
import org.springframework.http.RequestEntity;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

public class QqOAuth2AuthorizationCodeGrantRequestConverter implements OAuth2AuthorizationCodeGrantRequestConverter {

    @Override
    public RequestEntity<?> convert(OAuth2AuthorizationCodeGrantRequest request) {
        ClientRegistration clientRegistration = request.getClientRegistration();
        OAuth2AuthorizationExchange authorizationExchange = request.getAuthorizationExchange();
        MultiValueMap<String, String> queryParameters = new LinkedMultiValueMap<>();
        queryParameters.add(OAuth2ParameterNames.GRANT_TYPE, request.getGrantType().getValue());
        queryParameters.add(OAuth2ParameterNames.CLIENT_ID, clientRegistration.getClientId());
        queryParameters.add(OAuth2ParameterNames.CLIENT_SECRET, clientRegistration.getClientSecret());
        queryParameters.add(OAuth2ParameterNames.CODE, authorizationExchange.getAuthorizationResponse().getCode());
        queryParameters.add(OAuth2ParameterNames.REDIRECT_URI, authorizationExchange.getAuthorizationRequest().getRedirectUri());
        queryParameters.add(QqParameterNames.FMT, QqParameterNames.FMT_JOSN);
        // 1 is response openid
        queryParameters.add(QqParameterNames.NEED_OPENID, "1");
        String tokenUri = clientRegistration.getProviderDetails().getTokenUri();
        URI uri = UriComponentsBuilder.fromUriString(tokenUri).queryParams(queryParameters).build().toUri();
        return RequestEntity.get(uri).build();
    }


    @Override
    public boolean supports(String registrationId) {
        return ExtendedOAuth2ClientProvider.matchNameLowerCase(ExtendedOAuth2ClientProvider.QQ, registrationId);
    }
}
