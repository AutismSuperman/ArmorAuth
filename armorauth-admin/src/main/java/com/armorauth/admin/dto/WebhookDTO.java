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

public class WebhookDTO {

    public record CreateRequest(
            String name,
            String url,
            String secret,
            String eventTypes
    ) {}

    public record UpdateRequest(
            String name,
            String url,
            String secret,
            String eventTypes
    ) {}

    public record Response(
            String id,
            String name,
            String url,
            String eventTypes,
            Boolean enabled,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record DeliveryResponse(
            String id,
            String endpointId,
            String eventType,
            String payload,
            Integer responseStatus,
            Boolean success,
            Integer retryCount,
            Instant createdAt
    ) {}
}
