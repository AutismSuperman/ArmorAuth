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
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

/**
 * 用户DTO
 *
 * @author fulin
 * @since 2026-05-23
 */
public class UserDTO {

    /**
     * 创建用户请求
     */
    public record CreateRequest(
            @NotBlank(message = "username不能为空")
            String username,
            @NotBlank(message = "password不能为空")
            String password,
            @NotBlank(message = "displayName不能为空")
            String displayName,
            String email,
            String phone,
            String avatar,
            Boolean emailVerified,
            Boolean phoneVerified,
            String profile
    ) {
    }

    /**
     * 更新用户请求
     */
    public record UpdateRequest(
            String displayName,
            String email,
            String phone,
            String avatar,
            Boolean emailVerified,
            Boolean phoneVerified,
            String profile
    ) {
    }

    /**
     * 用户响应
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Response(
            String id,
            String username,
            String displayName,
            String email,
            String phone,
            String avatar,
            Integer status,
            Boolean emailVerified,
            Boolean phoneVerified,
            Instant lockedUntil,
            Instant createTime,
            Instant lastLoginTime,
            List<String> roles,
            String profile
    ) {
    }

    /**
     * 用户状态变更请求
     */
    public record StatusRequest(
            @NotNull(message = "status不能为空")
            Integer status
    ) {
    }

    /**
     * 重置密码请求
     */
    public record ResetPasswordRequest(
            @NotBlank(message = "newPassword不能为空")
            String newPassword
    ) {
    }

    /**
     * 用户锁定请求
     */
    public record LockRequest(
            @NotNull(message = "durationMinutes不能为空")
            Integer durationMinutes
    ) {
    }
}
