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
package com.armorauth.springboot.security;

import java.util.List;
import java.util.Map;

/**
 * Current authenticated ArmorAuth identity as seen by a relying Spring Boot service.
 *
 * @author fulin
 * @since 2026-06-21
 */
public record ArmorAuthCurrentUser(
        String subject,
        String username,
        String tenantId,
        List<String> organizationIds,
        List<String> organizationRoles,
        List<String> roles,
        List<String> scopes,
        List<String> permissions,
        Map<String, Object> claims) {

    public static ArmorAuthCurrentUser anonymous() {
        return new ArmorAuthCurrentUser(null, null, null, List.of(), List.of(), List.of(), List.of(), List.of(), Map.of());
    }

    public boolean authenticated() {
        return subject != null;
    }
}
