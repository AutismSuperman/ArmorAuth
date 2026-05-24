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

import java.time.Instant;

/**
 * JWK 密钥 DTO
 *
 * @author fulin
 * @since 2026-05-23
 */
public class JwkDTO {

    public record Response(
            String id,
            String kid,
            String keyType,
            String algorithm,
            String status,
            Instant createdAt,
            Instant expiresAt
    ) {
    }

    public record RotateResponse(
            String kid,
            String message
    ) {
    }

    public record RotateRequest(
            String algorithm
    ) {
    }
}
