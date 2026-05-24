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
import java.util.Map;

public class IdentityProviderDTO {

    public record CreateRequest(
            String providerName,
            String providerType,
            String registrationId,
            String clientId,
            String clientSecret,
            String authorizationUri,
            String tokenUri,
            String userinfoUri,
            String jwkSetUri,
            String samlEntityId,
            String samlSsoUrl,
            String samlSloUrl,
            String samlX509Certificate,
            String samlMetadataUrl,
            String samlSpEntityId,
            String samlAcsUrl,
            String samlNameIdFormat,
            String ldapUrl,
            String ldapBaseDn,
            String ldapBindDn,
            String ldapBindPassword,
            String ldapUserSearchBase,
            String ldapUserSearchFilter,
            String ldapUsernameAttribute,
            String ldapEmailAttribute,
            String ldapPhoneAttribute,
            String ldapDisplayNameAttribute,
            String ldapGroupAttribute,
            Boolean ldapUseSsl,
            Boolean ldapStartTls,
            Integer ldapPageSize,
            String scopes,
            String attributeMapping,
            String linkingStrategy,
            Integer displayOrder
    ) {}

    public record UpdateRequest(
            String providerName,
            String clientId,
            String clientSecret,
            String authorizationUri,
            String tokenUri,
            String userinfoUri,
            String jwkSetUri,
            String samlEntityId,
            String samlSsoUrl,
            String samlSloUrl,
            String samlX509Certificate,
            String samlMetadataUrl,
            String samlSpEntityId,
            String samlAcsUrl,
            String samlNameIdFormat,
            String ldapUrl,
            String ldapBaseDn,
            String ldapBindDn,
            String ldapBindPassword,
            String ldapUserSearchBase,
            String ldapUserSearchFilter,
            String ldapUsernameAttribute,
            String ldapEmailAttribute,
            String ldapPhoneAttribute,
            String ldapDisplayNameAttribute,
            String ldapGroupAttribute,
            Boolean ldapUseSsl,
            Boolean ldapStartTls,
            Integer ldapPageSize,
            String scopes,
            String attributeMapping,
            String linkingStrategy,
            Integer displayOrder
    ) {}

    public record Response(
            String id,
            String providerName,
            String providerType,
            String registrationId,
            String clientId,
            String authorizationUri,
            String tokenUri,
            String userinfoUri,
            String jwkSetUri,
            String samlEntityId,
            String samlSsoUrl,
            String samlSloUrl,
            String samlX509Certificate,
            String samlMetadataUrl,
            String samlSpEntityId,
            String samlAcsUrl,
            String samlNameIdFormat,
            String ldapUrl,
            String ldapBaseDn,
            String ldapBindDn,
            Boolean ldapBindPasswordConfigured,
            String ldapUserSearchBase,
            String ldapUserSearchFilter,
            String ldapUsernameAttribute,
            String ldapEmailAttribute,
            String ldapPhoneAttribute,
            String ldapDisplayNameAttribute,
            String ldapGroupAttribute,
            Boolean ldapUseSsl,
            Boolean ldapStartTls,
            Integer ldapPageSize,
            String scopes,
            String attributeMapping,
            String linkingStrategy,
            Integer displayOrder,
            Boolean enabled,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record TestResponse(
            Boolean success,
            String message,
            Map<String, Object> checks
    ) {}

    public record LdapSyncRequest(
            Boolean dryRun,
            Integer maxResults
    ) {}

    public record LdapSyncResponse(
            String providerId,
            String providerName,
            Boolean dryRun,
            Integer scanned,
            Integer wouldCreate,
            Integer wouldUpdate,
            Integer created,
            Integer updated,
            Integer skipped,
            Integer failed,
            String message,
            Map<String, Object> samples
    ) {}
}
