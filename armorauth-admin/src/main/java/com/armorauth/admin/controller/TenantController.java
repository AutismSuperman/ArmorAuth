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

import com.armorauth.admin.dto.TenantDTO;
import com.armorauth.admin.service.TenantService;
import com.armorauth.common.response.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/v1/tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ApiResponse<Page<TenantDTO.Response>> listTenants(Pageable pageable) {
        return ApiResponse.ok(tenantService.listTenants(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ApiResponse<TenantDTO.Response> getTenant(@PathVariable String id) {
        return ApiResponse.ok(tenantService.getTenant(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<TenantDTO.Response> createTenant(@RequestBody TenantDTO.CreateRequest request) {
        return ApiResponse.ok(tenantService.createTenant(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<TenantDTO.Response> updateTenant(@PathVariable String id,
                                                         @RequestBody TenantDTO.UpdateRequest request) {
        return ApiResponse.ok(tenantService.updateTenant(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<Void> updateTenantStatus(@PathVariable String id, @RequestParam Boolean enabled) {
        tenantService.updateTenantStatus(id, enabled);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<Void> deleteTenant(@PathVariable String id) {
        tenantService.deleteTenant(id);
        return ApiResponse.ok();
    }
}
