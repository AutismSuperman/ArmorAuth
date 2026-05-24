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

import com.armorauth.admin.dto.SecretProtectionDTO;
import com.armorauth.admin.service.SecretRekeyService;
import com.armorauth.common.response.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Secret protection operations.
 */
@RestController
@RequestMapping("/api/admin/v1/secret-protection")
public class SecretProtectionController {

    private final SecretRekeyService secretRekeyService;

    public SecretProtectionController(SecretRekeyService secretRekeyService) {
        this.secretRekeyService = secretRekeyService;
    }

    /**
     * Re-encrypts stored secrets with the active crypto key. Defaults to dry-run.
     */
    @PostMapping("/rekey")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<SecretProtectionDTO.RekeyResponse> rekey(
            @RequestBody(required = false) SecretProtectionDTO.RekeyRequest request) {
        boolean dryRun = request == null || !Boolean.FALSE.equals(request.dryRun());
        return ApiResponse.ok(secretRekeyService.rekey(dryRun));
    }
}
