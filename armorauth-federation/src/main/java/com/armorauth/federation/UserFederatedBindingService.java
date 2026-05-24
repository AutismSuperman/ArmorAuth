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

import com.armorauth.data.entity.UserFederatedBinding;
import com.armorauth.data.entity.UserInfo;
import com.armorauth.data.repository.UserFederatedBindingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class UserFederatedBindingService {

    private static final Logger log = LoggerFactory.getLogger(UserFederatedBindingService.class);

    private final UserFederatedBindingRepository userFederatedBindingRepository;

    public UserFederatedBindingService(UserFederatedBindingRepository userFederatedBindingRepository) {
        this.userFederatedBindingRepository = userFederatedBindingRepository;
    }

    @Transactional(readOnly = true)
    public Optional<UserFederatedBinding> findBinding(String registrationId, String providerUserId) {
        return this.userFederatedBindingRepository.findByRegistrationIdAndProviderUserId(registrationId, providerUserId);
    }

    @Transactional
    public UserFederatedBinding createOrUpdateBinding(UserInfo userInfo, FederatedUserProfile federatedUserProfile, Instant now) {
        Optional<UserFederatedBinding> existingBinding = findBinding(
                federatedUserProfile.registrationId(),
                federatedUserProfile.providerUserId()
        );
        if (existingBinding.isPresent()) {
            log.info(
                    "Updating existing federated binding registrationId={} userId={}",
                    federatedUserProfile.registrationId(),
                    userInfo.getId()
            );
            return updateExistingBinding(existingBinding.get(), userInfo, federatedUserProfile, now);
        }
        UserFederatedBinding binding = new UserFederatedBinding();
        binding.setUserId(userInfo.getId());
        binding.setRegistrationId(federatedUserProfile.registrationId());
        binding.setProviderUserId(federatedUserProfile.providerUserId());
        binding.setProviderUsername(federatedUserProfile.providerUsername());
        binding.setProviderAttributes(federatedUserProfile.providerAttributes());
        binding.setCreateTime(now);
        binding.setLastLoginTime(now);
        try {
            UserFederatedBinding savedBinding = this.userFederatedBindingRepository.save(binding);
            log.info(
                    "Created federated binding registrationId={} userId={}",
                    savedBinding.getRegistrationId(),
                    savedBinding.getUserId()
            );
            return savedBinding;
        } catch (DataIntegrityViolationException ex) {
            log.warn(
                    "Concurrent federated binding creation detected registrationId={} providerUserId={}",
                    federatedUserProfile.registrationId(),
                    federatedUserProfile.providerUserId()
            );
            UserFederatedBinding persisted = this.userFederatedBindingRepository
                    .findByRegistrationIdAndProviderUserId(
                            federatedUserProfile.registrationId(),
                            federatedUserProfile.providerUserId()
                    )
                    .orElseThrow(() -> ex);
            return updateExistingBinding(persisted, userInfo, federatedUserProfile, now);
        }
    }

    @Transactional
    public UserFederatedBinding touchLastLogin(UserFederatedBinding binding, Instant now) {
        binding.setLastLoginTime(now);
        log.debug("Refreshing federated binding lastLogin registrationId={} userId={}", binding.getRegistrationId(), binding.getUserId());
        return this.userFederatedBindingRepository.save(binding);
    }

    private UserFederatedBinding updateExistingBinding(UserFederatedBinding binding,
                                                       UserInfo userInfo,
                                                       FederatedUserProfile federatedUserProfile,
                                                       Instant now) {
        if (!userInfo.getId().equals(binding.getUserId())) {
            throw new IllegalStateException("该第三方账号已绑定其他本地账号。");
        }
        binding.setProviderUsername(federatedUserProfile.providerUsername());
        binding.setProviderAttributes(federatedUserProfile.providerAttributes());
        binding.setLastLoginTime(now);
        UserFederatedBinding savedBinding = this.userFederatedBindingRepository.save(binding);
        log.info(
                "Persisted federated binding registrationId={} userId={}",
                savedBinding.getRegistrationId(),
                savedBinding.getUserId()
        );
        return savedBinding;
    }
}
