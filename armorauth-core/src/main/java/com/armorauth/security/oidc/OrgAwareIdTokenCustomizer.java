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
package com.armorauth.security.oidc;

import com.armorauth.data.entity.Organization;
import com.armorauth.data.entity.OrganizationMember;
import com.armorauth.data.entity.UserRole;
import com.armorauth.data.repository.OrganizationMemberRepository;
import com.armorauth.data.repository.OrganizationRepository;
import com.armorauth.data.repository.UserRoleRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 组织/租户感知的 ID Token 定制器
 * <p>
 * 将用户的组织信息、组织角色写入 ID Token 和 Access Token
 *
 * @author fulin
 * @since 2026-05-23
 */
public class OrgAwareIdTokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    private static final Set<String> ID_TOKEN_CLAIMS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            IdTokenClaimNames.ISS, IdTokenClaimNames.SUB, IdTokenClaimNames.AUD,
            IdTokenClaimNames.EXP, IdTokenClaimNames.IAT, IdTokenClaimNames.AUTH_TIME,
            IdTokenClaimNames.NONCE, IdTokenClaimNames.ACR, IdTokenClaimNames.AMR,
            IdTokenClaimNames.AZP, IdTokenClaimNames.AT_HASH, IdTokenClaimNames.C_HASH
    )));

    private final OrganizationMemberRepository memberRepository;
    private final OrganizationRepository organizationRepository;

    public OrgAwareIdTokenCustomizer(OrganizationMemberRepository memberRepository,
                                      OrganizationRepository organizationRepository) {
        this.memberRepository = memberRepository;
        this.organizationRepository = organizationRepository;
    }

    @Override
    public void customize(JwtEncodingContext context) {
        if (OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue())) {
            addRolesClaim(context);
            addOrganizationClaims(context);
        }
    }

    private void addRolesClaim(JwtEncodingContext context) {
        Collection<? extends GrantedAuthority> authorities = context.getPrincipal().getAuthorities();
        if (authorities != null && !authorities.isEmpty()) {
            List<String> roles = authorities.stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(auth -> auth.startsWith("ROLE_"))
                    .map(auth -> auth.substring(5))
                    .collect(Collectors.toList());
            if (!roles.isEmpty()) {
                context.getClaims().claim("roles", roles);
            }
        }
    }

    private void addOrganizationClaims(JwtEncodingContext context) {
        String username = context.getPrincipal().getName();
        if (username == null) {
            return;
        }

        // 从用户的 authorities 中提取 userId (格式: user:{userId})
        String userId = null;
        for (GrantedAuthority authority : context.getPrincipal().getAuthorities()) {
            String auth = authority.getAuthority();
            if (auth.startsWith("SCOPE_user:")) {
                userId = auth.substring("SCOPE_user:".length());
                break;
            }
        }

        // 尝试通过 username 查找用户组织
        try {
            List<OrganizationMember> memberships = memberRepository.findByUserId(username);
            if (!memberships.isEmpty()) {
                List<String> orgIds = memberships.stream()
                        .map(OrganizationMember::getOrgId)
                        .distinct()
                        .toList();
                List<String> orgRoles = memberships.stream()
                        .filter(m -> m.getOrgRole() != null)
                        .map(OrganizationMember::getOrgRole)
                        .distinct()
                        .toList();

                if (!orgIds.isEmpty()) {
                    context.getClaims().claim("org_ids", orgIds);
                }
                if (!orgRoles.isEmpty()) {
                    context.getClaims().claim("org_roles", orgRoles);
                }

                // 获取第一个组织的 tenantId
                Optional<Organization> firstOrg = organizationRepository.findById(orgIds.get(0));
                firstOrg.ifPresent(org -> context.getClaims().claim("tenant_id", org.getTenantId()));
            }
        } catch (Exception e) {
            // 静默处理，不影响 token 生成
        }
    }
}
