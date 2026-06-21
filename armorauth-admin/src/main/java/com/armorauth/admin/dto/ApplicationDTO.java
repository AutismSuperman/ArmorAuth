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
package com.armorauth.admin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;

/**
 * 应用/OAuth2 Client DTO
 *
 * @author fulin
 * @since 2026-05-23
 */
public class ApplicationDTO {

    /**
     * 创建应用请求
     */
    public record CreateRequest(
            String tenantId,
            @NotBlank(message = "clientName不能为空")
            String clientName,
            @NotBlank(message = "clientAuthenticationMethods不能为空")
            String clientAuthenticationMethods,
            @NotBlank(message = "authorizationGrantTypes不能为空")
            String authorizationGrantTypes,
            String redirectUris,
            String postLogoutRedirectUris,
            List<String> scopes,
            ClientSettingsDTO clientSettings,
            TokenSettingsDTO tokenSettings,
            Boolean mfaRequired,
            Boolean dynamicClientRegistrar
    ) {
    }

    /**
     * 更新应用请求
     */
    public record UpdateRequest(
            String tenantId,
            String clientName,
            String clientAuthenticationMethods,
            String authorizationGrantTypes,
            String redirectUris,
            String postLogoutRedirectUris,
            List<String> scopes,
            ClientSettingsDTO clientSettings,
            TokenSettingsDTO tokenSettings,
            Boolean mfaRequired,
            Boolean dynamicClientRegistrar
    ) {
    }

    /**
     * 应用响应
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Response(
            String id,
            String tenantId,
            String tenantCode,
            String issuerPath,
            String clientId,
            String clientName,
            String clientAuthenticationMethods,
            String authorizationGrantTypes,
            String redirectUris,
            String postLogoutRedirectUris,
            List<String> scopes,
            ClientSettingsDTO clientSettings,
            TokenSettingsDTO tokenSettings,
            Instant clientIdIssuedAt,
            Instant clientSecretExpiresAt,
            Boolean enabled,
            Boolean mfaRequired,
            String registrationSource,
            Boolean dynamicClientRegistrar,
            String clientSecret
    ) {
    }

    /**
     * 客户端设置
     */
    public record ClientSettingsDTO(
            String jwkSetUrl,
            Boolean requireAuthorizationConsent,
            Boolean requireProofKey,
            String signingAlgorithm,
            String x509CertificateSubjectDN,
            Boolean dpopEnabled,
            Boolean dpopRequired,
            String dpopAllowedAlgorithms
    ) {
    }

    /**
     * Token设置
     */
    public record TokenSettingsDTO(
            Long accessTokenTimeToLiveSeconds,
            Long refreshTokenTimeToLiveSeconds,
            Long deviceCodeTimeToLiveSeconds,
            Long authorizationCodeTimeToLiveSeconds,
            String idTokenSignatureAlgorithm,
            Boolean reuseRefreshTokens,
            String tokenFormat,
            Boolean x509CertificateBoundAccessTokens
    ) {
    }

    /**
     * Secret重置响应
     */
    public record SecretResponse(
            String clientId,
            String clientSecret
    ) {
    }

    /**
     * 状态变更请求
     */
    public record StatusRequest(
            String status,
            Boolean enabled
    ) {
    }
}
