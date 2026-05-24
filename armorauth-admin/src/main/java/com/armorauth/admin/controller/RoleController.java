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
package com.armorauth.admin.controller;

import com.armorauth.admin.dto.RoleDTO;
import com.armorauth.admin.service.RoleService;
import com.armorauth.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理API
 *
 * @author fulin
 * @since 2026-05-23
 */
@RestController
@RequestMapping("/api/admin/v1")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping("/roles")
    public ApiResponse<List<RoleDTO.Response>> listRoles() {
        return ApiResponse.ok(roleService.listRoles());
    }

    @PostMapping("/roles")
    public ApiResponse<RoleDTO.Response> createRole(@Valid @RequestBody RoleDTO.CreateRequest request) {
        return ApiResponse.ok(roleService.createRole(request));
    }

    @DeleteMapping("/roles/{id}")
    public ApiResponse<Void> deleteRole(@PathVariable(name = "id") String id) {
        roleService.deleteRole(id);
        return ApiResponse.ok();
    }

    @PostMapping("/role-bindings")
    public ApiResponse<Void> bindRole(@Valid @RequestBody RoleDTO.BindRequest request) {
        roleService.bindUserRole(request.userId(), request.roleId());
        return ApiResponse.ok();
    }

    @GetMapping("/role-bindings")
    public ApiResponse<List<RoleDTO.BindingResponse>> listRoleBindings(
            @RequestParam(name = "userId") String userId) {
        return ApiResponse.ok(roleService.listUserRoleBindings(userId));
    }

    @DeleteMapping("/role-bindings/{id}")
    public ApiResponse<Void> unbindRoleById(@PathVariable(name = "id") String id) {
        roleService.unbindUserRole(id);
        return ApiResponse.ok();
    }

    @DeleteMapping("/role-bindings")
    public ApiResponse<Void> unbindRole(
            @RequestParam(name = "userId") String userId,
            @RequestParam(name = "roleId") String roleId) {
        roleService.unbindUserRole(userId, roleId);
        return ApiResponse.ok();
    }
}
