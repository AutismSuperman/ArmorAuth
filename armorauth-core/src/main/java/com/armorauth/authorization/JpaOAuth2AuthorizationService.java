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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.armorauth.authorization.client.ClientTransformUtil;
import com.armorauth.data.entity.Authorization;
import com.armorauth.data.repository.AuthorizationRepository;
import com.armorauth.jackson.ArmorAuthJackson2Module;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class JpaOAuth2AuthorizationService implements OAuth2AuthorizationService {


    private final AuthorizationRepository authorizationRepository;
    private final RegisteredClientRepository registeredClientRepository;
    /**
     * 处理 metadata
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JpaOAuth2AuthorizationService(AuthorizationRepository authorizationRepository, RegisteredClientRepository registeredClientRepository) {
        Assert.notNull(authorizationRepository, "authorizationRepository cannot be null");
        Assert.notNull(registeredClientRepository, "registeredClientRepository cannot be null");
        this.authorizationRepository = authorizationRepository;
        this.registeredClientRepository = registeredClientRepository;
        ClassLoader classLoader = JpaOAuth2AuthorizationService.class.getClassLoader();
        List<Module> securityModules = SecurityJackson2Modules.getModules(classLoader);
        this.objectMapper.registerModules(securityModules);
        //处理 metadata
        this.objectMapper.registerModules(new OAuth2AuthorizationServerJackson2Module(), new ArmorAuthJackson2Module());
    }


    @Override
    public void save(OAuth2Authorization authorization) {
        Assert.notNull(authorization, "authorization cannot be null");
        authorizationRepository.save(toEntity(authorization));
    }

    @Override
    public void remove(OAuth2Authorization authorization) {
        Assert.notNull(authorization, "authorization cannot be null");
        authorizationRepository.deleteById(authorization.getId());
    }

    @Override
    public OAuth2Authorization findById(String id) {
        Assert.hasText(id, "id cannot be empty");
        Optional<Authorization> authorization = this.authorizationRepository.findById(id);
        return authorization.map(this::toObject).orElse(null);
    }

    @Override
    public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
        Assert.hasText(token, "token cannot be empty");
        Optional<Authorization> result;
        if (tokenType == null) {
            result = authorizationRepository.findByStateOrAuthorizationCodeValueOrAccessTokenValueOrRefreshTokenValue(token);
        } else if (OAuth2ParameterNames.STATE.equals(tokenType.getValue())) {
            result = authorizationRepository.findByState(token);
        } else if (OAuth2ParameterNames.CODE.equals(tokenType.getValue())) {
            result = authorizationRepository.findByAuthorizationCodeValue(token);
        } else if (OAuth2ParameterNames.ACCESS_TOKEN.equals(tokenType.getValue())) {
            result = authorizationRepository.findByAccessTokenValue(token);
        } else if (OAuth2ParameterNames.REFRESH_TOKEN.equals(tokenType.getValue())) {
            result = authorizationRepository.findByRefreshTokenValue(token);
        } else {
            result = Optional.empty();
        }
        return result.map(this::toObject).orElse(null);
    }


    //*****************************************************convert*****************************************************//


    /**
     * Authorization  convert OAuth2Authorization
     *
     * @param authorization OAuth2Authorization
     * @return Authorization
     * @see OAuth2Authorization
     * @see Authorization
     */
    private OAuth2Authorization toObject(Authorization authorization) {
        RegisteredClient registeredClient = this.registeredClientRepository.findById(authorization.getRegisteredClientId());
        if (registeredClient == null) {
            throw new DataRetrievalFailureException(
                    "The RegisteredClient with id '" + authorization.getRegisteredClientId() + "' was not found in the RegisteredClientRepository.");
        }
        OAuth2Authorization.Builder builder = OAuth2Authorization.withRegisteredClient(registeredClient)
                .id(authorization.getId())
                .principalName(authorization.getPrincipalName())
                .authorizationGrantType(ClientTransformUtil.resolveAuthorizationGrantType(authorization.getAuthorizationGrantType()))
                .attributes(attributes -> attributes.putAll(parseMap(authorization.getAttributes())));

        if (authorization.getAuthorizedScopes() != null) {
            builder.authorizedScopes(StringUtils.commaDelimitedListToSet(authorization.getAuthorizedScopes()));
        }
        if (authorization.getState() != null) {
            builder.attribute(OAuth2ParameterNames.STATE, authorization.getState());
        }
        // AuthorizationCode
        if (authorization.getAuthorizationCodeValue() != null) {
            OAuth2AuthorizationCode authorizationCode = new OAuth2AuthorizationCode(
                    authorization.getAuthorizationCodeValue(),
                    authorization.getAuthorizationCodeIssuedAt(),
                    authorization.getAuthorizationCodeExpiresAt());
            builder.token(authorizationCode, metadata -> metadata.putAll(parseMap(authorization.getAuthorizationCodeMetadata())));
        }
        // AccessToken
        if (authorization.getAccessTokenValue() != null) {
            OAuth2AccessToken accessToken = new OAuth2AccessToken(
                    OAuth2AccessToken.TokenType.BEARER,
                    authorization.getAccessTokenValue(),
                    authorization.getAccessTokenIssuedAt(),
                    authorization.getAccessTokenExpiresAt(),
                    StringUtils.commaDelimitedListToSet(authorization.getAccessTokenScopes()));
            builder.token(accessToken, metadata -> metadata.putAll(parseMap(authorization.getAccessTokenMetadata())));
        }
        // RefreshToken
        if (authorization.getRefreshTokenValue() != null) {
            OAuth2RefreshToken refreshToken = new OAuth2RefreshToken(
                    authorization.getRefreshTokenValue(),
                    authorization.getRefreshTokenIssuedAt(),
                    authorization.getRefreshTokenExpiresAt());
            builder.token(refreshToken, metadata -> metadata.putAll(parseMap(authorization.getRefreshTokenMetadata())));
        }
        // OidcIdToken
        if (authorization.getOidcIdTokenValue() != null) {
            OidcIdToken idToken = new OidcIdToken(
                    authorization.getOidcIdTokenValue(),
                    authorization.getOidcIdTokenIssuedAt(),
                    authorization.getOidcIdTokenExpiresAt(),
                    parseMap(authorization.getOidcIdTokenClaims()));
            builder.token(idToken, metadata -> metadata.putAll(parseMap(authorization.getOidcIdTokenMetadata())));
        }
        return builder.build();
    }


    /**
     * OAuth2Authorization convert Authorization
     *
     * @param oAuth2Authorization OAuth2Authorization
     * @return Authorization
     * @see OAuth2Authorization
     * @see Authorization
     */
    private Authorization toEntity(OAuth2Authorization oAuth2Authorization) {
        Authorization authorization = new Authorization();
        authorization.setId(oAuth2Authorization.getId());
        authorization.setRegisteredClientId(oAuth2Authorization.getRegisteredClientId());
        authorization.setPrincipalName(oAuth2Authorization.getPrincipalName());
        authorization.setAuthorizationGrantType(oAuth2Authorization.getAuthorizationGrantType().getValue());
        //attributes metadata
        authorization.setAttributes(writeMap(oAuth2Authorization.getAttributes()));
        authorization.setState(oAuth2Authorization.getAttribute(OAuth2ParameterNames.STATE));
        if (!CollectionUtils.isEmpty(oAuth2Authorization.getAuthorizedScopes())) {
            authorization.setAuthorizedScopes(StringUtils.collectionToDelimitedString(oAuth2Authorization.getAuthorizedScopes(), ","));
        }
        // AuthorizationCode
        OAuth2Authorization.Token<OAuth2AuthorizationCode> authorizationCode =
                oAuth2Authorization.getToken(OAuth2AuthorizationCode.class);
        setTokenValues(
                authorizationCode,
                authorization::setAuthorizationCodeValue,
                authorization::setAuthorizationCodeIssuedAt,
                authorization::setAuthorizationCodeExpiresAt,
                authorization::setAuthorizationCodeMetadata
        );
        // AccessToken
        OAuth2Authorization.Token<OAuth2AccessToken> accessToken =
                oAuth2Authorization.getToken(OAuth2AccessToken.class);
        setTokenValues(
                accessToken,
                authorization::setAccessTokenValue,
                authorization::setAccessTokenIssuedAt,
                authorization::setAccessTokenExpiresAt,
                authorization::setAccessTokenMetadata
        );
        // AccessTokenScopes
        if (accessToken != null && accessToken.getToken().getScopes() != null) {
            authorization.setAccessTokenScopes(StringUtils.
                    collectionToDelimitedString(accessToken.getToken().getScopes(), ","));
        }
        // RefreshToken
        OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken =
                oAuth2Authorization.getToken(OAuth2RefreshToken.class);
        setTokenValues(
                refreshToken,
                authorization::setRefreshTokenValue,
                authorization::setRefreshTokenIssuedAt,
                authorization::setRefreshTokenExpiresAt,
                authorization::setRefreshTokenMetadata
        );
        // OidcIdToken
        OAuth2Authorization.Token<OidcIdToken> oidcIdToken =
                oAuth2Authorization.getToken(OidcIdToken.class);
        setTokenValues(
                oidcIdToken,
                authorization::setOidcIdTokenValue,
                authorization::setOidcIdTokenIssuedAt,
                authorization::setOidcIdTokenExpiresAt,
                authorization::setOidcIdTokenMetadata
        );
        if (oidcIdToken != null) {
            authorization.setOidcIdTokenClaims(writeMap(oidcIdToken.getClaims()));
        }
        return authorization;
    }


    private void setTokenValues(
            OAuth2Authorization.Token<?> token, Consumer<String> tokenValueConsumer,
            Consumer<Instant> issuedAtConsumer, Consumer<Instant> expiresAtConsumer,
            Consumer<String> metadataConsumer) {
        if (token != null) {
            OAuth2Token oAuth2Token = token.getToken();
            tokenValueConsumer.accept(oAuth2Token.getTokenValue());
            issuedAtConsumer.accept(oAuth2Token.getIssuedAt());
            expiresAtConsumer.accept(oAuth2Token.getExpiresAt());
            metadataConsumer.accept(writeMap(token.getMetadata()));
        }
    }


    private Map<String, Object> parseMap(String data) {
        try {
            return this.objectMapper.readValue(data, new TypeReference<>() {
            });
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }

    private String writeMap(Map<String, Object> metadata) {
        try {
            //处理 metadata
            return this.objectMapper.writeValueAsString(metadata);
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }


}
