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
package com.armorauth.authorization;

import com.armorauth.data.entity.AuthorizationConsent;
import com.armorauth.data.repository.AuthorizationConsentRepository;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class JpaOAuth2AuthorizationConsentService implements OAuth2AuthorizationConsentService {


    private final AuthorizationConsentRepository authorizationConsentRepository;
    private final RegisteredClientRepository registeredClientRepository;

    public JpaOAuth2AuthorizationConsentService(AuthorizationConsentRepository authorizationConsentRepository,
                                                RegisteredClientRepository registeredClientRepository) {
        Assert.notNull(authorizationConsentRepository, "authorizationConsentRepository cannot be null");
        Assert.notNull(registeredClientRepository, "registeredClientRepository cannot be null");
        this.authorizationConsentRepository = authorizationConsentRepository;
        this.registeredClientRepository = registeredClientRepository;
    }


    @Override
    public void save(OAuth2AuthorizationConsent oAuth2AuthorizationConsent) {
        Assert.notNull(oAuth2AuthorizationConsent, "oAuth2AuthorizationConsent cannot be null");
        authorizationConsentRepository.save(toEntity(oAuth2AuthorizationConsent));
    }

    @Override
    public void remove(OAuth2AuthorizationConsent oAuth2AuthorizationConsent) {
        Assert.notNull(oAuth2AuthorizationConsent, "oAuth2AuthorizationConsent cannot be null");
        authorizationConsentRepository.deleteByPrincipalNameAndRegisteredClientId(
                oAuth2AuthorizationConsent.getPrincipalName(),
                oAuth2AuthorizationConsent.getRegisteredClientId()
        );
    }

    @Override
    public OAuth2AuthorizationConsent findById(String registeredClientId, String principalName) {
        Assert.hasText(registeredClientId, "registeredClientId cannot be empty");
        Assert.hasText(principalName, "principalName cannot be empty");
        AuthorizationConsent byPrincipalNameAndRegisteredClientId = authorizationConsentRepository.findByPrincipalNameAndRegisteredClientId(
                principalName,
                registeredClientId
        );
        return Optional.ofNullable(byPrincipalNameAndRegisteredClientId).map(this::toObject).orElse(null);
    }


    /**
     * AuthorizationConsent convert OAuth2AuthorizationConsent
     *
     * @param authorizationConsent AuthorizationConsent
     * @return AuthorizationConsent
     * @see OAuth2AuthorizationConsent
     * @see AuthorizationConsent
     */
    private OAuth2AuthorizationConsent toObject(AuthorizationConsent authorizationConsent) {
        String registeredClientId = authorizationConsent.getRegisteredClientId();
        RegisteredClient registeredClient = this.registeredClientRepository.findById(registeredClientId);
        if (registeredClient == null) {
            throw new DataRetrievalFailureException(
                    "The RegisteredClient with id '" + registeredClientId + "' was not found in the RegisteredClientRepository.");
        }
        OAuth2AuthorizationConsent.Builder builder = OAuth2AuthorizationConsent.withId(
                registeredClientId, authorizationConsent.getPrincipalName());
        if (authorizationConsent.getAuthorities() != null) {
            for (String authority : StringUtils.commaDelimitedListToSet(authorizationConsent.getAuthorities())) {
                builder.authority(new SimpleGrantedAuthority(authority));
            }
        }
        return builder.build();
    }


    /**
     * OAuth2AuthorizationConsent convert AuthorizationConsent
     *
     * @param oAuth2AuthorizationConsent OAuth2Authorization
     * @return AuthorizationConsent
     * @see OAuth2AuthorizationConsent
     * @see AuthorizationConsent
     */
    private AuthorizationConsent toEntity(OAuth2AuthorizationConsent oAuth2AuthorizationConsent) {
        AuthorizationConsent authorizationConsent = new AuthorizationConsent();
        authorizationConsent.setRegisteredClientId(oAuth2AuthorizationConsent.getRegisteredClientId());
        authorizationConsent.setPrincipalName(oAuth2AuthorizationConsent.getPrincipalName());
        Set<String> authorities = new HashSet<>();
        for (GrantedAuthority authority : oAuth2AuthorizationConsent.getAuthorities()) {
            authorities.add(authority.getAuthority());
        }
        authorizationConsent.setAuthorities(StringUtils.collectionToCommaDelimitedString(authorities));
        return authorizationConsent;
    }


}
