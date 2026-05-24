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

import com.armorauth.admin.service.PermissionService;
import com.armorauth.common.response.ApiResponse;
import com.armorauth.common.response.PageResponse;
import com.armorauth.data.entity.Permission;
import com.armorauth.data.entity.RolePermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限管理API
 *
 * @author fulin
 * @since 2026-05-23
 */
@RestController
@RequestMapping("/api/admin/v1")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping("/permissions")
    public ApiResponse<PageResponse<Permission>> listPermissions(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "permissionCode"));
        Page<Permission> result = permissionService.listPermissions(pageRequest);
        PageResponse<Permission> pageResponse = new PageResponse<>(
                result.getContent(), result.getTotalElements(),
                result.getTotalPages(), result.getNumber(), result.getSize());
        return ApiResponse.ok(pageResponse);
    }

    @GetMapping("/permissions/{id}")
    public ApiResponse<Permission> getPermission(@PathVariable String id) {
        return ApiResponse.ok(permissionService.getPermission(id));
    }

    @PostMapping("/permissions")
    public ApiResponse<Permission> createPermission(@RequestBody CreatePermissionRequest request) {
        Permission permission = permissionService.createPermission(
                request.permissionCode(), request.permissionName(),
                request.resourceType(), request.action(), request.description());
        return ApiResponse.ok(permission);
    }

    @DeleteMapping("/permissions/{id}")
    public ApiResponse<Void> deletePermission(@PathVariable String id) {
        permissionService.deletePermission(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/roles/{roleId}/permissions")
    public ApiResponse<List<RolePermission>> getRolePermissions(@PathVariable String roleId) {
        return ApiResponse.ok(permissionService.getRolePermissions(roleId));
    }

    @PostMapping("/roles/{roleId}/permissions/{permissionId}")
    public ApiResponse<Void> assignPermission(@PathVariable String roleId, @PathVariable String permissionId) {
        permissionService.assignPermissionToRole(roleId, permissionId);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/roles/{roleId}/permissions/{permissionId}")
    public ApiResponse<Void> removePermission(@PathVariable String roleId, @PathVariable String permissionId) {
        permissionService.removePermissionFromRole(roleId, permissionId);
        return ApiResponse.ok(null);
    }

    public record CreatePermissionRequest(
            String permissionCode,
            String permissionName,
            String resourceType,
            String action,
            String description
    ) {}
}
