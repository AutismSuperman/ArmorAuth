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
package com.armorauth.common.audit;

import org.springframework.context.ApplicationEvent;

/**
 * 安全审计事件，由认证/授权层发布，由 admin 模块监听并持久化
 *
 * @author fulin
 * @since 2026-05-23
 */
public class SecurityAuditEvent extends ApplicationEvent {

    private final String eventType;
    private final String principalName;
    private final String resourceType;
    private final String resourceId;
    private final String detail;
    private final String ipAddress;

    public SecurityAuditEvent(Object source, String eventType, String principalName,
                              String resourceType, String resourceId,
                              String detail, String ipAddress) {
        super(source);
        this.eventType = eventType;
        this.principalName = principalName;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.detail = detail;
        this.ipAddress = ipAddress;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPrincipalName() {
        return principalName;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getDetail() {
        return detail;
    }

    public String getIpAddress() {
        return ipAddress;
    }
}
