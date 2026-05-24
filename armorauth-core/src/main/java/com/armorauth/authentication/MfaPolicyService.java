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
package com.armorauth.authentication;

import com.armorauth.data.entity.OAuth2Client;
import com.armorauth.data.entity.UserRole;
import com.armorauth.data.repository.OAuth2ClientRepository;
import com.armorauth.data.repository.UserRoleRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MFA 策略服务
 * <p>
 * 判断用户是否需要完成 MFA 验证：
 * - 应用级别：OAuth2Client.mfaRequired
 * - 角色级别：SUPER_ADMIN / TENANT_ADMIN 强制 MFA
 *
 * @author fulin
 * @since 2026-05-23
 */
@Service
public class MfaPolicyService {

    private static final Set<String> MFA_REQUIRED_ROLES = Set.of(
            "SUPER_ADMIN", "TENANT_ADMIN"
    );

    private final OAuth2ClientRepository clientRepository;
    private final UserRoleRepository userRoleRepository;

    public MfaPolicyService(OAuth2ClientRepository clientRepository,
                            UserRoleRepository userRoleRepository) {
        this.clientRepository = clientRepository;
        this.userRoleRepository = userRoleRepository;
    }

    /**
     * 判断指定应用是否要求 MFA
     */
    public boolean isAppMfaRequired(String clientId) {
        return clientRepository.findOAuth2ClientByClientId(clientId)
                .map(c -> Boolean.TRUE.equals(c.getMfaRequired()))
                .orElse(false);
    }

    /**
     * 判断用户是否有要求 MFA 的角色
     */
    public boolean isUserMfaRequired(String userId) {
        List<UserRole> roles = userRoleRepository.findByUserId(userId);
        return roles.stream()
                .map(UserRole::getRole)
                .filter(r -> r != null)
                .anyMatch(r -> MFA_REQUIRED_ROLES.contains(r.getRoleCode()));
    }

    /**
     * 综合判断：应用要求 MFA 或用户角色要求 MFA
     */
    public boolean requiresMfa(String userId, String clientId) {
        if (clientId != null && isAppMfaRequired(clientId)) {
            return true;
        }
        return isUserMfaRequired(userId);
    }
}
