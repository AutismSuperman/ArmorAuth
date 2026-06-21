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

public class TenantDTO {

    public record CreateRequest(
            String tenantCode,
            String tenantName,
            String description,
            String logo,
            String primaryColor,
            String loginPageTitle,
            String privacyPolicyUrl,
            String termsOfServiceUrl
    ) {}

    public record UpdateRequest(
            String tenantName,
            String description,
            String logo,
            String primaryColor,
            String customDomain,
            String loginPageTitle,
            String privacyPolicyUrl,
            String termsOfServiceUrl
    ) {}

    public record Response(
            String id,
            String tenantCode,
            String tenantName,
            String description,
            String logo,
            String primaryColor,
            String customDomain,
            String loginPageTitle,
            String privacyPolicyUrl,
            String termsOfServiceUrl,
            Boolean enabled,
            String issuerPath,
            Boolean pathIssuerEnabled,
            String customDomainIssuer,
            Instant createdAt,
            Instant updatedAt
    ) {}
}
