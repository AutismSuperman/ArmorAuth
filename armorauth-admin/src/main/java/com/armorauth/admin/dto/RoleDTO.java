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

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;

/**
 * 角色DTO
 *
 * @author fulin
 * @since 2026-05-23
 */
public class RoleDTO {

    /**
     * 创建角色请求
     */
    public record CreateRequest(
            @NotBlank(message = "roleCode不能为空")
            String roleCode,
            @NotBlank(message = "roleName不能为空")
            String roleName,
            String description
    ) {
    }

    /**
     * 角色响应
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Response(
            String id,
            String roleCode,
            String roleName,
            String description,
            Boolean builtin
    ) {
    }

    /**
     * 角色绑定请求
     */
    public record BindRequest(
            @NotBlank(message = "userId不能为空")
            String userId,
            @NotBlank(message = "roleId不能为空")
            String roleId
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record BindingResponse(
            String id,
            String userId,
            String roleId,
            String roleCode,
            String roleName
    ) {
    }
}
