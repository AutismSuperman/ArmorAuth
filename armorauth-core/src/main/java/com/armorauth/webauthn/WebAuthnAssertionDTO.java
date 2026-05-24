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
package com.armorauth.webauthn;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

public class WebAuthnAssertionDTO {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PublicKeyCredentialDescriptor(
            String type,
            String id,
            List<String> transports
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AssertionOptionsResponse(
            String challenge,
            String rpId,
            Long timeoutMillis,
            String userVerification,
            List<PublicKeyCredentialDescriptor> allowCredentials
    ) {
    }

    public record AssertionFinishRequest(
            String credentialId,
            String clientDataJSON,
            String authenticatorData,
            String signature,
            String userHandle
    ) {
    }

    public record PasswordlessOptionsRequest(
            String username
    ) {
    }

    public record AssertionFinishResponse(
            Boolean success,
            String redirectUrl,
            String factorId,
            String runtimeSupport
    ) {
    }

    public record VerifiedAssertion(
            String factorId,
            String credentialId,
            long signCount
    ) {
    }

    public record VerifiedPasswordlessAssertion(
            String username,
            String factorId,
            String credentialId,
            long signCount
    ) {
    }
}
