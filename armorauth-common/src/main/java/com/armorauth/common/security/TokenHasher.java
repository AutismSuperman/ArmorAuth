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
package com.armorauth.common.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Token 哈希工具
 * <p>
 * 用于在数据库中存储 token 时使用 SHA-256 哈希代替明文，
 * 提升安全性。适用于 access token、refresh token、authorization code 等。
 *
 * @author fulin
 * @since 2026-05-23
 */
public final class TokenHasher {

    private TokenHasher() {
    }

    /**
     * 计算 token 的 SHA-256 哈希值
     *
     * @param token 原始 token 值
     * @return 十六进制哈希字符串
     */
    public static String hash(String token) {
        if (token == null || token.isEmpty()) {
            return token;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * 验证 token 是否匹配哈希值
     *
     * @param token      原始 token
     * @param hashedToken 存储的哈希值
     * @return true 如果匹配
     */
    public static boolean matches(String token, String hashedToken) {
        if (token == null || hashedToken == null) {
            return false;
        }
        return hash(token).equals(hashedToken);
    }
}
