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

import com.armorauth.admin.service.AuditEventService;
import com.armorauth.common.response.ApiResponse;
import com.armorauth.common.response.PageResponse;
import com.armorauth.data.entity.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

/**
 * 审计日志API
 *
 * @author fulin
 * @since 2026-05-23
 */
@RestController
@RequestMapping("/api/admin/v1/audit-events")
public class AuditController {

    private final AuditEventService auditEventService;

    public AuditController(AuditEventService auditEventService) {
        this.auditEventService = auditEventService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AuditEvent>> list(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "eventType", required = false) String eventType,
            @RequestParam(name = "principalName", required = false) String principalName) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditEvent> result;
        if (eventType != null) {
            result = auditEventService.listByEventType(eventType, pageRequest);
        } else if (principalName != null) {
            result = auditEventService.listByPrincipal(principalName, pageRequest);
        } else {
            result = auditEventService.listEvents(pageRequest);
        }
        PageResponse<AuditEvent> pageResponse = new PageResponse<>(
                result.getContent(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize()
        );
        return ApiResponse.ok(pageResponse);
    }
}
