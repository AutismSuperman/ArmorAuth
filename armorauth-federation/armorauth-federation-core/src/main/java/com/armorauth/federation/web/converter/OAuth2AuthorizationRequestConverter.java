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
package com.armorauth.federation.web.converter;

import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.function.Consumer;

/**
 * 针对 OAuth2AuthorizationRequestResolver 处理结果进行第三方适配转换
 * @author AutismSuperman
 * @see OAuth2AuthorizationRequestResolver
 * @see DefaultOAuth2AuthorizationRequestResolver#setAuthorizationRequestCustomizer(Consumer)
 */
public interface OAuth2AuthorizationRequestConverter {

    /**
     * OAuth2AuthorizationRequest.Builder convert
     *
     * @param builder
     */
    void convert(OAuth2AuthorizationRequest.Builder builder);


    /**
     * Returns <code>true</code> if this <Code>OAuth2AuthorizationCodeGrantRequestConverter</code> supports the
     * indicated <Code>ClientRegistration</code>.
     *
     * @param registrationId the registration identifier
     * @return <code>true</code> if the implementation can more closely evaluate the
     * <code>ClientRegistration</code>
     */
    boolean supports(String registrationId);


}