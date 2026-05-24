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

import com.armorauth.admin.dto.SessionDTO;
import com.armorauth.common.audit.AuditContext;
import com.armorauth.common.response.ApiResponse;
import com.armorauth.admin.service.AuditEventService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 会话管理 API
 *
 * @author fulin
 * @since 2026-05-23
 */
@RestController
@RequestMapping("/api/admin/v1/sessions")
public class SessionController {

    private final SessionRegistry sessionRegistry;
    private final AuditEventService auditEventService;

    public SessionController(SessionRegistry sessionRegistry,
                             AuditEventService auditEventService) {
        this.sessionRegistry = sessionRegistry;
        this.auditEventService = auditEventService;
    }

    /**
     * 获取所有活跃会话
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'AUDIT_VIEWER')")
    public ApiResponse<List<SessionDTO.Response>> listSessions() {
        List<SessionDTO.Response> sessions = sessionRegistry.getAllPrincipals().stream()
                .flatMap(principal -> {
                    String username = principal.toString();
                    return sessionRegistry.getAllSessions(principal, false).stream()
                            .map(session -> new SessionDTO.Response(
                                    session.getSessionId(),
                                    username,
                                    session.getLastRequest(),
                                    !session.isExpired()
                            ));
                })
                .toList();
        return ApiResponse.ok(sessions);
    }

    /**
     * 获取指定用户的所有会话
     */
    @GetMapping("/{username}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'USER_ADMIN', 'AUDIT_VIEWER')")
    public ApiResponse<List<SessionDTO.Response>> listUserSessions(
            @PathVariable(name = "username") String username) {
        List<SessionDTO.Response> sessions = sessionRegistry.getAllPrincipals().stream()
                .filter(p -> p.toString().equals(username))
                .flatMap(principal -> sessionRegistry.getAllSessions(principal, false).stream()
                        .map(session -> new SessionDTO.Response(
                                session.getSessionId(),
                                username,
                                session.getLastRequest(),
                                !session.isExpired()
                        )))
                .toList();
        return ApiResponse.ok(sessions);
    }

    /**
     * 强制下线指定会话
     */
    @DeleteMapping("/{sessionId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<Void> expireSession(@PathVariable(name = "sessionId") String sessionId) {
        SessionInformation session = sessionRegistry.getSessionInformation(sessionId);
        if (session != null) {
            session.expireNow();
            auditEventService.record("SESSION_EXPIRED",
                    AuditContext.getCurrentPrincipal(), "session", sessionId,
                    "强制下线会话: " + sessionId + " (用户: " + session.getPrincipal() + ")",
                    AuditContext.getClientIp());
        }
        return ApiResponse.ok();
    }
}
