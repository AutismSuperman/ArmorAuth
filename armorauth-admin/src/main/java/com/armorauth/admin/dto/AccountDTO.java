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
 * 账户自助 DTO
 *
 * @author fulin
 * @since 2026-05-23
 */
public class AccountDTO {

    public record ProfileResponse(
            String id,
            String username,
            String displayName,
            String email,
            String phone,
            String avatar,
            Boolean emailVerified,
            Boolean phoneVerified,
            Instant lastLoginTime,
            String profile
    ) {
    }

    public record UpdateProfileRequest(
            String displayName,
            String email,
            String phone,
            String avatar,
            String profile
    ) {
    }

    public record ChangePasswordRequest(
            @NotBlank(message = "旧密码不能为空")
            String oldPassword,
            @NotBlank(message = "新密码不能为空")
            String newPassword
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FactorResponse(
            String id,
            String factorType,
            String name,
            Boolean verified,
            Boolean enabled,
            Instant createdAt,
            Instant lastUsedAt,
            String runtimeSupport
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record TotpSetupResponse(
            String factorId,
            String secret,
            String uri,
            List<String> recoveryCodes
    ) {
    }

    public record VerifyFactorRequest(
            @NotBlank(message = "验证码不能为空")
            String code
    ) {
    }

    public record PasskeyBeginRegistrationRequest(
            String name,
            String rpId,
            String rpName
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PasskeyBeginRegistrationResponse(
            String factorId,
            String challenge,
            String rpId,
            String rpName,
            Long timeoutMillis,
            String userHandle,
            String username,
            String displayName,
            List<String> excludeCredentialIds,
            List<String> pubKeyCredParams,
            String attestation,
            String verificationMode
    ) {
    }

    public record PasskeyFinishRegistrationRequest(
            @NotBlank(message = "challenge 不能为空")
            String challenge,
            String credentialId,
            String publicKey,
            String clientDataJSON,
            String attestationObject,
            String rpId,
            String name,
            Long signCount,
            String transports,
            String aaguid,
            String userHandle,
            Boolean backupEligible,
            Boolean backupState
    ) {
    }
}
