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
package com.armorauth.federation;

import com.armorauth.data.entity.IdentityProvider;
import com.armorauth.data.entity.Organization;
import com.armorauth.data.entity.OrganizationMember;
import com.armorauth.data.entity.Role;
import com.armorauth.data.entity.UserRole;
import com.armorauth.data.repository.IdentityProviderRepository;
import com.armorauth.data.repository.OrganizationMemberRepository;
import com.armorauth.data.repository.OrganizationRepository;
import com.armorauth.data.repository.RoleRepository;
import com.armorauth.data.repository.UserRoleRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 身份源属性映射服务
 * <p>
 * 根据 IdentityProvider.attributeMapping 配置，
 * 将外部身份源的属性映射到本地用户的角色和组织。
 *
 * @author fulin
 * @since 2026-05-23
 */
@Service
public class IdpAttributeMappingService {

    private static final Logger log = LoggerFactory.getLogger(IdpAttributeMappingService.class);

    private final IdentityProviderRepository idpRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final ObjectMapper objectMapper;

    public IdpAttributeMappingService(IdentityProviderRepository idpRepository,
                                      OrganizationRepository organizationRepository,
                                      OrganizationMemberRepository memberRepository,
                                      RoleRepository roleRepository,
                                      UserRoleRepository userRoleRepository,
                                      ObjectMapper objectMapper) {
        this.idpRepository = idpRepository;
        this.organizationRepository = organizationRepository;
        this.memberRepository = memberRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 根据身份源配置映射用户属性（组织和角色）
     *
     * @param userId          本地用户ID
     * @param registrationId  身份源 registrationId
     * @param providerAttributes 外部身份源返回的用户属性 JSON
     */
    public void applyAttributeMapping(String userId, String registrationId, String providerAttributes) {
        Optional<IdentityProvider> idpOpt = idpRepository.findByRegistrationId(registrationId);
        if (idpOpt.isEmpty()) {
            return;
        }

        IdentityProvider idp = idpOpt.get();
        String mappingJson = idp.getAttributeMapping();
        if (mappingJson == null || mappingJson.isBlank()) {
            return;
        }

        try {
            Map<String, Object> mapping = objectMapper.readValue(mappingJson, new TypeReference<>() {});
            Map<String, Object> attributes = providerAttributes != null && !providerAttributes.isBlank()
                    ? objectMapper.readValue(providerAttributes, new TypeReference<>() {})
                    : Map.of();

            // 映射组织
            mapOrganization(userId, mapping, attributes);

            // 映射角色
            mapRoles(userId, mapping, attributes);

        } catch (JsonProcessingException e) {
            log.warn("Failed to parse attribute mapping for IdP {}: {}", registrationId, e.getMessage());
        }
    }

    private void mapOrganization(String userId, Map<String, Object> mapping, Map<String, Object> attributes) {
        Object orgMappingObj = mapping.get("organization");
        if (!(orgMappingObj instanceof Map<?, ?> orgMapping)) {
            return;
        }

        String orgIdValue = resolveValue(orgMapping, attributes);
        if (orgIdValue == null) {
            return;
        }

        Optional<Organization> orgOpt;
        // 尝试通过 orgCode 查找
        String matchBy = orgMapping.containsKey("matchBy") ? String.valueOf(orgMapping.get("matchBy")) : "orgCode";
        if ("orgCode".equals(matchBy)) {
            orgOpt = organizationRepository.findByOrgCode(orgIdValue);
        } else {
            orgOpt = organizationRepository.findById(orgIdValue);
        }

        if (orgOpt.isPresent()) {
            Organization org = orgOpt.get();
            // 检查是否已经是成员
            Optional<OrganizationMember> existing = memberRepository.findByOrgIdAndUserId(org.getId(), userId);
            if (existing.isEmpty()) {
                OrganizationMember member = new OrganizationMember();
                member.setOrgId(org.getId());
                member.setUserId(userId);
                member.setOrgRole("MEMBER");
                member.setCreatedAt(Instant.now());
                memberRepository.save(member);
                log.info("Mapped user {} to organization {} via IdP", userId, org.getOrgName());
            }
        }
    }

    private void mapRoles(String userId, Map<String, Object> mapping, Map<String, Object> attributes) {
        Object roleMappingObj = mapping.get("roles");
        if (!(roleMappingObj instanceof Map<?, ?> roleMapping)) {
            return;
        }

        String roleValue = resolveValue(roleMapping, attributes);
        if (roleValue == null) {
            return;
        }

        // 支持逗号分隔的多角色
        String[] roles = roleValue.split(",");
        for (String roleCode : roles) {
            String trimmed = roleCode.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            Optional<Role> role = roleRepository.findByRoleCodeIgnoreCase(trimmed);
            if (role.isEmpty()) {
                log.warn("Configured IdP role mapping references missing role {}", trimmed);
                continue;
            }

            String roleId = role.get().getId();
            if (!userRoleRepository.existsByUserIdAndRoleId(userId, roleId)) {
                UserRole userRole = new UserRole();
                userRole.setUserId(userId);
                userRole.setRoleId(roleId);
                userRoleRepository.save(userRole);
                log.info("Mapped user {} to role {} via IdP", userId, trimmed);
            }
        }
    }

    private String resolveValue(Map<?, ?> mappingConfig, Map<String, Object> attributes) {
        // 支持固定值
        Object fixedValue = mappingConfig.get("value");
        if (fixedValue instanceof String fv && !fv.isBlank()) {
            return fv;
        }

        // 支持从外部属性中提取
        Object fromAttr = mappingConfig.get("fromAttribute");
        if (fromAttr instanceof String attrName && !attrName.isBlank()) {
            Object value = attributes.get(attrName);
            return value != null ? value.toString() : null;
        }

        return null;
    }
}
