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

import com.armorauth.admin.dto.ScopeDTO;
import com.armorauth.admin.service.ScopeService;
import com.armorauth.common.response.ApiResponse;
import com.armorauth.common.response.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/v1/scopes")
public class ScopeController {

    private final ScopeService scopeService;

    public ScopeController(ScopeService scopeService) {
        this.scopeService = scopeService;
    }

    @GetMapping
    public ApiResponse<PageResponse<ScopeDTO.Response>> listScopes(
            @RequestParam(name = "clientId", required = false) String clientId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        Page<ScopeDTO.Response> result = scopeService.listScopes(
                clientId, PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "scope")));
        return ApiResponse.ok(new PageResponse<>(
                result.getContent(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize()
        ));
    }

    @PostMapping
    public ApiResponse<ScopeDTO.Response> createScope(@Valid @RequestBody ScopeDTO.CreateRequest request) {
        return ApiResponse.ok(scopeService.createScope(request));
    }

    @PutMapping
    public ApiResponse<ScopeDTO.Response> updateScope(
            @RequestParam(name = "clientId") String clientId,
            @RequestParam(name = "scope") String scope,
            @RequestBody ScopeDTO.UpdateRequest request) {
        return ApiResponse.ok(scopeService.updateScope(clientId, scope, request));
    }

    @DeleteMapping
    public ApiResponse<Void> deleteScope(
            @RequestParam(name = "clientId") String clientId,
            @RequestParam(name = "scope") String scope) {
        scopeService.deleteScope(clientId, scope);
        return ApiResponse.ok();
    }
}
