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

import com.armorauth.common.audit.AuditContext;
import com.armorauth.common.exception.ResourceNotFoundException;
import com.armorauth.common.exception.ValidationException;
import com.armorauth.data.entity.Permission;
import com.armorauth.data.entity.RolePermission;
import com.armorauth.data.repository.PermissionRepository;
import com.armorauth.data.repository.RolePermissionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 权限管理服务
 *
 * @author fulin
 * @since 2026-05-23
 */
@Service
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final AuditEventService auditEventService;

    public PermissionService(PermissionRepository permissionRepository,
                             RolePermissionRepository rolePermissionRepository,
                             AuditEventService auditEventService) {
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.auditEventService = auditEventService;
    }

    @Transactional(readOnly = true)
    public Page<Permission> listPermissions(Pageable pageable) {
        return permissionRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Permission getPermission(String id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("权限", id));
    }

    @Transactional
    public Permission createPermission(String permissionCode, String permissionName,
                                       String resourceType, String action, String description) {
        if (permissionRepository.existsByPermissionCode(permissionCode)) {
            throw new ValidationException("权限编码已存在: " + permissionCode);
        }

        Permission permission = new Permission();
        permission.setPermissionCode(permissionCode);
        permission.setPermissionName(permissionName);
        permission.setResourceType(resourceType);
        permission.setAction(action);
        permission.setDescription(description);
        permission.setBuiltin(false);
        permission = permissionRepository.save(permission);

        auditEventService.record("PERMISSION_CREATED",
                AuditContext.getCurrentPrincipal(), "permission", permission.getId(),
                "创建权限: " + permissionName, AuditContext.getClientIp());

        return permission;
    }

    @Transactional
    public void deletePermission(String id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("权限", id));
        if (Boolean.TRUE.equals(permission.getBuiltin())) {
            throw new ValidationException("内置权限不允许删除");
        }
        permissionRepository.delete(permission);

        auditEventService.record("PERMISSION_DELETED",
                AuditContext.getCurrentPrincipal(), "permission", id,
                "删除权限: " + permission.getPermissionName(), AuditContext.getClientIp());
    }

    @Transactional
    public void assignPermissionToRole(String roleId, String permissionId) {
        List<RolePermission> existing = rolePermissionRepository.findByRoleId(roleId);
        boolean alreadyAssigned = existing.stream()
                .anyMatch(rp -> rp.getPermissionId().equals(permissionId));
        if (alreadyAssigned) {
            return;
        }

        RolePermission rp = new RolePermission();
        rp.setRoleId(roleId);
        rp.setPermissionId(permissionId);
        rolePermissionRepository.save(rp);

        auditEventService.record("ROLE_PERMISSION_ASSIGNED",
                AuditContext.getCurrentPrincipal(), "role", roleId,
                "分配权限 " + permissionId + " 到角色 " + roleId, AuditContext.getClientIp());
    }

    @Transactional
    public void removePermissionFromRole(String roleId, String permissionId) {
        List<RolePermission> existing = rolePermissionRepository.findByRoleId(roleId);
        existing.stream()
                .filter(rp -> rp.getPermissionId().equals(permissionId))
                .findFirst()
                .ifPresent(rp -> {
                    rolePermissionRepository.delete(rp);
                    auditEventService.record("ROLE_PERMISSION_REMOVED",
                            AuditContext.getCurrentPrincipal(), "role", roleId,
                            "移除权限 " + permissionId + " 从角色 " + roleId, AuditContext.getClientIp());
                });
    }

    @Transactional(readOnly = true)
    public List<RolePermission> getRolePermissions(String roleId) {
        return rolePermissionRepository.findByRoleId(roleId);
    }
}
