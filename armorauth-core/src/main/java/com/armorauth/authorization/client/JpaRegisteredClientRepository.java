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
package com.armorauth.authorization.client;

import com.armorauth.data.entity.OAuth2Client;
import com.armorauth.data.entity.OAuth2ClientSettings;
import com.armorauth.data.entity.OAuth2Scope;
import com.armorauth.data.entity.OAuth2TokenSettings;
import com.armorauth.data.repository.OAuth2ClientRepository;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

public class JpaRegisteredClientRepository implements RegisteredClientRepository {

    private final OAuth2ClientRepository oAuth2ClientRepository;


    public JpaRegisteredClientRepository(OAuth2ClientRepository oAuth2ClientRepository) {
        Assert.notNull(oAuth2ClientRepository, "oAuth2ClientRepository cannot be null");
        this.oAuth2ClientRepository = oAuth2ClientRepository;
    }

    @Override
    public void save(RegisteredClient registeredClient) {
        Assert.notNull(registeredClient, "registeredClient cannot be null");
        oAuth2ClientRepository.save(toEntity(registeredClient));
    }

    @Override
    public RegisteredClient findById(String id) {
        Assert.hasText(id, "id cannot be empty");
        Optional<OAuth2Client> oAuth2ClientById =
                oAuth2ClientRepository.findOAuth2ClientById(id);
        return oAuth2ClientById
                .map(this::toObject)
                .orElse(null);
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        Assert.hasText(clientId, "clientId cannot be empty");
        Optional<OAuth2Client> oAuth2ClientByClientId =
                oAuth2ClientRepository.findOAuth2ClientByClientId(clientId);
        return oAuth2ClientByClientId
                .map(this::toObject)
                .orElse(null);
    }


    /**
     * registeredClient 转换为 OAuth2Client
     *
     * @param registeredClient registeredClient
     * @return OAuth2Client
     */
    public OAuth2Client toEntity(RegisteredClient registeredClient) {
        List<String> clientAuthenticationMethods = new ArrayList<>(registeredClient.getClientAuthenticationMethods().size());
        registeredClient.getClientAuthenticationMethods().forEach(clientAuthenticationMethod ->
                clientAuthenticationMethods.add(clientAuthenticationMethod.getValue()));

        List<String> authorizationGrantTypes = new ArrayList<>(registeredClient.getAuthorizationGrantTypes().size());
        registeredClient.getAuthorizationGrantTypes().forEach(authorizationGrantType ->
                authorizationGrantTypes.add(authorizationGrantType.getValue()));
        OAuth2Client client = new OAuth2Client();
        client.setId(registeredClient.getId());
        String clientId = registeredClient.getClientId();
        client.setClientId(clientId);
        client.setClientIdIssuedAt(registeredClient.getClientIdIssuedAt());
        //ignore clientSecret clientSecretExpiresAt
        client.setClientSecret(registeredClient.getClientSecret());
        client.setClientName(registeredClient.getClientName());
        client.setClientAuthenticationMethods(StringUtils.collectionToCommaDelimitedString(clientAuthenticationMethods));
        client.setAuthorizationGrantTypes(StringUtils.collectionToCommaDelimitedString(authorizationGrantTypes));
        client.setRedirectUris(StringUtils.collectionToCommaDelimitedString(registeredClient.getRedirectUris()));
        client.setPostLogoutRedirectUris(StringUtils.collectionToCommaDelimitedString(registeredClient.getPostLogoutRedirectUris()));
        client.setScopes(registeredClient.getScopes()
                .stream()
                .filter(scope -> !OidcScopes.OPENID.equals(scope))
                .map(scope -> {
                    OAuth2Scope oAuth2Scope = new OAuth2Scope();
                    oAuth2Scope.setClientId(clientId);
                    oAuth2Scope.setScope(scope);
                    return oAuth2Scope;
                }).collect(Collectors.toSet()));
        OAuth2ClientSettings settings = ClientTransformUtil.fromClientSettings(registeredClient.getClientSettings());
        settings.setClientId(clientId);
        client.setClientSettings(settings);
        OAuth2TokenSettings oAuth2TokenSettings = ClientTransformUtil.fromTokenSettings(registeredClient.getTokenSettings());
        oAuth2TokenSettings.setClientId(clientId);
        client.setTokenSettings(oAuth2TokenSettings);
        return client;
    }


    /**
     * oAuth2Client 转换为 RegisteredClient
     *
     * @param oAuth2Client oAuth2Client
     * @return RegisteredClient
     */
    public RegisteredClient toObject(OAuth2Client oAuth2Client) {
        Set<String> clientAuthenticationMethods = StringUtils.commaDelimitedListToSet(
                oAuth2Client.getClientAuthenticationMethods());
        Set<String> authorizationGrantTypes = StringUtils.commaDelimitedListToSet(
                oAuth2Client.getAuthorizationGrantTypes());
        Set<String> redirectUris = StringUtils.commaDelimitedListToSet(
                oAuth2Client.getRedirectUris());
        Set<String> postLogoutRedirectUris = StringUtils.commaDelimitedListToSet(
                oAuth2Client.getPostLogoutRedirectUris());
        Set<OAuth2Scope> oAuth2Scopes = oAuth2Client.getScopes() == null ? Collections.emptySet() : oAuth2Client.getScopes();
        RegisteredClient.Builder builder = RegisteredClient.withId(Optional.ofNullable(oAuth2Client.getId()).orElse(UUID.randomUUID().toString()))
                .clientId(Optional.ofNullable(oAuth2Client.getClientId()).orElse(UUID.randomUUID().toString()))
                .clientSecret(oAuth2Client.getClientSecret())
                .clientIdIssuedAt(oAuth2Client.getClientIdIssuedAt().atZone(ZoneId.systemDefault()).toInstant())
                .clientSecretExpiresAt(oAuth2Client.getClientSecretExpiresAt())
                .clientName(oAuth2Client.getClientName())
                .clientAuthenticationMethods(authenticationMethods ->
                        clientAuthenticationMethods.forEach(authenticationMethod ->
                                authenticationMethods.add(ClientTransformUtil.resolveClientAuthenticationMethod(authenticationMethod))))
                .authorizationGrantTypes((grantTypes) ->
                        authorizationGrantTypes.forEach(grantType ->
                                grantTypes.add(ClientTransformUtil.resolveAuthorizationGrantType(grantType))))
                .redirectUris((uris) -> uris.addAll(redirectUris))
                .postLogoutRedirectUris((uris) -> uris.addAll(postLogoutRedirectUris))
                .scopes(scopeSet -> scopeSet.addAll(oAuth2Scopes.stream()
                        .map(OAuth2Scope::getScope)
                        .collect(Collectors.toSet())))
                .scope(OidcScopes.OPENID)
                .clientSettings(ClientTransformUtil.toClientSettings(oAuth2Client.getClientSettings()))
                .tokenSettings(ClientTransformUtil.toTokenSettings(oAuth2Client.getTokenSettings()));
        return builder.build();
    }


}
