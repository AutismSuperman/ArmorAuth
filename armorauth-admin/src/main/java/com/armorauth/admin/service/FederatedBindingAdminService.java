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

import com.armorauth.admin.dto.FederatedBindingDTO;
import com.armorauth.common.audit.AuditContext;
import com.armorauth.common.exception.ResourceNotFoundException;
import com.armorauth.data.entity.UserInfo;
import com.armorauth.data.entity.UserFederatedBinding;
import com.armorauth.data.repository.UserInfoRepository;
import com.armorauth.data.repository.UserFederatedBindingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FederatedBindingAdminService {

    private final UserFederatedBindingRepository bindingRepository;
    private final UserInfoRepository userRepository;
    private final AuditEventService auditEventService;

    public FederatedBindingAdminService(UserFederatedBindingRepository bindingRepository,
                                        UserInfoRepository userRepository,
                                        AuditEventService auditEventService) {
        this.bindingRepository = bindingRepository;
        this.userRepository = userRepository;
        this.auditEventService = auditEventService;
    }

    @Transactional(readOnly = true)
    public Page<FederatedBindingDTO.Response> listBindings(String userId,
                                                           String registrationId,
                                                           Pageable pageable) {
        Page<UserFederatedBinding> bindings;
        if (hasText(userId) && hasText(registrationId)) {
            bindings = bindingRepository.findByUserIdAndRegistrationId(userId, registrationId, pageable);
        } else if (hasText(userId)) {
            bindings = bindingRepository.findByUserId(userId, pageable);
        } else if (hasText(registrationId)) {
            bindings = bindingRepository.findByRegistrationId(registrationId, pageable);
        } else {
            bindings = bindingRepository.findAll(pageable);
        }
        return bindings.map(this::toResponse);
    }

    @Transactional
    public void deleteBinding(String id) {
        UserFederatedBinding binding = bindingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("外部账号绑定", id));
        bindingRepository.delete(binding);

        auditEventService.record("FEDERATED_BINDING_DELETED",
                AuditContext.getCurrentPrincipal(), "federated_binding", id,
                "解除外部账号绑定: " + binding.getRegistrationId() + "/" + binding.getProviderUserId(),
                AuditContext.getClientIp());
    }

    private FederatedBindingDTO.Response toResponse(UserFederatedBinding binding) {
        UserInfo user = userRepository.findById(binding.getUserId()).orElse(null);
        return new FederatedBindingDTO.Response(
                binding.getId(),
                binding.getUserId(),
                user != null ? user.getUsername() : null,
                user != null ? user.getDisplayName() : null,
                user != null ? user.getEmail() : null,
                binding.getRegistrationId(),
                binding.getProviderUserId(),
                binding.getProviderUsername(),
                binding.getProviderAttributes(),
                binding.getCreateTime(),
                binding.getLastLoginTime()
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
