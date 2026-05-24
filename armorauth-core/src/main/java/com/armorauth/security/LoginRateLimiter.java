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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 登录限流器
 * <p>
 * 按用户名和IP地址进行登录失败次数统计和限流。
 * 超过阈值后在指定时间窗口内拒绝登录请求。
 *
 * @author fulin
 * @since 2026-05-23
 */
public class LoginRateLimiter {

    private final int maxAttempts;
    private final long windowMs;
    private final Map<String, AttemptEntry> attempts = new ConcurrentHashMap<>();

    /**
     * @param maxAttempts 时间窗口内最大失败次数
     * @param windowMs    时间窗口（毫秒），默认 300000 (5分钟)
     */
    public LoginRateLimiter(int maxAttempts, long windowMs) {
        this.maxAttempts = maxAttempts;
        this.windowMs = windowMs;
    }

    /**
     * 检查是否被限流
     *
     * @param key 限流键（用户名或IP）
     * @return true 表示被限流，应拒绝登录
     */
    public boolean isBlocked(String key) {
        if (key == null) return false;
        AttemptEntry entry = attempts.get(key);
        if (entry == null) return false;
        if (System.currentTimeMillis() > entry.windowEnd) {
            attempts.remove(key);
            return false;
        }
        return entry.count.get() >= maxAttempts;
    }

    /**
     * 检查用户名和IP是否被限流
     *
     * @param username 用户名
     * @param ip       客户端IP
     * @return true 表示被限流
     */
    public boolean isBlocked(String username, String ip) {
        return isBlocked(username) || isBlocked(ip);
    }

    /**
     * 检查用户名、IP和客户端是否被限流
     *
     * @param username 用户名
     * @param ip       客户端IP
     * @param clientId 客户端ID
     * @return true 表示被限流
     */
    public boolean isBlocked(String username, String ip, String clientId) {
        return isBlocked(username) || isBlocked(ip) || (clientId != null && isBlocked(clientId));
    }

    /**
     * 记录一次登录失败
     *
     * @param key 限流键（用户名或IP）
     */
    public void recordFailure(String key) {
        if (key == null) return;
        long now = System.currentTimeMillis();
        attempts.compute(key, (k, existing) -> {
            if (existing == null || now > existing.windowEnd) {
                return new AttemptEntry(1, now + windowMs);
            }
            existing.count.incrementAndGet();
            return existing;
        });
    }

    /**
     * 记录登录失败（用户名、IP和客户端同时记录）
     *
     * @param username 用户名
     * @param ip       客户端IP
     * @param clientId 客户端ID
     */
    public void recordFailure(String username, String ip, String clientId) {
        recordFailure(username);
        recordFailure(ip);
        if (clientId != null) {
            recordFailure(clientId);
        }
    }

    /**
     * 记录登录失败（用户名和IP同时记录）
     *
     * @param username 用户名
     * @param ip       客户端IP
     */
    public void recordFailure(String username, String ip) {
        recordFailure(username);
        recordFailure(ip);
    }

    /**
     * 登录成功后清除该用户的失败记录
     *
     * @param key 限流键
     */
    public void clearFailures(String key) {
        if (key != null) {
            attempts.remove(key);
        }
    }

    /**
     * 获取当前失败次数
     *
     * @param key 限流键
     * @return 失败次数
     */
    public int getFailureCount(String key) {
        if (key == null) return 0;
        AttemptEntry entry = attempts.get(key);
        if (entry == null) return 0;
        if (System.currentTimeMillis() > entry.windowEnd) {
            attempts.remove(key);
            return 0;
        }
        return entry.count.get();
    }

    /**
     * 清理过期条目
     */
    public void cleanup() {
        long now = System.currentTimeMillis();
        attempts.entrySet().removeIf(entry -> now > entry.getValue().windowEnd);
    }

    private static class AttemptEntry {
        final AtomicInteger count;
        final long windowEnd;

        AttemptEntry(int count, long windowEnd) {
            this.count = new AtomicInteger(count);
            this.windowEnd = windowEnd;
        }
    }
}
