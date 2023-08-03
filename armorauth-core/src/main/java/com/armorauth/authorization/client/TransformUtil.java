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

import com.armorauth.data.entity.*;
import lombok.Data;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithm;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Optional;

/**
 * 用于 spring-authorization-server 与数据库实体之间的转换
 *
 * @author fulin
 */
@Data
public class TransformUtil {


    public static AuthorizationGrantType resolveAuthorizationGrantType(String authorizationGrantType) {
        if (AuthorizationGrantType.AUTHORIZATION_CODE.getValue().equals(authorizationGrantType)) {
            return AuthorizationGrantType.AUTHORIZATION_CODE;
        } else if (AuthorizationGrantType.CLIENT_CREDENTIALS.getValue().equals(authorizationGrantType)) {
            return AuthorizationGrantType.CLIENT_CREDENTIALS;
        } else if (AuthorizationGrantType.REFRESH_TOKEN.getValue().equals(authorizationGrantType)) {
            return AuthorizationGrantType.REFRESH_TOKEN;
        }
        // Custom authorization grant type
        return new AuthorizationGrantType(authorizationGrantType);
    }

    public static ClientAuthenticationMethod resolveClientAuthenticationMethod(String clientAuthenticationMethod) {
        if (ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue().equals(clientAuthenticationMethod)) {
            return ClientAuthenticationMethod.CLIENT_SECRET_BASIC;
        } else if (ClientAuthenticationMethod.CLIENT_SECRET_POST.getValue().equals(clientAuthenticationMethod)) {
            return ClientAuthenticationMethod.CLIENT_SECRET_POST;
        } else if (ClientAuthenticationMethod.NONE.getValue().equals(clientAuthenticationMethod)) {
            return ClientAuthenticationMethod.NONE;
        }
        return new ClientAuthenticationMethod(clientAuthenticationMethod);      // Custom client authentication method
    }


    /**
     * oAuth2TokenSettings 转化为 TokenSettings
     *
     * @param oAuth2TokenSettings oAuth2TokenSettings
     * @return TokenSettings
     */
    public static TokenSettings toTokenSettings(OAuth2TokenSettings oAuth2TokenSettings) {
        return TokenSettings.builder()
                .accessTokenTimeToLive(Optional.ofNullable(oAuth2TokenSettings.getAccessTokenTimeToLive()).orElse(Duration.ofMinutes(5)))
                .refreshTokenTimeToLive(Optional.ofNullable(oAuth2TokenSettings.getRefreshTokenTimeToLive()).orElse(Duration.ofMinutes(30)))
                .accessTokenFormat(Optional.ofNullable(oAuth2TokenSettings.getTokenFormat())
                        .map(OAuth2TokenFormat::new)
                        .orElse(OAuth2TokenFormat.SELF_CONTAINED))
                .reuseRefreshTokens(oAuth2TokenSettings.getReuseRefreshTokens())
                .idTokenSignatureAlgorithm(Optional.ofNullable(oAuth2TokenSettings.getIdTokenSignatureAlgorithm())
                        .map(SignatureAlgorithm::from)
                        .orElse(SignatureAlgorithm.RS256))
                .build();
    }

    /**
     * oAuth2ClientSettings 转化为 ClientSettings
     *
     * @param oAuth2ClientSettings oAuth2ClientSettings
     * @return ClientSettings
     */
    public static ClientSettings toClientSettings(OAuth2ClientSettings oAuth2ClientSettings) {
        ClientSettings.Builder builder = ClientSettings.builder()
                .requireProofKey(oAuth2ClientSettings.getRequireProofKey())
                .requireAuthorizationConsent(oAuth2ClientSettings.getRequireAuthorizationConsent());
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.from(oAuth2ClientSettings.getSigningAlgorithm());
        JwsAlgorithm jwsAlgorithm = signatureAlgorithm == null ? MacAlgorithm.from(oAuth2ClientSettings.getSigningAlgorithm()) : signatureAlgorithm;
        if (jwsAlgorithm != null) {
            builder.tokenEndpointAuthenticationSigningAlgorithm(jwsAlgorithm);
        }
        if (StringUtils.hasText(oAuth2ClientSettings.getJwkSetUrl())) {
            builder.jwkSetUrl(oAuth2ClientSettings.getJwkSetUrl());
        }
        return builder.build();
    }


    /**
     * clientSettings 转换为 OAuth2ClientSettings
     *
     * @param clientSettings clientSettings
     * @return OAuth2ClientSettings
     */
    public static OAuth2ClientSettings fromClientSettings(ClientSettings clientSettings) {
        OAuth2ClientSettings oAuth2ClientSettings = new OAuth2ClientSettings();
        oAuth2ClientSettings.setRequireProofKey(clientSettings.isRequireProofKey());
        oAuth2ClientSettings.setRequireAuthorizationConsent(clientSettings.isRequireAuthorizationConsent());
        oAuth2ClientSettings.setJwkSetUrl(clientSettings.getJwkSetUrl());
        JwsAlgorithm algorithm = clientSettings.getTokenEndpointAuthenticationSigningAlgorithm();
        if (algorithm != null) {
            oAuth2ClientSettings.setSigningAlgorithm(algorithm.getName());
        }
        return oAuth2ClientSettings;
    }

    /**
     * tokenSettings 转换为  OAuth2TokenSettings
     *
     * @param tokenSettings tokenSettings
     * @return OAuth2TokenSettings
     */
    public static OAuth2TokenSettings fromTokenSettings(TokenSettings tokenSettings) {
        OAuth2TokenSettings oAuth2TokenSettings = new OAuth2TokenSettings();
        oAuth2TokenSettings.setAccessTokenTimeToLive(tokenSettings.getAccessTokenTimeToLive());
        oAuth2TokenSettings.setTokenFormat(tokenSettings.getAccessTokenFormat().getValue());
        oAuth2TokenSettings.setReuseRefreshTokens(tokenSettings.isReuseRefreshTokens());
        oAuth2TokenSettings.setRefreshTokenTimeToLive(tokenSettings.getRefreshTokenTimeToLive());
        oAuth2TokenSettings.setIdTokenSignatureAlgorithm(tokenSettings.getIdTokenSignatureAlgorithm().getName());
        return oAuth2TokenSettings;
    }

    public static void main(String[] args) {
        SignatureAlgorithm hs256 = SignatureAlgorithm.from("HS256");
        MacAlgorithm hs2561 = MacAlgorithm.from("HS256");
        System.out.println(hs256);
        System.out.println(hs2561);
    }

}