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

import com.armorauth.admin.dto.ScopeDTO;
import com.armorauth.common.audit.AuditContext;
import com.armorauth.common.exception.ResourceNotFoundException;
import com.armorauth.common.exception.ValidationException;
import com.armorauth.data.entity.OAuth2Scope;
import com.armorauth.data.repository.OAuth2ClientRepository;
import com.armorauth.data.repository.OAuth2ScopeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScopeService {

    private final OAuth2ScopeRepository scopeRepository;
    private final OAuth2ClientRepository clientRepository;
    private final AuditEventService auditEventService;

    public ScopeService(OAuth2ScopeRepository scopeRepository,
                        OAuth2ClientRepository clientRepository,
                        AuditEventService auditEventService) {
        this.scopeRepository = scopeRepository;
        this.clientRepository = clientRepository;
        this.auditEventService = auditEventService;
    }

    @Transactional(readOnly = true)
    public Page<ScopeDTO.Response> listScopes(String clientId, Pageable pageable) {
        Page<OAuth2Scope> scopes = clientId == null || clientId.isBlank()
                ? scopeRepository.findAll(pageable)
                : scopeRepository.findByClientId(clientId, pageable);
        return scopes.map(this::toResponse);
    }

    @Transactional
    public ScopeDTO.Response createScope(ScopeDTO.CreateRequest request) {
        String clientId = request.clientId();
        String scope = request.scope();
        clientRepository.findOAuth2ClientByClientId(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("应用", clientId));
        if (scopeRepository.existsByClientIdAndScope(clientId, scope)) {
            throw new ValidationException("Scope 已存在: " + scope);
        }

        OAuth2Scope entity = new OAuth2Scope();
        entity.setClientId(clientId);
        entity.setScope(scope);
        entity.setDescription(request.description());
        entity = scopeRepository.save(entity);

        auditEventService.record("SCOPE_CREATED",
                AuditContext.getCurrentPrincipal(), "scope", clientId + "/" + scope,
                "创建应用 scope: " + scope, AuditContext.getClientIp());

        return toResponse(entity);
    }

    @Transactional
    public ScopeDTO.Response updateScope(String clientId, String scope, ScopeDTO.UpdateRequest request) {
        OAuth2Scope entity = scopeRepository.findByClientIdAndScope(clientId, scope)
                .orElseThrow(() -> new ResourceNotFoundException("Scope", clientId + "/" + scope));
        entity.setDescription(request.description());
        entity = scopeRepository.save(entity);

        auditEventService.record("SCOPE_UPDATED",
                AuditContext.getCurrentPrincipal(), "scope", clientId + "/" + scope,
                "更新应用 scope: " + scope, AuditContext.getClientIp());

        return toResponse(entity);
    }

    @Transactional
    public void deleteScope(String clientId, String scope) {
        if (!scopeRepository.existsByClientIdAndScope(clientId, scope)) {
            throw new ResourceNotFoundException("Scope", clientId + "/" + scope);
        }
        scopeRepository.deleteByClientIdAndScope(clientId, scope);

        auditEventService.record("SCOPE_DELETED",
                AuditContext.getCurrentPrincipal(), "scope", clientId + "/" + scope,
                "删除应用 scope: " + scope, AuditContext.getClientIp());
    }

    private ScopeDTO.Response toResponse(OAuth2Scope scope) {
        return new ScopeDTO.Response(scope.getClientId(), scope.getScope(), scope.getDescription());
    }
}
