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

import com.armorauth.admin.dto.IdentityProviderDTO;
import com.armorauth.admin.service.IdentityProviderService;
import com.armorauth.common.response.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/v1/identity-providers")
public class IdentityProviderController {

    private final IdentityProviderService idpService;

    public IdentityProviderController(IdentityProviderService idpService) {
        this.idpService = idpService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'APPLICATION_ADMIN')")
    public ApiResponse<Page<IdentityProviderDTO.Response>> listProviders(
            Pageable pageable,
            @RequestParam(name = "source", required = false) String source) {
        return ApiResponse.ok(idpService.listProviders(pageable, source));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'APPLICATION_ADMIN')")
    public ApiResponse<IdentityProviderDTO.Response> getProvider(@PathVariable String id) {
        return ApiResponse.ok(idpService.getProvider(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<IdentityProviderDTO.Response> createProvider(@RequestBody IdentityProviderDTO.CreateRequest request) {
        return ApiResponse.ok(idpService.createProvider(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<IdentityProviderDTO.Response> updateProvider(@PathVariable String id,
                                                                     @RequestBody IdentityProviderDTO.UpdateRequest request) {
        return ApiResponse.ok(idpService.updateProvider(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<Void> updateProviderStatus(@PathVariable String id, @RequestParam Boolean enabled) {
        idpService.updateProviderStatus(id, enabled);
        return ApiResponse.ok();
    }

    @PatchMapping("/{id}/login-display")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<IdentityProviderDTO.Response> updateProviderLoginDisplay(
            @PathVariable String id,
            @RequestBody IdentityProviderDTO.LoginDisplayRequest request) {
        return ApiResponse.ok(idpService.updateProviderLoginDisplay(
                id, request != null ? request.displayOnLogin() : null));
    }

    @PostMapping("/{id}:test")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'APPLICATION_ADMIN')")
    public ApiResponse<IdentityProviderDTO.TestResponse> testProvider(
            @PathVariable String id,
            @RequestParam(name = "probeRemote", defaultValue = "false") boolean probeRemote) {
        return ApiResponse.ok(idpService.testProvider(id, probeRemote));
    }

    @PostMapping("/{id}:sync-users")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'USER_ADMIN')")
    public ApiResponse<IdentityProviderDTO.LdapSyncResponse> syncLdapUsers(
            @PathVariable String id,
            @RequestBody(required = false) IdentityProviderDTO.LdapSyncRequest request) {
        return ApiResponse.ok(idpService.syncLdapUsers(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<Void> deleteProvider(@PathVariable String id) {
        idpService.deleteProvider(id);
        return ApiResponse.ok();
    }
}
