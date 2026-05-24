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
package com.armorauth.data.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * MFA认证因子表
 *
 * @author fulin
 * @since 2026-05-23
 */
@Data
@Entity
@Table(name = "auth_factor")
public class AuthFactor implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private String userId;

    /**
     * 因子类型: TOTP, EMAIL_OTP, SMS_OTP, WEBAUTHN
     */
    @Column(name = "factor_type", nullable = false, length = 50)
    private String factorType;

    /**
     * 因子名称（用户自定义，如"我的手机"）
     */
    @Column(name = "name", length = 100)
    private String name;

    /**
     * 密钥（TOTP secret, 加密存储）
     */
    @Column(name = "secret", length = 500)
    private String secret;

    /**
     * 恢复码（逗号分隔，加密存储）
     */
    @Column(name = "recovery_codes", columnDefinition = "text")
    private String recoveryCodes;

    /**
     * WebAuthn/Passkey 注册或认证 challenge（加密存储，完成后清空）
     */
    @Column(name = "webauthn_challenge", length = 500)
    private String webauthnChallenge;

    /**
     * WebAuthn credential ID
     */
    @Column(name = "credential_id", length = 500)
    private String credentialId;

    /**
     * WebAuthn credential public key（COSE 或服务端标准化格式）
     */
    @Column(name = "credential_public_key", columnDefinition = "text")
    private String credentialPublicKey;

    @Column(name = "sign_count")
    private Long signCount;

    @Column(name = "transports", length = 200)
    private String transports;

    @Column(name = "aaguid", length = 100)
    private String aaguid;

    @Column(name = "webauthn_user_handle", length = 500)
    private String webauthnUserHandle;

    @Column(name = "backup_eligible")
    private Boolean backupEligible;

    @Column(name = "backup_state")
    private Boolean backupState;

    /**
     * 是否已验证（首次绑定时需要验证）
     */
    @Column(name = "verified")
    private Boolean verified = false;

    /**
     * 是否启用
     */
    @Column(name = "enabled")
    private Boolean enabled = true;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * 最后使用时间
     */
    @Column(name = "last_used_at")
    private Instant lastUsedAt;
}
