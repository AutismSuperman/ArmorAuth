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

import com.armorauth.admin.dto.ApplicationDTO;
import com.armorauth.admin.service.ApplicationService;
import com.armorauth.common.response.ApiResponse;
import com.armorauth.common.response.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

/**
 * 应用管理API
 *
 * @author fulin
 * @since 2026-05-23
 */
@RestController
@RequestMapping("/api/admin/v1/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    /**
     * 获取应用列表
     */
    @GetMapping
    public ApiResponse<PageResponse<ApplicationDTO.Response>> list(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sort", defaultValue = "clientIdIssuedAt") String sort,
            @RequestParam(name = "direction", defaultValue = "DESC") String direction,
            @RequestParam(name = "tenantId", required = false) String tenantId) {
        Sort sortObj = Sort.by(Sort.Direction.fromString(direction), sort);
        Page<ApplicationDTO.Response> result = applicationService.listApplications(tenantId, PageRequest.of(page, size, sortObj));
        PageResponse<ApplicationDTO.Response> pageResponse = new PageResponse<>(
                result.getContent(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize()
        );
        return ApiResponse.ok(pageResponse);
    }

    /**
     * 获取应用详情
     */
    @GetMapping("/{id}")
    public ApiResponse<ApplicationDTO.Response> get(@PathVariable(name = "id") String id) {
        return ApiResponse.ok(applicationService.getApplication(id));
    }

    /**
     * 创建应用
     */
    @PostMapping
    public ApiResponse<ApplicationDTO.Response> create(@Valid @RequestBody ApplicationDTO.CreateRequest request) {
        return ApiResponse.ok(applicationService.createApplication(request));
    }

    /**
     * 更新应用
     */
    @PutMapping("/{id}")
    public ApiResponse<ApplicationDTO.Response> update(
            @PathVariable(name = "id") String id,
            @Valid @RequestBody ApplicationDTO.UpdateRequest request) {
        return ApiResponse.ok(applicationService.updateApplication(id, request));
    }

    /**
     * 重置客户端密钥
     */
    @PostMapping("/{id}/secret:rotate")
    public ApiResponse<ApplicationDTO.SecretResponse> rotateSecret(@PathVariable(name = "id") String id) {
        return ApiResponse.ok(applicationService.rotateSecret(id));
    }

    /**
     * 启用/禁用应用
     */
    @PatchMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(
            @PathVariable(name = "id") String id,
            @Valid @RequestBody ApplicationDTO.StatusRequest request) {
        Boolean enabled = request.enabled();
        if (enabled == null && request.status() != null) {
            enabled = "enabled".equalsIgnoreCase(request.status())
                    || "true".equalsIgnoreCase(request.status());
        }
        if (enabled == null) {
            throw new IllegalArgumentException("status 或 enabled 不能为空");
        }
        applicationService.updateApplicationStatus(id, enabled);
        return ApiResponse.ok();
    }

    /**
     * 删除应用
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable(name = "id") String id) {
        applicationService.deleteApplication(id);
        return ApiResponse.ok();
    }
}
