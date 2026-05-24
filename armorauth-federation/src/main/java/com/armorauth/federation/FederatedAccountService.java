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

import com.armorauth.data.entity.UserInfo;
import com.armorauth.data.repository.UserInfoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class FederatedAccountService {

    private static final Logger log = LoggerFactory.getLogger(FederatedAccountService.class);

    private static final int MAX_USERNAME_LENGTH = 32;

    private final UserInfoRepository userInfoRepository;

    private final PasswordEncoder passwordEncoder;

    public FederatedAccountService(UserInfoRepository userInfoRepository, PasswordEncoder passwordEncoder) {
        this.userInfoRepository = userInfoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Optional<UserInfo> findByUsername(String username) {
        return this.userInfoRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public UserInfo getRequiredUser(String userId) {
        return this.userInfoRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("联合登录绑定的本地账号不存在。"));
    }

    @Transactional(readOnly = true)
    public Optional<UserInfo> authenticateLocalUser(String username, String rawPassword) {
        return findByUsername(username)
                .filter(user -> this.passwordEncoder.matches(rawPassword, user.getPassword()));
    }

    @Transactional
    public UserInfo createLocalUser(String username, String rawPassword, String displayName, Instant now) {
        String normalizedUsername = username == null ? null : username.trim();
        if (!StringUtils.hasText(normalizedUsername)) {
            throw new IllegalArgumentException("用户名不能为空。");
        }
        if (this.userInfoRepository.existsByUsername(normalizedUsername)) {
            throw new IllegalStateException("用户名已存在，请更换后重试。");
        }
        UserInfo userInfo = new UserInfo();
        userInfo.setUsername(normalizedUsername);
        userInfo.setDisplayName(StringUtils.hasText(displayName) ? displayName : normalizedUsername);
        userInfo.setPassword(this.passwordEncoder.encode(rawPassword));
        userInfo.setStatus(0);
        userInfo.setCreateTime(now);
        userInfo.setLastLoginTime(now);
        UserInfo savedUser = this.userInfoRepository.save(userInfo);
        log.info("Created local user for federated flow username={} userId={}", savedUser.getUsername(), savedUser.getId());
        return savedUser;
    }

    @Transactional
    public UserInfo createAutoRegisteredUser(FederatedUserProfile federatedUserProfile, Instant now) {
        String username = generateAvailableUsername(
                federatedUserProfile.registrationId(),
                federatedUserProfile.providerUserId(),
                federatedUserProfile.displayName()
        );
        String rawPassword = UUID.randomUUID().toString().replace("-", "");
        String displayName = StringUtils.hasText(federatedUserProfile.displayName())
                ? federatedUserProfile.displayName()
                : username;
        log.info(
                "Creating auto-registered local user registrationId={} username={}",
                federatedUserProfile.registrationId(),
                username
        );
        return createLocalUser(username, rawPassword, displayName, now);
    }

    @Transactional(readOnly = true)
    public String generateAvailableUsername(String registrationId, String providerUserId, String displayNameCandidate) {
        String baseUsername = normalizeUsername(displayNameCandidate);
        if (!StringUtils.hasText(baseUsername)) {
            baseUsername = fallbackUsername(registrationId, providerUserId);
        }
        String candidate = abbreviate(baseUsername, MAX_USERNAME_LENGTH);
        int suffix = 1;
        while (this.userInfoRepository.existsByUsername(candidate)) {
            String candidateSuffix = String.valueOf(suffix++);
            candidate = abbreviate(baseUsername, MAX_USERNAME_LENGTH - candidateSuffix.length()) + candidateSuffix;
        }
        log.debug("Generated available username registrationId={} username={}", registrationId, candidate);
        return candidate;
    }

    public Instant currentTimestamp() {
        return Instant.now();
    }

    private String normalizeUsername(String rawCandidate) {
        if (!StringUtils.hasText(rawCandidate)) {
            return "";
        }
        String normalized = Normalizer.normalize(rawCandidate, Normalizer.Form.NFKC).trim();
        normalized = normalized.replaceAll("\\s+", "_");
        normalized = normalized.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}_-]", "_");
        normalized = normalized.replaceAll("_+", "_");
        normalized = normalized.replaceAll("^[_.-]+|[_.-]+$", "");
        return abbreviate(normalized, MAX_USERNAME_LENGTH);
    }

    private String fallbackUsername(String registrationId, String providerUserId) {
        String sanitizedProviderUserId = providerUserId == null ? "" : providerUserId.replaceAll("[^A-Za-z0-9]", "");
        if (!StringUtils.hasText(sanitizedProviderUserId)) {
            sanitizedProviderUserId = UUID.randomUUID().toString().replace("-", "");
        }
        String providerFragment = abbreviate(sanitizedProviderUserId, 8);
        return normalizeUsername(registrationId + "_" + providerFragment);
    }

    private String abbreviate(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
