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

import com.armorauth.admin.dto.OrganizationDTO;
import com.armorauth.admin.service.OrganizationService;
import com.armorauth.common.response.ApiResponse;
import com.armorauth.common.response.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/v1")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping({"/organizations", "/tenants/{tenantId}/organizations"})
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ApiResponse<PageResponse<OrganizationDTO.Response>> listOrganizations(
            @PathVariable(required = false) String tenantId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sort", defaultValue = "createdAt") String sort,
            @RequestParam(name = "direction", defaultValue = "DESC") String direction) {
        Sort sortObj = Sort.by(Sort.Direction.fromString(direction), sort);
        Page<OrganizationDTO.Response> result = organizationService.listOrganizations(
                tenantId, PageRequest.of(page, size, sortObj));
        return ApiResponse.ok(new PageResponse<>(
                result.getContent(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize()
        ));
    }

    @GetMapping({"/organizations/{id}", "/tenants/{tenantId}/organizations/{id}"})
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ApiResponse<OrganizationDTO.Response> getOrganization(@PathVariable String id) {
        return ApiResponse.ok(organizationService.getOrganization(id));
    }

    @PostMapping({"/organizations", "/tenants/{tenantId}/organizations"})
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ApiResponse<OrganizationDTO.Response> createOrganization(@PathVariable(required = false) String tenantId,
                                                                     @RequestBody OrganizationDTO.CreateRequest request) {
        return ApiResponse.ok(organizationService.createOrganization(tenantId, request));
    }

    @PutMapping({"/organizations/{id}", "/tenants/{tenantId}/organizations/{id}"})
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ApiResponse<OrganizationDTO.Response> updateOrganization(@PathVariable String id,
                                                                     @RequestBody OrganizationDTO.UpdateRequest request) {
        return ApiResponse.ok(organizationService.updateOrganization(id, request));
    }

    @DeleteMapping({"/organizations/{id}", "/tenants/{tenantId}/organizations/{id}"})
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<Void> deleteOrganization(@PathVariable String id) {
        organizationService.deleteOrganization(id);
        return ApiResponse.ok();
    }

    @GetMapping({"/organizations/{id}/members", "/tenants/{tenantId}/organizations/{id}/members"})
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'USER_ADMIN')")
    public ApiResponse<List<OrganizationDTO.MemberResponse>> listMembers(@PathVariable String id) {
        return ApiResponse.ok(organizationService.listMembers(id));
    }

    @PostMapping({"/organizations/{id}/members", "/tenants/{tenantId}/organizations/{id}/members"})
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ApiResponse<OrganizationDTO.MemberResponse> addMember(@PathVariable String id,
                                                                  @RequestBody OrganizationDTO.MemberRequest request) {
        return ApiResponse.ok(organizationService.addMember(id, request));
    }

    @DeleteMapping({"/organizations/{id}/members/{userId}", "/tenants/{tenantId}/organizations/{id}/members/{userId}"})
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ApiResponse<Void> removeMember(@PathVariable String id, @PathVariable String userId) {
        organizationService.removeMember(id, userId);
        return ApiResponse.ok();
    }
}
