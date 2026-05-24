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

import com.armorauth.admin.dto.WebhookDTO;
import com.armorauth.admin.service.WebhookService;
import com.armorauth.common.response.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/v1/webhooks")
public class WebhookController {

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<Page<WebhookDTO.Response>> listEndpoints(Pageable pageable) {
        return ApiResponse.ok(webhookService.listEndpoints(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<WebhookDTO.Response> getEndpoint(@PathVariable String id) {
        return ApiResponse.ok(webhookService.getEndpoint(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<WebhookDTO.Response> createEndpoint(@RequestBody WebhookDTO.CreateRequest request) {
        return ApiResponse.ok(webhookService.createEndpoint(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<WebhookDTO.Response> updateEndpoint(@PathVariable String id,
                                                            @RequestBody WebhookDTO.UpdateRequest request) {
        return ApiResponse.ok(webhookService.updateEndpoint(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<Void> updateStatus(@PathVariable String id, @RequestParam Boolean enabled) {
        webhookService.updateStatus(id, enabled);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<Void> deleteEndpoint(@PathVariable String id) {
        webhookService.deleteEndpoint(id);
        return ApiResponse.ok();
    }

    @GetMapping("/{id}/deliveries")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<Page<WebhookDTO.DeliveryResponse>> listDeliveries(@PathVariable String id, Pageable pageable) {
        return ApiResponse.ok(webhookService.listDeliveries(id, pageable));
    }
}
