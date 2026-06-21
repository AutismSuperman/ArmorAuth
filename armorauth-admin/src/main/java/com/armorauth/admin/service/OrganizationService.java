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

import com.armorauth.admin.dto.OrganizationDTO;
import com.armorauth.common.audit.AuditContext;
import com.armorauth.common.exception.ResourceNotFoundException;
import com.armorauth.common.exception.ValidationException;
import com.armorauth.data.entity.Organization;
import com.armorauth.data.entity.OrganizationMember;
import com.armorauth.data.repository.OrganizationMemberRepository;
import com.armorauth.data.repository.OrganizationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class OrganizationService {

    public static final String DEFAULT_TENANT_ID = "default";

    private final OrganizationRepository orgRepository;
    private final OrganizationMemberRepository memberRepository;
    private final AuditEventService auditEventService;

    public OrganizationService(OrganizationRepository orgRepository,
                               OrganizationMemberRepository memberRepository,
                               AuditEventService auditEventService) {
        this.orgRepository = orgRepository;
        this.memberRepository = memberRepository;
        this.auditEventService = auditEventService;
    }

    @Transactional(readOnly = true)
    public List<OrganizationDTO.Response> listOrganizations(String tenantId) {
        return orgRepository.findByTenantId(tenantId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<OrganizationDTO.Response> listOrganizations(String tenantId, Pageable pageable) {
        return orgRepository.findByTenantId(resolveTenantId(tenantId), pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public OrganizationDTO.Response getOrganization(String id) {
        Organization org = orgRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("组织", id));
        return toResponse(org);
    }

    @Transactional
    public OrganizationDTO.Response createOrganization(String tenantId, OrganizationDTO.CreateRequest request) {
        tenantId = resolveTenantId(tenantId != null ? tenantId : request.tenantId());
        if (orgRepository.existsByTenantIdAndOrgCode(tenantId, request.orgCode())) {
            throw new ValidationException("组织编码已存在: " + request.orgCode());
        }

        Organization org = new Organization();
        org.setTenantId(tenantId);
        org.setOrgCode(request.orgCode());
        org.setOrgName(request.orgName());
        org.setDescription(request.description());
        org.setLogo(request.logo());
        org.setParentId(request.parentId());
        org.setEnabled(true);
        org.setCreatedAt(Instant.now());
        org = orgRepository.save(org);

        auditEventService.record("ORGANIZATION_CREATED",
                AuditContext.getCurrentPrincipal(), "organization", org.getId(),
                "创建组织: " + org.getOrgName(), AuditContext.getClientIp());

        return toResponse(org);
    }

    @Transactional
    public OrganizationDTO.Response updateOrganization(String id, OrganizationDTO.UpdateRequest request) {
        Organization org = orgRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("组织", id));

        if (request.orgName() != null) org.setOrgName(request.orgName());
        org.setDescription(request.description());
        org.setLogo(request.logo());
        org.setParentId(request.parentId());
        org.setUpdatedAt(Instant.now());
        org = orgRepository.save(org);

        auditEventService.record("ORGANIZATION_UPDATED",
                AuditContext.getCurrentPrincipal(), "organization", id,
                "更新组织: " + org.getOrgName(), AuditContext.getClientIp());

        return toResponse(org);
    }

    @Transactional
    public void deleteOrganization(String id) {
        Organization org = orgRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("组织", id));
        String orgName = org.getOrgName();
        memberRepository.deleteByOrgId(id);
        orgRepository.delete(org);

        auditEventService.record("ORGANIZATION_DELETED",
                AuditContext.getCurrentPrincipal(), "organization", id,
                "删除组织: " + orgName, AuditContext.getClientIp());
    }

    @Transactional
    public OrganizationDTO.MemberResponse addMember(String orgId, OrganizationDTO.MemberRequest request) {
        Organization org = orgRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("组织", orgId));

        memberRepository.findByOrgIdAndUserId(orgId, request.userId()).ifPresent(m -> {
            throw new ValidationException("用户已是组织成员");
        });

        OrganizationMember member = new OrganizationMember();
        member.setOrgId(orgId);
        member.setUserId(request.userId());
        member.setOrgRole(request.orgRole());
        member.setCreatedAt(Instant.now());
        member = memberRepository.save(member);

        auditEventService.record("ORG_MEMBER_ADDED",
                AuditContext.getCurrentPrincipal(), "organization_member", member.getId(),
                "添加组织成员: " + request.userId() + " 到 " + org.getOrgName(), AuditContext.getClientIp());

        return new OrganizationDTO.MemberResponse(member.getId(), orgId, request.userId(), request.orgRole(), member.getCreatedAt());
    }

    @Transactional
    public OrganizationDTO.MemberResponse updateMember(String orgId, String userId, OrganizationDTO.MemberRequest request) {
        OrganizationMember member = memberRepository.findByOrgIdAndUserId(orgId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("组织成员", orgId + "/" + userId));
        member.setOrgRole(request.orgRole());
        member = memberRepository.save(member);

        auditEventService.record("ORG_MEMBER_UPDATED",
                AuditContext.getCurrentPrincipal(), "organization_member", member.getId(),
                "更新组织成员角色: " + userId + " -> " + request.orgRole(), AuditContext.getClientIp());

        return new OrganizationDTO.MemberResponse(member.getId(), orgId, userId, member.getOrgRole(), member.getCreatedAt());
    }

    @Transactional
    public void removeMember(String orgId, String userId) {
        OrganizationMember member = memberRepository.findByOrgIdAndUserId(orgId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("组织成员", orgId + "/" + userId));
        memberRepository.delete(member);

        auditEventService.record("ORG_MEMBER_REMOVED",
                AuditContext.getCurrentPrincipal(), "organization_member", member.getId(),
                "移除组织成员: " + userId, AuditContext.getClientIp());
    }

    @Transactional(readOnly = true)
    public List<OrganizationDTO.MemberResponse> listMembers(String orgId) {
        return memberRepository.findByOrgId(orgId).stream()
                .map(m -> new OrganizationDTO.MemberResponse(m.getId(), m.getOrgId(), m.getUserId(), m.getOrgRole(), m.getCreatedAt()))
                .toList();
    }

    private OrganizationDTO.Response toResponse(Organization org) {
        return new OrganizationDTO.Response(
                org.getId(), org.getTenantId(), org.getOrgCode(), org.getOrgName(),
                org.getDescription(), org.getLogo(), org.getParentId(),
                org.getEnabled(), org.getCreatedAt(), org.getUpdatedAt()
        );
    }

    private String resolveTenantId(String tenantId) {
        return tenantId == null || tenantId.isBlank() ? DEFAULT_TENANT_ID : tenantId;
    }
}
