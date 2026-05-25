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

import com.armorauth.admin.dto.UserDTO;
import com.armorauth.common.audit.AuditContext;
import com.armorauth.common.exception.ResourceNotFoundException;
import com.armorauth.data.entity.UserInfo;
import com.armorauth.data.entity.UserRole;
import com.armorauth.data.repository.UserInfoRepository;
import com.armorauth.data.repository.UserRoleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

/**
 * 用户管理服务
 *
 * @author fulin
 * @since 2026-05-23
 */
@Service
public class UserService {

    private final UserInfoRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicyService;
    private final AuditEventService auditEventService;

    public UserService(UserInfoRepository userRepository,
                       UserRoleRepository userRoleRepository,
                       PasswordEncoder passwordEncoder,
                       PasswordPolicyService passwordPolicyService,
                       AuditEventService auditEventService) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyService = passwordPolicyService;
        this.auditEventService = auditEventService;
    }

    @Transactional(readOnly = true)
    public Page<UserDTO.Response> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<UserDTO.Response> listUsers(Pageable pageable, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return listUsers(pageable);
        }
        return userRepository.search(keyword.trim(), pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public UserDTO.Response getUser(String id) {
        UserInfo user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户", id));
        return toResponse(user);
    }

    @Transactional
    public UserDTO.Response createUser(UserDTO.CreateRequest request) {
        validatePassword(request.password());

        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("用户名已存在: " + request.username());
        }
        if (request.email() != null && userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("邮箱已被使用: " + request.email());
        }

        UserInfo user = new UserInfo();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setAvatar(request.avatar());
        user.setProfile(request.profile());
        user.setStatus(0);
        user.setEmailVerified(Boolean.TRUE.equals(request.emailVerified()));
        user.setPhoneVerified(Boolean.TRUE.equals(request.phoneVerified()));
        user.setCreateTime(Instant.now());
        user = userRepository.save(user);

        auditEventService.record("USER_CREATED",
                AuditContext.getCurrentPrincipal(), "user", user.getId(),
                "创建用户: " + request.username(), AuditContext.getClientIp());

        return toResponse(user);
    }

    @Transactional
    public UserDTO.Response updateUser(String id, UserDTO.UpdateRequest request) {
        UserInfo user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户", id));

        if (request.displayName() != null) {
            user.setDisplayName(request.displayName());
        }
        if (request.email() != null) {
            user.setEmail(request.email());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }
        if (request.avatar() != null) {
            user.setAvatar(request.avatar());
        }
        if (request.emailVerified() != null) {
            user.setEmailVerified(request.emailVerified());
        }
        if (request.phoneVerified() != null) {
            user.setPhoneVerified(request.phoneVerified());
        }
        if (request.profile() != null) {
            user.setProfile(request.profile());
        }
        user.setUpdateTime(Instant.now());
        user = userRepository.save(user);

        auditEventService.record("USER_UPDATED",
                AuditContext.getCurrentPrincipal(), "user", id,
                "更新用户: " + user.getUsername(), AuditContext.getClientIp());

        return toResponse(user);
    }

    @Transactional
    public void updateUserStatus(String id, Integer status) {
        UserInfo user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户", id));
        user.setStatus(status);
        user.setUpdateTime(Instant.now());
        userRepository.save(user);

        auditEventService.record("USER_STATUS_CHANGED",
                AuditContext.getCurrentPrincipal(), "user", id,
                "变更用户状态: " + user.getUsername() + " -> " + status, AuditContext.getClientIp());
    }

    @Transactional
    public void lockUser(String id, int durationMinutes) {
        UserInfo user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户", id));
        user.setLockedUntil(Instant.now().plusSeconds(durationMinutes * 60L));
        user.setStatus(1);
        user.setUpdateTime(Instant.now());
        userRepository.save(user);

        auditEventService.record("USER_LOCKED",
                AuditContext.getCurrentPrincipal(), "user", id,
                "锁定用户: " + user.getUsername() + " (" + durationMinutes + "分钟)", AuditContext.getClientIp());
    }

    @Transactional
    public void unlockUser(String id) {
        UserInfo user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户", id));
        user.setLockedUntil(null);
        user.setStatus(0);
        user.setUpdateTime(Instant.now());
        userRepository.save(user);

        auditEventService.record("USER_UNLOCKED",
                AuditContext.getCurrentPrincipal(), "user", id,
                "解锁用户: " + user.getUsername(), AuditContext.getClientIp());
    }

    @Transactional
    public void resetPassword(String id, String newPassword) {
        validatePassword(newPassword);

        UserInfo user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户", id));
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(Instant.now());
        userRepository.save(user);

        auditEventService.record("USER_PASSWORD_RESET",
                AuditContext.getCurrentPrincipal(), "user", id,
                "重置密码: " + user.getUsername(), AuditContext.getClientIp());
    }

    private void validatePassword(String password) {
        List<String> errors = passwordPolicyService.validate(password);
        if (!errors.isEmpty()) {
            StringJoiner joiner = new StringJoiner("; ");
            errors.forEach(joiner::add);
            throw new IllegalArgumentException("密码不符合策略: " + joiner);
        }
    }

    @Transactional
    public void deleteUser(String id) {
        UserInfo user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户", id));
        String username = user.getUsername();
        userRoleRepository.deleteByUserId(id);
        userRepository.delete(user);

        auditEventService.record("USER_DELETED",
                AuditContext.getCurrentPrincipal(), "user", id,
                "删除用户: " + username, AuditContext.getClientIp());
    }

    private UserDTO.Response toResponse(UserInfo user) {
        List<String> roles = userRoleRepository.findByUserId(user.getId()).stream()
                .map(UserRole::getRole)
                .filter(role -> role != null)
                .map(role -> role.getRoleCode())
                .toList();

        return new UserDTO.Response(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                user.getPhone(),
                user.getAvatar(),
                user.getStatus(),
                user.getEmailVerified(),
                user.getPhoneVerified(),
                user.getLockedUntil(),
                user.getCreateTime(),
                user.getLastLoginTime(),
                roles.isEmpty() ? Collections.emptyList() : roles,
                user.getProfile()
        );
    }
}
