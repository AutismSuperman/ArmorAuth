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
package com.armorauth.admin.service;

import com.armorauth.data.entity.AuditEvent;
import com.armorauth.data.repository.AuditEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * 审计事件服务
 *
 * @author fulin
 * @since 2026-05-23
 */
@Service
public class AuditEventService {

    private final AuditEventRepository auditEventRepository;

    public AuditEventService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional
    public void record(String eventType, String principalName, String resourceType,
                       String resourceId, String detail, String ipAddress) {
        AuditEvent event = new AuditEvent();
        event.setEventType(eventType);
        event.setPrincipalName(principalName);
        event.setResourceType(resourceType);
        event.setResourceId(resourceId);
        event.setDetail(detail);
        event.setIpAddress(ipAddress);
        event.setCreatedAt(Instant.now());
        auditEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public Page<AuditEvent> listEvents(Pageable pageable) {
        return auditEventRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditEvent> listByEventType(String eventType, Pageable pageable) {
        return auditEventRepository.findByEventType(eventType, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditEvent> listByPrincipal(String principalName, Pageable pageable) {
        return auditEventRepository.findByPrincipalName(principalName, pageable);
    }
}
