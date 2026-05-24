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
package com.armorauth.common.validation;

import com.armorauth.common.exception.ValidationException;

import java.net.URI;
import java.util.Set;

/**
 * Redirect URI 校验器
 * <p>
 * 默认策略：
 * - 不允许通配符
 * - 生产环境要求 HTTPS
 * - 本地开发允许 localhost 的 HTTP
 *
 * @author fulin
 * @since 2026-05-23
 */
public final class RedirectUriValidator {

    private static final Set<String> LOCALHOST_HOSTS = Set.of(
            "localhost", "127.0.0.1", "[::1]", "0:0:0:0:0:0:0:1"
    );

    private RedirectUriValidator() {
    }

    /**
     * 校验 redirect URI 是否合法
     *
     * @param redirectUri    待校验的 redirect URI
     * @param allowHttpLocal 是否允许 localhost 的 HTTP（开发模式）
     * @throws ValidationException 如果校验不通过
     */
    public static void validate(String redirectUri, boolean allowHttpLocal) {
        if (redirectUri == null || redirectUri.isBlank()) {
            return;
        }

        // 不允许通配符
        if (redirectUri.contains("*")) {
            throw new ValidationException("Redirect URI 不允许使用通配符: " + redirectUri);
        }

        try {
            URI uri = URI.create(redirectUri);

            String scheme = uri.getScheme();
            if (scheme == null) {
                throw new ValidationException("Redirect URI 缺少 scheme: " + redirectUri);
            }

            // HTTPS 始终允许
            if ("https".equalsIgnoreCase(scheme)) {
                return;
            }

            // HTTP 仅允许 localhost
            if ("http".equalsIgnoreCase(scheme)) {
                if (!allowHttpLocal) {
                    throw new ValidationException("生产环境不允许 HTTP Redirect URI: " + redirectUri);
                }
                String host = uri.getHost();
                if (host == null || !LOCALHOST_HOSTS.contains(host.toLowerCase())) {
                    throw new ValidationException("HTTP Redirect URI 仅允许 localhost: " + redirectUri);
                }
                return;
            }

            // 自定义 scheme（如 myapp://callback）允许
            if (uri.getHost() != null || uri.getAuthority() != null) {
                return;
            }

            throw new ValidationException("无效的 Redirect URI: " + redirectUri);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("无效的 Redirect URI: " + redirectUri);
        }
    }

    /**
     * 校验多条 redirect URI（分号或换行分隔）
     */
    public static void validateAll(String redirectUris, boolean allowHttpLocal) {
        if (redirectUris == null || redirectUris.isBlank()) {
            return;
        }
        for (String uri : redirectUris.split("[;\\n]")) {
            String trimmed = uri.trim();
            if (!trimmed.isEmpty()) {
                validate(trimmed, allowHttpLocal);
            }
        }
    }
}
