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

import com.armorauth.admin.dto.FederatedBindingDTO;
import com.armorauth.admin.service.FederatedBindingAdminService;
import com.armorauth.common.response.ApiResponse;
import com.armorauth.common.response.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/v1/federated-bindings")
public class FederatedBindingController {

    private final FederatedBindingAdminService bindingService;

    public FederatedBindingController(FederatedBindingAdminService bindingService) {
        this.bindingService = bindingService;
    }

    @GetMapping
    public ApiResponse<PageResponse<FederatedBindingDTO.Response>> listBindings(
            @RequestParam(name = "userId", required = false) String userId,
            @RequestParam(name = "registrationId", required = false) String registrationId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        Page<FederatedBindingDTO.Response> result = bindingService.listBindings(
                userId,
                registrationId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lastLoginTime"))
        );
        return ApiResponse.ok(new PageResponse<>(
                result.getContent(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize()
        ));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteBinding(@PathVariable(name = "id") String id) {
        bindingService.deleteBinding(id);
        return ApiResponse.ok();
    }
}
