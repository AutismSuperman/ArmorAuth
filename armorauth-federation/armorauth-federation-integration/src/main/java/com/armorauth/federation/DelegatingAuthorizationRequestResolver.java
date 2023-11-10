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

import com.armorauth.federation.web.FederatedAuthorizationRequestRedirectFilter;
import com.armorauth.federation.web.converter.OAuth2AuthorizationRequestConverter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.Assert;

import java.util.List;

public class DelegatingAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final DefaultOAuth2AuthorizationRequestResolver delegate;

    private final List<OAuth2AuthorizationRequestConverter> authorizationRequestConverters;

    public DelegatingAuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository,
                                                  List<OAuth2AuthorizationRequestConverter> authorizationRequestConverters) {
        this(clientRegistrationRepository,
                FederatedAuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI, authorizationRequestConverters);
    }


    public DelegatingAuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository,
                                                  String authorizationRequestBaseUri,
                                                  List<OAuth2AuthorizationRequestConverter> authorizationRequestConverters) {
        Assert.notNull(clientRegistrationRepository, "clientRegistrationRepository cannot be null");
        this.authorizationRequestConverters = authorizationRequestConverters;
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, authorizationRequestBaseUri);
        this.delegate.setAuthorizationRequestCustomizer(this::authorizationRequestCustomizer);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return delegate.resolve(request);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        return delegate.resolve(request, clientRegistrationId);
    }

    public void authorizationRequestCustomizer(OAuth2AuthorizationRequest.Builder builder) {
        builder.attributes(attribute -> {
            String registrationId = (String) attribute.get(OAuth2ParameterNames.REGISTRATION_ID);
            authorizationRequestConverters.stream()
                    .filter(authorizationRequestConverter -> authorizationRequestConverter.supports(registrationId))
                    .findAny()
                    .ifPresent(authorizationRequestConverter -> authorizationRequestConverter.convert(builder));
        });
    }

    public void addOAuth2AuthorizationRequestConverter(OAuth2AuthorizationRequestConverter authorizationRequestConverter) {
        this.authorizationRequestConverters.add(authorizationRequestConverter);
    }


}