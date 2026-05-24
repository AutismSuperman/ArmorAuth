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
package com.armorauth.admin.config;

import com.armorauth.common.audit.SecurityAuditEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Micrometer 指标配置，统计认证/授权相关指标
 *
 * @author fulin
 * @since 2026-05-23
 */
@Component
public class MetricsConfiguration {

    private final Counter loginSuccessCounter;
    private final Counter loginFailureCounter;
    private final Counter mfaChallengeCounter;
    private final Counter mfaSuccessCounter;
    private final Counter mfaFailureCounter;
    private final Counter tokenIssuedCounter;
    private final Counter tokenRefreshedCounter;
    private final Counter tokenRevokedCounter;

    public MetricsConfiguration(MeterRegistry meterRegistry) {
        this.loginSuccessCounter = Counter.builder("armorauth.login")
                .tag("result", "success")
                .description("登录成功次数")
                .register(meterRegistry);
        this.loginFailureCounter = Counter.builder("armorauth.login")
                .tag("result", "failure")
                .description("登录失败次数")
                .register(meterRegistry);
        this.mfaChallengeCounter = Counter.builder("armorauth.mfa")
                .tag("result", "challenge")
                .description("MFA 挑战次数")
                .register(meterRegistry);
        this.mfaSuccessCounter = Counter.builder("armorauth.mfa")
                .tag("result", "success")
                .description("MFA 验证成功次数")
                .register(meterRegistry);
        this.mfaFailureCounter = Counter.builder("armorauth.mfa")
                .tag("result", "failure")
                .description("MFA 验证失败次数")
                .register(meterRegistry);
        this.tokenIssuedCounter = Counter.builder("armorauth.token")
                .tag("action", "issued")
                .description("Token 签发次数")
                .register(meterRegistry);
        this.tokenRefreshedCounter = Counter.builder("armorauth.token")
                .tag("action", "refreshed")
                .description("Token 刷新次数")
                .register(meterRegistry);
        this.tokenRevokedCounter = Counter.builder("armorauth.token")
                .tag("action", "revoked")
                .description("Token 撤销次数")
                .register(meterRegistry);
    }

    @EventListener
    public void onSecurityAuditEvent(SecurityAuditEvent event) {
        switch (event.getEventType()) {
            case "LOGIN_SUCCESS" -> loginSuccessCounter.increment();
            case "LOGIN_FAILURE" -> loginFailureCounter.increment();
            case "MFA_CHALLENGE" -> mfaChallengeCounter.increment();
            case "MFA_SUCCESS" -> mfaSuccessCounter.increment();
            case "MFA_FAILURE" -> mfaFailureCounter.increment();
            case "TOKEN_ISSUED" -> tokenIssuedCounter.increment();
            case "TOKEN_REFRESHED" -> tokenRefreshedCounter.increment();
            case "TOKEN_REVOKED" -> tokenRevokedCounter.increment();
            default -> { /* ignore other event types */ }
        }
    }
}
