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

import com.armorauth.admin.dto.TenantDTO;
import com.armorauth.common.audit.AuditContext;
import com.armorauth.common.exception.ResourceNotFoundException;
import com.armorauth.common.exception.ValidationException;
import com.armorauth.data.entity.Tenant;
import com.armorauth.data.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;
    private final AuditEventService auditEventService;
    private final boolean multipleIssuersEnabled;

    public TenantService(TenantRepository tenantRepository, AuditEventService auditEventService,
                         @Value("${armorauth.authorization-server.multiple-issuers.enabled:false}")
                         boolean multipleIssuersEnabled) {
        this.tenantRepository = tenantRepository;
        this.auditEventService = auditEventService;
        this.multipleIssuersEnabled = multipleIssuersEnabled;
    }

    @Transactional(readOnly = true)
    public Page<TenantDTO.Response> listTenants(Pageable pageable) {
        return tenantRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TenantDTO.Response getTenant(String id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("租户", id));
        return toResponse(tenant);
    }

    @Transactional
    public TenantDTO.Response createTenant(TenantDTO.CreateRequest request) {
        if (tenantRepository.existsByTenantCode(request.tenantCode())) {
            throw new ValidationException("租户编码已存在: " + request.tenantCode());
        }

        Tenant tenant = new Tenant();
        tenant.setTenantCode(request.tenantCode());
        tenant.setTenantName(request.tenantName());
        tenant.setDescription(request.description());
        tenant.setLogo(request.logo());
        tenant.setPrimaryColor(request.primaryColor());
        tenant.setLoginPageTitle(request.loginPageTitle());
        tenant.setPrivacyPolicyUrl(request.privacyPolicyUrl());
        tenant.setTermsOfServiceUrl(request.termsOfServiceUrl());
        tenant.setEnabled(true);
        tenant.setCreatedAt(Instant.now());
        tenant = tenantRepository.save(tenant);

        auditEventService.record("TENANT_CREATED",
                AuditContext.getCurrentPrincipal(), "tenant", tenant.getId(),
                "创建租户: " + tenant.getTenantName(), AuditContext.getClientIp());

        return toResponse(tenant);
    }

    @Transactional
    public TenantDTO.Response updateTenant(String id, TenantDTO.UpdateRequest request) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("租户", id));

        if (request.tenantName() != null) tenant.setTenantName(request.tenantName());
        tenant.setDescription(request.description());
        tenant.setLogo(request.logo());
        tenant.setPrimaryColor(request.primaryColor());
        tenant.setCustomDomain(request.customDomain());
        tenant.setLoginPageTitle(request.loginPageTitle());
        tenant.setPrivacyPolicyUrl(request.privacyPolicyUrl());
        tenant.setTermsOfServiceUrl(request.termsOfServiceUrl());
        tenant.setUpdatedAt(Instant.now());
        tenant = tenantRepository.save(tenant);

        auditEventService.record("TENANT_UPDATED",
                AuditContext.getCurrentPrincipal(), "tenant", id,
                "更新租户: " + tenant.getTenantName(), AuditContext.getClientIp());

        return toResponse(tenant);
    }

    @Transactional
    public void updateTenantStatus(String id, Boolean enabled) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("租户", id));
        tenant.setEnabled(enabled);
        tenant.setUpdatedAt(Instant.now());
        tenantRepository.save(tenant);

        String action = Boolean.TRUE.equals(enabled) ? "启用" : "禁用";
        auditEventService.record("TENANT_STATUS_CHANGED",
                AuditContext.getCurrentPrincipal(), "tenant", id,
                action + "租户: " + tenant.getTenantName(), AuditContext.getClientIp());
    }

    @Transactional
    public void deleteTenant(String id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("租户", id));
        String tenantName = tenant.getTenantName();
        tenantRepository.delete(tenant);

        auditEventService.record("TENANT_DELETED",
                AuditContext.getCurrentPrincipal(), "tenant", id,
                "删除租户: " + tenantName, AuditContext.getClientIp());
    }

    private TenantDTO.Response toResponse(Tenant tenant) {
        return new TenantDTO.Response(
                tenant.getId(), tenant.getTenantCode(), tenant.getTenantName(),
                tenant.getDescription(), tenant.getLogo(), tenant.getPrimaryColor(),
                tenant.getCustomDomain(), tenant.getLoginPageTitle(),
                tenant.getPrivacyPolicyUrl(), tenant.getTermsOfServiceUrl(),
                tenant.getEnabled(), "/t/" + tenant.getTenantCode(),
                multipleIssuersEnabled && Boolean.TRUE.equals(tenant.getEnabled()),
                tenant.getCustomDomain() == null || tenant.getCustomDomain().isBlank()
                        ? null : "https://" + tenant.getCustomDomain(),
                tenant.getCreatedAt(), tenant.getUpdatedAt()
        );
    }
}
