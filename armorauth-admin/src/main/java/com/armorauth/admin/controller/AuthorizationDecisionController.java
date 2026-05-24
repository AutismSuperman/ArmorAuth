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

import com.armorauth.admin.dto.AuthorizationDecisionDTO;
import com.armorauth.admin.service.AuthorizationDecisionService;
import com.armorauth.common.response.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/v1/authorization")
public class AuthorizationDecisionController {

    private final AuthorizationDecisionService authorizationDecisionService;

    public AuthorizationDecisionController(AuthorizationDecisionService authorizationDecisionService) {
        this.authorizationDecisionService = authorizationDecisionService;
    }

    @PostMapping("/check")
    public ApiResponse<AuthorizationDecisionDTO.CheckResponse> check(
            @RequestBody AuthorizationDecisionDTO.CheckRequest request) {
        return ApiResponse.ok(authorizationDecisionService.check(request));
    }
}
