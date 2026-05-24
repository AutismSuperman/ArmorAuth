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
package com.armorauth.security;

import com.armorauth.data.entity.UserInfo;
import com.armorauth.data.repository.UserInfoRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * 登录失败锁定服务
 * <p>
 * 连续失败5次锁定30分钟
 *
 * @author fulin
 * @since 2026-05-23
 */
@Service
public class LoginLockoutService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(30);

    private final UserInfoRepository userInfoRepository;

    public LoginLockoutService(UserInfoRepository userInfoRepository) {
        this.userInfoRepository = userInfoRepository;
    }

    /**
     * 检查账号是否被锁定
     */
    public boolean isLocked(String username) {
        Optional<UserInfo> userOpt = userInfoRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return false;
        }
        UserInfo user = userOpt.get();
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            return true;
        }
        // 锁定已过期，自动解锁
        if (user.getLockedUntil() != null && user.getLockedUntil().isBefore(Instant.now())) {
            user.setLockedUntil(null);
            user.setFailedLoginAttempts(0);
            userInfoRepository.save(user);
        }
        return false;
    }

    /**
     * 记录登录失败，达到阈值时锁定
     */
    public void recordFailure(String username) {
        Optional<UserInfo> userOpt = userInfoRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return;
        }
        UserInfo user = userOpt.get();
        int attempts = user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() + 1 : 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(Instant.now().plus(LOCKOUT_DURATION));
        }

        userInfoRepository.save(user);
    }

    /**
     * 登录成功时重置失败计数
     */
    public void recordSuccess(String username) {
        Optional<UserInfo> userOpt = userInfoRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return;
        }
        UserInfo user = userOpt.get();
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userInfoRepository.save(user);
    }
}
