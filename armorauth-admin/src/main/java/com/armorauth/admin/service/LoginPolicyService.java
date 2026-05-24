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

import com.armorauth.admin.dto.LoginPolicyDTO;
import com.armorauth.common.audit.AuditContext;
import com.armorauth.common.exception.ResourceNotFoundException;
import com.armorauth.data.entity.OAuth2Client;
import com.armorauth.data.repository.OAuth2ClientRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LoginPolicyService {

    private static final List<String> ROLE_MFA_REQUIRED = List.of("SUPER_ADMIN", "TENANT_ADMIN");

    private final OAuth2ClientRepository clientRepository;
    private final AuditEventService auditEventService;

    public LoginPolicyService(OAuth2ClientRepository clientRepository,
                              AuditEventService auditEventService) {
        this.clientRepository = clientRepository;
        this.auditEventService = auditEventService;
    }

    @Transactional(readOnly = true)
    public Page<LoginPolicyDTO.Response> listPolicies(Pageable pageable) {
        return clientRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public LoginPolicyDTO.Response getPolicy(String id) {
        OAuth2Client client = findClient(id);
        return toResponse(client);
    }

    @Transactional
    public LoginPolicyDTO.Response updatePolicy(String id, LoginPolicyDTO.UpdateRequest request) {
        OAuth2Client client = findClient(id);
        if (request.mfaRequired() != null) {
            client.setMfaRequired(request.mfaRequired());
        }
        client = clientRepository.save(client);

        auditEventService.record("LOGIN_POLICY_UPDATED",
                AuditContext.getCurrentPrincipal(), "application", client.getId(),
                "更新登录策略: " + client.getClientName(), AuditContext.getClientIp());

        return toResponse(client);
    }

    private OAuth2Client findClient(String id) {
        return clientRepository.findOAuth2ClientById(id)
                .or(() -> clientRepository.findOAuth2ClientByClientId(id))
                .orElseThrow(() -> new ResourceNotFoundException("登录策略", id));
    }

    private LoginPolicyDTO.Response toResponse(OAuth2Client client) {
        return new LoginPolicyDTO.Response(
                client.getId(),
                client.getClientId(),
                client.getClientName(),
                Boolean.TRUE.equals(client.getMfaRequired()),
                ROLE_MFA_REQUIRED,
                client.getClientIdIssuedAt()
        );
    }
}
