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
package com.armorauth.admin.listener;

import com.armorauth.admin.service.AuditEventService;
import com.armorauth.common.audit.SecurityAuditEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 安全审计事件监听器，将事件持久化到数据库
 *
 * @author fulin
 * @since 2026-05-23
 */
@Component
public class SecurityAuditEventListener {

    private final AuditEventService auditEventService;

    public SecurityAuditEventListener(AuditEventService auditEventService) {
        this.auditEventService = auditEventService;
    }

    @Async
    @EventListener
    public void onSecurityAuditEvent(SecurityAuditEvent event) {
        auditEventService.record(
                event.getEventType(),
                event.getPrincipalName(),
                event.getResourceType(),
                event.getResourceId(),
                event.getDetail(),
                event.getIpAddress()
        );
    }
}
