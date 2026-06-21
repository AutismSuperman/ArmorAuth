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
package com.armorauth.authorization.tenant;

import org.springframework.util.StringUtils;

import java.util.Optional;

public final class TenantIssuerContext {

    public static final String DEFAULT_TENANT_ID = "tenant-default";

    private static final ThreadLocal<TenantIssuer> CURRENT = new ThreadLocal<>();

    private TenantIssuerContext() {
    }

    public static void set(String tenantId, String tenantCode) {
        CURRENT.set(new TenantIssuer(tenantId, tenantCode));
    }

    public static Optional<TenantIssuer> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static String tenantIdOrDefault() {
        return current()
                .map(TenantIssuer::tenantId)
                .filter(StringUtils::hasText)
                .orElse(DEFAULT_TENANT_ID);
    }

    public static void clear() {
        CURRENT.remove();
    }

    public record TenantIssuer(String tenantId, String tenantCode) {
    }
}
