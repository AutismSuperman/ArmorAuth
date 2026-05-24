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
package com.armorauth.config;

import com.armorauth.crypto.SecretCryptoService;
import com.armorauth.mfa.TotpService;
import com.armorauth.security.LoginRateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 安全增强自动配置
 * <p>
 * 配置登录限流和 MFA 相关 Bean
 *
 * @author fulin
 * @since 2026-05-23
 */
@Configuration(proxyBeanMethods = false)
public class SecurityEnhancementConfiguration {

    /**
     * 登录限流器
     * <p>
     * 默认：5分钟内最多10次失败尝试
     * 可通过配置调整：
     * armorauth.login.rate-limit.max-attempts=10
     * armorauth.login.rate-limit.window-seconds=300
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "armorauth.login.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
    public LoginRateLimiter loginRateLimiter() {
        return new LoginRateLimiter(10, 300_000L);
    }

    /**
     * TOTP 服务
     */
    @Bean
    @ConditionalOnMissingBean
    public TotpService totpService(SecretCryptoService secretCryptoService) {
        return new TotpService(secretCryptoService);
    }

    /**
     * 可逆 secret 加密服务。
     */
    @Bean
    @ConditionalOnMissingBean
    public SecretCryptoService secretCryptoService(
            @Value("${armorauth.crypto.secret-key:${ARMORAUTH_CRYPTO_SECRET:ArmorAuth local development secret key change me}}")
            String secretKey,
            @Value("${armorauth.crypto.keys:${ARMORAUTH_CRYPTO_KEYS:}}")
            String keyRing,
            @Value("${armorauth.crypto.active-key-id:${ARMORAUTH_CRYPTO_ACTIVE_KEY_ID:v1}}")
            String activeKeyId) {
        return SecretCryptoService.fromProperties(secretKey, keyRing, activeKeyId);
    }
}
