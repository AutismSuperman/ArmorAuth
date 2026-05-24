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

import com.armorauth.common.audit.SecurityAuditEvent;
import org.springframework.context.ApplicationEventPublisher;

/**
 * 安全审计事件发布器
 *
 * @author fulin
 * @since 2026-05-23
 */
public class AuditEventPublisher {

    private final ApplicationEventPublisher publisher;

    public AuditEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publishLoginSuccess(String username, String ipAddress, String detail) {
        publisher.publishEvent(new SecurityAuditEvent(
                this, "LOGIN_SUCCESS", username, "authentication", null, detail, ipAddress));
    }

    public void publishLoginFailure(String username, String ipAddress, String detail) {
        publisher.publishEvent(new SecurityAuditEvent(
                this, "LOGIN_FAILURE", username, "authentication", null, detail, ipAddress));
    }

    public void publishLogout(String username, String ipAddress, String detail) {
        publisher.publishEvent(new SecurityAuditEvent(
                this, "LOGOUT", username, "authentication", null, detail, ipAddress));
    }

    public void publishMfaChallenge(String username, String ipAddress, String detail) {
        publisher.publishEvent(new SecurityAuditEvent(
                this, "MFA_CHALLENGE", username, "mfa", null, detail, ipAddress));
    }

    public void publishMfaSuccess(String username, String ipAddress, String detail) {
        publisher.publishEvent(new SecurityAuditEvent(
                this, "MFA_SUCCESS", username, "mfa", null, detail, ipAddress));
    }

    public void publishMfaFailure(String username, String ipAddress, String detail) {
        publisher.publishEvent(new SecurityAuditEvent(
                this, "MFA_FAILURE", username, "mfa", null, detail, ipAddress));
    }

    public void publishTokenIssued(String clientId, String username, String ipAddress, String detail) {
        publisher.publishEvent(new SecurityAuditEvent(
                this, "TOKEN_ISSUED", username, "oauth2_token", clientId, detail, ipAddress));
    }

    public void publishTokenRefreshed(String clientId, String username, String ipAddress, String detail) {
        publisher.publishEvent(new SecurityAuditEvent(
                this, "TOKEN_REFRESHED", username, "oauth2_token", clientId, detail, ipAddress));
    }

    public void publishTokenRevoked(String clientId, String username, String ipAddress, String detail) {
        publisher.publishEvent(new SecurityAuditEvent(
                this, "TOKEN_REVOKED", username, "oauth2_token", clientId, detail, ipAddress));
    }

    public void publishAuthorizationConsent(String clientId, String username, String ipAddress, String detail) {
        publisher.publishEvent(new SecurityAuditEvent(
                this, "AUTHORIZATION_CONSENT", username, "oauth2_consent", clientId, detail, ipAddress));
    }
}
