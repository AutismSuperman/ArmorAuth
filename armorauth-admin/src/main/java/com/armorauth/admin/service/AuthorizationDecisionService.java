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

import com.armorauth.actions.ActionContext;
import com.armorauth.actions.ActionExecutionService;
import com.armorauth.actions.ActionPhase;
import com.armorauth.actions.ActionResult;
import com.armorauth.admin.dto.AuthorizationDecisionDTO;
import com.armorauth.common.exception.ResourceNotFoundException;
import com.armorauth.common.exception.ValidationException;
import com.armorauth.data.entity.Permission;
import com.armorauth.data.entity.RolePermission;
import com.armorauth.data.entity.UserInfo;
import com.armorauth.data.entity.UserRole;
import com.armorauth.data.repository.PermissionRepository;
import com.armorauth.data.repository.RolePermissionRepository;
import com.armorauth.data.repository.UserInfoRepository;
import com.armorauth.data.repository.UserRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AuthorizationDecisionService {

    private final UserInfoRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final ActionExecutionService actionExecutionService;

    public AuthorizationDecisionService(UserInfoRepository userRepository,
                                        UserRoleRepository userRoleRepository,
                                        RolePermissionRepository rolePermissionRepository,
                                        PermissionRepository permissionRepository,
                                        ActionExecutionService actionExecutionService) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.permissionRepository = permissionRepository;
        this.actionExecutionService = actionExecutionService;
    }

    @Transactional(readOnly = true)
    public AuthorizationDecisionDTO.CheckResponse check(AuthorizationDecisionDTO.CheckRequest request) {
        if (request == null) {
            throw new ValidationException("授权检查请求不能为空");
        }
        UserInfo user = resolveUser(request);
        List<UserRole> userRoles = userRoleRepository.findByUserId(user.getId());
        Set<String> roleCodes = new LinkedHashSet<>();
        Set<String> permissionCodes = new LinkedHashSet<>();
        List<Permission> permissions = new ArrayList<>();

        for (UserRole userRole : userRoles) {
            if (userRole.getRole() != null) {
                roleCodes.add(userRole.getRole().getRoleCode());
            }
            for (RolePermission rolePermission : rolePermissionRepository.findByRoleId(userRole.getRoleId())) {
                Permission permission = rolePermission.getPermission();
                if (permission == null && rolePermission.getPermissionId() != null) {
                    permission = permissionRepository.findById(rolePermission.getPermissionId()).orElse(null);
                }
                if (permission != null) {
                    permissions.add(permission);
                    permissionCodes.add(permission.getPermissionCode());
                }
            }
        }

        boolean superAdmin = roleCodes.stream().anyMatch("SUPER_ADMIN"::equalsIgnoreCase);
        boolean matched = superAdmin || permissions.stream().anyMatch(permission -> matches(permission, request));
        String reason = matched ? "matched_permission" : "no_matching_permission";
        if (superAdmin) {
            reason = "super_admin";
        }

        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("userId", user.getId());
        attributes.put("username", user.getUsername());
        attributes.put("roles", List.copyOf(roleCodes));
        attributes.put("permissions", List.copyOf(permissionCodes));
        attributes.put("request", request);
        attributes.put("context", request.context() == null ? Map.of() : request.context());
        ActionResult actionResult = actionExecutionService.execute(
                new ActionContext(ActionPhase.AUTHORIZATION_CHECK, user.getUsername(), attributes));
        if (actionResult.terminal()) {
            matched = actionResult.allowed();
            reason = actionResult.reason() != null ? actionResult.reason() : "action_decision";
        } else if (!actionResult.allowed()) {
            matched = false;
            reason = actionResult.reason() != null ? actionResult.reason() : "action_denied";
        }

        return new AuthorizationDecisionDTO.CheckResponse(
                matched,
                reason,
                user.getId(),
                user.getUsername(),
                List.copyOf(roleCodes),
                List.copyOf(permissionCodes),
                actionResult.attributes());
    }

    private UserInfo resolveUser(AuthorizationDecisionDTO.CheckRequest request) {
        if (hasText(request.userId())) {
            return userRepository.findById(request.userId())
                    .orElseThrow(() -> new ResourceNotFoundException("用户", request.userId()));
        }
        if (hasText(request.username())) {
            return userRepository.findByUsernameIgnoreCase(request.username())
                    .orElseThrow(() -> new ResourceNotFoundException("用户", request.username()));
        }
        throw new ValidationException("userId 或 username 不能为空");
    }

    private boolean matches(Permission permission, AuthorizationDecisionDTO.CheckRequest request) {
        if (hasText(request.permissionCode())) {
            return request.permissionCode().equalsIgnoreCase(permission.getPermissionCode());
        }
        boolean resourceMatches = !hasText(request.resourceType())
                || request.resourceType().equalsIgnoreCase(permission.getResourceType());
        boolean actionMatches = !hasText(request.action())
                || request.action().equalsIgnoreCase(permission.getAction());
        return resourceMatches && actionMatches;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
