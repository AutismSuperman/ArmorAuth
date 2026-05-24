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

import com.armorauth.admin.dto.RoleDTO;
import com.armorauth.common.audit.AuditContext;
import com.armorauth.common.exception.ResourceNotFoundException;
import com.armorauth.data.entity.Role;
import com.armorauth.data.entity.UserRole;
import com.armorauth.data.repository.RoleRepository;
import com.armorauth.data.repository.UserRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 角色管理服务
 *
 * @author fulin
 * @since 2026-05-23
 */
@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final AuditEventService auditEventService;

    public RoleService(RoleRepository roleRepository,
                       UserRoleRepository userRoleRepository,
                       AuditEventService auditEventService) {
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.auditEventService = auditEventService;
    }

    @Transactional(readOnly = true)
    public List<RoleDTO.Response> listRoles() {
        return roleRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RoleDTO.Response createRole(RoleDTO.CreateRequest request) {
        if (roleRepository.existsByRoleCode(request.roleCode())) {
            throw new IllegalArgumentException("角色编码已存在: " + request.roleCode());
        }
        Role role = new Role();
        role.setRoleCode(request.roleCode());
        role.setRoleName(request.roleName());
        role.setDescription(request.description());
        role.setBuiltin(false);
        role = roleRepository.save(role);

        auditEventService.record("ROLE_CREATED",
                AuditContext.getCurrentPrincipal(), "role", role.getId(),
                "创建角色: " + request.roleCode(), AuditContext.getClientIp());

        return toResponse(role);
    }

    @Transactional
    public void deleteRole(String id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("角色", id));
        if (Boolean.TRUE.equals(role.getBuiltin())) {
            throw new IllegalArgumentException("内置角色不能删除");
        }
        roleRepository.delete(role);

        auditEventService.record("ROLE_DELETED",
                AuditContext.getCurrentPrincipal(), "role", id,
                "删除角色: " + role.getRoleCode(), AuditContext.getClientIp());
    }

    @Transactional
    public void bindUserRole(String userId, String roleId) {
        List<UserRole> existing = userRoleRepository.findByUserId(userId);
        boolean alreadyBound = existing.stream()
                .anyMatch(ur -> ur.getRoleId().equals(roleId));
        if (alreadyBound) {
            return;
        }
        UserRole userRole = new UserRole();
        userRole.setId(UUID.randomUUID().toString());
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        userRoleRepository.save(userRole);

        auditEventService.record("ROLE_BINDING_CREATED",
                AuditContext.getCurrentPrincipal(), "role_binding", userId,
                "绑定角色: userId=" + userId + ", roleId=" + roleId, AuditContext.getClientIp());
    }

    @Transactional(readOnly = true)
    public List<RoleDTO.BindingResponse> listUserRoleBindings(String userId) {
        return userRoleRepository.findByUserId(userId).stream()
                .map(this::toBindingResponse)
                .toList();
    }

    @Transactional
    public void unbindUserRole(String bindingId) {
        UserRole userRole = userRoleRepository.findById(bindingId)
                .orElseThrow(() -> new ResourceNotFoundException("角色绑定", bindingId));
        String userId = userRole.getUserId();
        String roleId = userRole.getRoleId();
        userRoleRepository.delete(userRole);

        auditEventService.record("ROLE_BINDING_REMOVED",
                AuditContext.getCurrentPrincipal(), "role_binding", userId,
                "解绑角色: userId=" + userId + ", roleId=" + roleId, AuditContext.getClientIp());
    }

    @Transactional
    public void unbindUserRole(String userId, String roleId) {
        userRoleRepository.deleteByUserIdAndRoleId(userId, roleId);

        auditEventService.record("ROLE_BINDING_REMOVED",
                AuditContext.getCurrentPrincipal(), "role_binding", userId,
                "解绑角色: userId=" + userId + ", roleId=" + roleId, AuditContext.getClientIp());
    }

    private RoleDTO.Response toResponse(Role role) {
        return new RoleDTO.Response(
                role.getId(),
                role.getRoleCode(),
                role.getRoleName(),
                role.getDescription(),
                role.getBuiltin()
        );
    }

    private RoleDTO.BindingResponse toBindingResponse(UserRole userRole) {
        Role role = userRole.getRole();
        return new RoleDTO.BindingResponse(
                userRole.getId(),
                userRole.getUserId(),
                userRole.getRoleId(),
                role != null ? role.getRoleCode() : null,
                role != null ? role.getRoleName() : null
        );
    }
}
