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
import java.util.List;

public class OrganizationDTO {

    public record CreateRequest(
            String tenantId,
            String orgCode,
            String orgName,
            String description,
            String logo,
            String parentId
    ) {}

    public record UpdateRequest(
            String orgName,
            String description,
            String logo,
            String parentId
    ) {}

    public record Response(
            String id,
            String tenantId,
            String orgCode,
            String orgName,
            String description,
            String logo,
            String parentId,
            Boolean enabled,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record MemberRequest(
            String userId,
            String orgRole
    ) {}

    public record MemberResponse(
            String id,
            String orgId,
            String userId,
            String orgRole,
            Instant createdAt
    ) {}
}
