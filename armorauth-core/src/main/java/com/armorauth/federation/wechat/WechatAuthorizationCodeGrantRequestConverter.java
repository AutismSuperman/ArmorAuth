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
package com.armorauth.federation.wechat;

import com.armorauth.federation.ExtendedOAuth2ClientProvider;
import com.armorauth.federation.converter.OAuth2AuthorizationCodeGrantRequestConverter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

public class WechatAuthorizationCodeGrantRequestConverter implements OAuth2AuthorizationCodeGrantRequestConverter {

    /**
     * 微信获取token地址
     * {@code
     * https://api.weixin.qq.com/sns/oauth2/access_token?
     * grant_type=authorization_code&
     * code=CODE&
     * appid=APP_ID&
     * secret=SECRET
     * }
     * RequestEntity must be GET
     * Returns the {@link RequestEntity} used for the Access Token Request.
     */
    @Override
    public RequestEntity<?> convert(OAuth2AuthorizationCodeGrantRequest request) {
        ClientRegistration clientRegistration = request.getClientRegistration();
        HttpHeaders headers = getTokenRequestHeaders(clientRegistration);
        OAuth2AuthorizationExchange authorizationExchange = request.getAuthorizationExchange();
        MultiValueMap<String, String> queryParameters = new LinkedMultiValueMap<>();
        queryParameters.add(OAuth2ParameterNames.GRANT_TYPE, request.getGrantType().getValue());
        queryParameters.add(OAuth2ParameterNames.CODE, authorizationExchange.getAuthorizationResponse().getCode());
        queryParameters.add(WechatParameterNames.APP_ID, clientRegistration.getClientId());
        queryParameters.add(WechatParameterNames.SECRET, clientRegistration.getClientSecret());
        String tokenUri = clientRegistration.getProviderDetails().getTokenUri();
        URI uri = UriComponentsBuilder.fromUriString(tokenUri).queryParams(queryParameters).build().toUri();
        return RequestEntity.get(uri).headers(headers).build();
    }


    private static HttpHeaders getTokenRequestHeaders(ClientRegistration clientRegistration) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.valueOf(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")));
        final MediaType contentType = MediaType.valueOf(MediaType.APPLICATION_FORM_URLENCODED_VALUE + ";charset=UTF-8");
        headers.setContentType(contentType);
        if (ClientAuthenticationMethod.CLIENT_SECRET_BASIC.equals(clientRegistration.getClientAuthenticationMethod())) {
            String clientId = encodeClientCredential(clientRegistration.getClientId());
            String clientSecret = encodeClientCredential(clientRegistration.getClientSecret());
            headers.setBasicAuth(clientId, clientSecret);
        }
        return headers;
    }

    private static String encodeClientCredential(String clientCredential) {
        return URLEncoder.encode(clientCredential, StandardCharsets.UTF_8);
    }


    @Override
    public boolean supports(String registrationId) {
        return ExtendedOAuth2ClientProvider.matchNameLowerCase(ExtendedOAuth2ClientProvider.WECHAT, registrationId);
    }


}
