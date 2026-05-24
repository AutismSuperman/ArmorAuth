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

import jakarta.validation.constraints.NotBlank;

public class ScopeDTO {

    public record CreateRequest(
            @NotBlank(message = "clientId不能为空")
            String clientId,
            @NotBlank(message = "scope不能为空")
            String scope,
            String description
    ) {}

    public record UpdateRequest(
            String description
    ) {}

    public record Response(
            String clientId,
            String scope,
            String description
    ) {}
}
