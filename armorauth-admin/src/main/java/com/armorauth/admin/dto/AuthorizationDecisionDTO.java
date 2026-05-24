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

import java.util.List;
import java.util.Map;

public class AuthorizationDecisionDTO {

    public record CheckRequest(
            String userId,
            String username,
            String permissionCode,
            String resourceType,
            String action,
            Map<String, Object> context
    ) {}

    public record CheckResponse(
            Boolean allowed,
            String reason,
            String userId,
            String username,
            List<String> roles,
            List<String> permissions,
            Map<String, Object> actionAttributes
    ) {}
}
