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
package com.armorauth.admin.service;

import com.armorauth.admin.dto.AccountDTO;
import com.armorauth.authentication.EmailOtpService;
import com.armorauth.authentication.SmsOtpService;
import com.armorauth.common.audit.AuditContext;
import com.armorauth.common.exception.ResourceNotFoundException;
import com.armorauth.common.exception.ValidationException;
import com.armorauth.crypto.SecretCryptoService;
import com.armorauth.data.entity.AuthFactor;
import com.armorauth.data.entity.PasswordHistory;
import com.armorauth.data.entity.UserInfo;
import com.armorauth.data.repository.AuthFactorRepository;
import com.armorauth.data.repository.PasswordHistoryRepository;
import com.armorauth.data.repository.UserInfoRepository;
import com.armorauth.mfa.TotpService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

/**
 * 账户自助服务
 *
 * @author fulin
 * @since 2026-05-23
 */
@Service
public class AccountService {

    private static final String FACTOR_TYPE_TOTP = "TOTP";
    private static final String FACTOR_TYPE_WEBAUTHN = "WEBAUTHN";
    private static final long PASSKEY_TIMEOUT_MILLIS = 60_000L;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final List<String> RUNTIME_MFA_FACTOR_TYPES = List.of(FACTOR_TYPE_TOTP, FACTOR_TYPE_WEBAUTHN);

    private final UserInfoRepository userRepository;
    private final AuthFactorRepository authFactorRepository;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicyService;
    private final TotpService totpService;
    private final AuditEventService auditEventService;
    private final SecretCryptoService secretCryptoService;
    private final PasskeyRegistrationVerifier passkeyRegistrationVerifier;
    private final SmsOtpService smsOtpService;
    private final EmailOtpService emailOtpService;
    private final boolean exposeVerificationCode;

    public AccountService(UserInfoRepository userRepository,
                          AuthFactorRepository authFactorRepository,
                          PasswordHistoryRepository passwordHistoryRepository,
                          PasswordEncoder passwordEncoder,
                          PasswordPolicyService passwordPolicyService,
                          TotpService totpService,
                          AuditEventService auditEventService,
                          SecretCryptoService secretCryptoService,
                          PasskeyRegistrationVerifier passkeyRegistrationVerifier,
                          SmsOtpService smsOtpService,
                          EmailOtpService emailOtpService,
                          @Value("${armorauth.account.verification.expose-code:${armorauth.captcha.sms.expose-code:false}}")
                          boolean exposeVerificationCode) {
        this.userRepository = userRepository;
        this.authFactorRepository = authFactorRepository;
        this.passwordHistoryRepository = passwordHistoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyService = passwordPolicyService;
        this.totpService = totpService;
        this.auditEventService = auditEventService;
        this.secretCryptoService = secretCryptoService;
        this.passkeyRegistrationVerifier = passkeyRegistrationVerifier;
        this.smsOtpService = smsOtpService;
        this.emailOtpService = emailOtpService;
        this.exposeVerificationCode = exposeVerificationCode;
    }

    @Transactional(readOnly = true)
    public AccountDTO.ProfileResponse getProfile(String username) {
        UserInfo user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户", username));
        return toProfileResponse(user);
    }

    @Transactional
    public AccountDTO.ProfileResponse updateProfile(String username, AccountDTO.UpdateProfileRequest request) {
        UserInfo user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户", username));

        if (request.displayName() != null) {
            user.setDisplayName(request.displayName());
        }
        if (request.email() != null) {
            String email = trimToNull(request.email());
            if (!equalsNullable(email, user.getEmail())) {
                user.setEmailVerified(false);
            }
            user.setEmail(email);
        }
        if (request.phone() != null) {
            String phone = trimToNull(request.phone());
            if (!equalsNullable(phone, user.getPhone())) {
                user.setPhoneVerified(false);
            }
            user.setPhone(phone);
        }
        if (request.avatar() != null) {
            user.setAvatar(request.avatar());
        }
        if (request.profile() != null) {
            user.setProfile(request.profile());
        }
        user.setUpdateTime(Instant.now());
        user = userRepository.save(user);

        auditEventService.record("ACCOUNT_PROFILE_UPDATED",
                AuditContext.getCurrentPrincipal(), "user", user.getId(),
                "用户更新个人资料", AuditContext.getClientIp());

        return toProfileResponse(user);
    }

    @Transactional
    public AccountDTO.ContactVerificationCodeResponse sendContactVerificationCode(String username, String channel) {
        UserInfo user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户", username));
        ContactTarget target = contactTarget(user, channel);
        if (!StringUtils.hasText(target.value())) {
            throw new ValidationException(target.displayName() + "不能为空");
        }
        String code = switch (target.channel()) {
            case "email" -> emailOtpService.generateOtp(target.value());
            case "phone" -> smsOtpService.generateOtp(target.value());
            default -> throw new ValidationException("不支持的验证类型");
        };

        auditEventService.record("ACCOUNT_CONTACT_VERIFICATION_SENT",
                AuditContext.getCurrentPrincipal(), "user", user.getId(),
                "用户发送" + target.displayName() + "验证码", AuditContext.getClientIp());

        String message = target.displayName() + "验证码已发送。";
        return new AccountDTO.ContactVerificationCodeResponse(
                target.channel(),
                maskContact(target.channel(), target.value()),
                exposeVerificationCode ? message + " Mock 验证码：" + code + "。" : message,
                exposeVerificationCode ? code : null
        );
    }

    @Transactional
    public AccountDTO.ProfileResponse verifyContact(String username, String channel, AccountDTO.VerifyContactRequest request) {
        UserInfo user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户", username));
        ContactTarget target = contactTarget(user, channel);
        if (!StringUtils.hasText(target.value())) {
            throw new ValidationException(target.displayName() + "不能为空");
        }
        String code = request != null ? request.code() : null;
        boolean verified = switch (target.channel()) {
            case "email" -> emailOtpService.verifyOtp(target.value(), code);
            case "phone" -> smsOtpService.verifyOtp(target.value(), code);
            default -> throw new ValidationException("不支持的验证类型");
        };
        if (!verified) {
            throw new ValidationException("验证码不正确或已过期");
        }

        if ("email".equals(target.channel())) {
            user.setEmailVerified(true);
        } else {
            user.setPhoneVerified(true);
        }
        user.setUpdateTime(Instant.now());
        user = userRepository.save(user);

        auditEventService.record("ACCOUNT_CONTACT_VERIFIED",
                AuditContext.getCurrentPrincipal(), "user", user.getId(),
                "用户完成" + target.displayName() + "验证", AuditContext.getClientIp());

        return toProfileResponse(user);
    }

    @Transactional
    public void changePassword(String username, AccountDTO.ChangePasswordRequest request) {
        UserInfo user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户", username));

        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new ValidationException("旧密码不正确");
        }

        List<String> errors = passwordPolicyService.validate(request.newPassword());
        if (!errors.isEmpty()) {
            throw new ValidationException("密码不符合策略: " + String.join("; ", errors));
        }

        // 检查密码历史（最近5个）
        List<PasswordHistory> history = passwordHistoryRepository.findTop5ByUserIdOrderByCreatedAtDesc(user.getId());
        for (PasswordHistory h : history) {
            if (passwordEncoder.matches(request.newPassword(), h.getPasswordHash())) {
                throw new ValidationException("不能使用最近5次使用过的密码");
            }
        }

        // 保存旧密码到历史
        PasswordHistory ph = new PasswordHistory();
        ph.setUserId(user.getId());
        ph.setPasswordHash(user.getPassword());
        ph.setCreatedAt(Instant.now());
        passwordHistoryRepository.save(ph);

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setPasswordChangedAt(Instant.now());
        user.setUpdateTime(Instant.now());
        userRepository.save(user);

        auditEventService.record("ACCOUNT_PASSWORD_CHANGED",
                AuditContext.getCurrentPrincipal(), "user", user.getId(),
                "用户修改密码", AuditContext.getClientIp());
    }

    @Transactional(readOnly = true)
    public List<AccountDTO.FactorResponse> listFactors(String username) {
        UserInfo user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户", username));
        return authFactorRepository.findByUserId(user.getId()).stream()
                .map(this::toFactorResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountDTO.SecurityResponse getSecurity(String username) {
        UserInfo user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户", username));
        return toSecurityResponse(user, authFactorRepository.findByUserId(user.getId()));
    }

    @Transactional
    public AccountDTO.SecurityResponse updateMfaPreference(
            String username,
            AccountDTO.UpdateMfaPreferenceRequest request
    ) {
        UserInfo user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户", username));
        boolean enabled = request != null && Boolean.TRUE.equals(request.enabled());
        List<AuthFactor> factors = authFactorRepository.findByUserId(user.getId());

        if (enabled && !hasReadyRuntimeFactor(factors)) {
            throw new ValidationException("请先添加并验证一种 MFA 方法");
        }

        user.setMfaEnabled(enabled);
        user.setUpdateTime(Instant.now());
        user = userRepository.save(user);

        auditEventService.record(enabled ? "ACCOUNT_MFA_ENABLED" : "ACCOUNT_MFA_DISABLED",
                AuditContext.getCurrentPrincipal(), "user", user.getId(),
                enabled ? "用户启用登录 MFA" : "用户关闭登录 MFA", AuditContext.getClientIp());

        return toSecurityResponse(user, factors);
    }

    @Transactional
    public AccountDTO.TotpSetupResponse setupTotp(String username) {
        UserInfo user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户", username));

        // 检查是否已有未验证的 TOTP 因子
        List<AuthFactor> existingFactors = authFactorRepository
                .findByUserIdAndFactorType(user.getId(), FACTOR_TYPE_TOTP);
        for (AuthFactor factor : existingFactors) {
            if (!Boolean.TRUE.equals(factor.getVerified())) {
                // 删除未验证的旧因子
                authFactorRepository.delete(factor);
            }
        }

        String secret = totpService.generateSecret();
        String uri = totpService.generateUri(secret, user.getEmail() != null ? user.getEmail() : user.getUsername(), "ArmorAuth");
        List<String> recoveryCodes = totpService.generateRecoveryCodes();

        AuthFactor factor = new AuthFactor();
        factor.setUserId(user.getId());
        factor.setFactorType(FACTOR_TYPE_TOTP);
        factor.setName("身份验证器");
        factor.setSecret(secretCryptoService.protect(secret));
        factor.setRecoveryCodes(String.join(",", recoveryCodes));
        factor.setVerified(false);
        factor.setEnabled(true);
        factor.setCreatedAt(Instant.now());
        factor = authFactorRepository.save(factor);

        auditEventService.record("MFA_TOTP_SETUP",
                AuditContext.getCurrentPrincipal(), "auth_factor", factor.getId(),
                "用户初始化 TOTP 绑定", AuditContext.getClientIp());

        return new AccountDTO.TotpSetupResponse(factor.getId(), secret, uri, toQrCodeDataUri(uri), recoveryCodes);
    }

    @Transactional
    public AccountDTO.PasskeyBeginRegistrationResponse beginPasskeyRegistration(
            String username, AccountDTO.PasskeyBeginRegistrationRequest request) {
        UserInfo user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户", username));

        authFactorRepository.findByUserIdAndFactorType(user.getId(), FACTOR_TYPE_WEBAUTHN).stream()
                .filter(factor -> !Boolean.TRUE.equals(factor.getVerified()))
                .filter(factor -> factor.getCredentialId() == null || factor.getCredentialId().isBlank())
                .forEach(authFactorRepository::delete);

        String challenge = randomBase64Url(32);
        String rpId = defaultIfBlank(request != null ? request.rpId() : null, "127.0.0.1");
        String rpName = defaultIfBlank(request != null ? request.rpName() : null, "ArmorAuth");
        String userHandle = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(user.getId().getBytes(StandardCharsets.UTF_8));

        AuthFactor factor = new AuthFactor();
        factor.setUserId(user.getId());
        factor.setFactorType(FACTOR_TYPE_WEBAUTHN);
        factor.setName(defaultIfBlank(request != null ? request.name() : null, "Passkey"));
        factor.setWebauthnChallenge(secretCryptoService.protect(challenge));
        factor.setSecret(secretCryptoService.protect(passkeyRegistrationState(challenge, rpId, rpName)));
        factor.setWebauthnUserHandle(userHandle);
        factor.setVerified(false);
        factor.setEnabled(true);
        factor.setCreatedAt(Instant.now());
        factor = authFactorRepository.save(factor);

        auditEventService.record("PASSKEY_REGISTRATION_STARTED",
                AuditContext.getCurrentPrincipal(), "auth_factor", factor.getId(),
                "用户初始化 Passkey 注册", AuditContext.getClientIp());

        List<String> excludeCredentialIds = authFactorRepository
                .findByUserIdAndFactorType(user.getId(), FACTOR_TYPE_WEBAUTHN).stream()
                .filter(existing -> existing.getCredentialId() != null && !existing.getCredentialId().isBlank())
                .map(AuthFactor::getCredentialId)
                .toList();

        return new AccountDTO.PasskeyBeginRegistrationResponse(
                factor.getId(),
                challenge,
                rpId,
                rpName,
                PASSKEY_TIMEOUT_MILLIS,
                userHandle,
                user.getUsername(),
                user.getDisplayName(),
                excludeCredentialIds,
                List.of("ES256", "RS256"),
                "none",
                "attestation_object_verified_then_assertion_ready"
        );
    }

    @Transactional
    public AccountDTO.FactorResponse finishPasskeyRegistration(
            String username, String factorId, AccountDTO.PasskeyFinishRegistrationRequest request) {
        UserInfo user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户", username));

        AuthFactor factor = authFactorRepository.findById(factorId)
                .orElseThrow(() -> new ResourceNotFoundException("MFA 因子", factorId));
        if (!factor.getUserId().equals(user.getId()) || !FACTOR_TYPE_WEBAUTHN.equals(factor.getFactorType())) {
            throw new ResourceNotFoundException("MFA 因子", factorId);
        }

        String expectedChallenge = secretCryptoService.reveal(factor.getWebauthnChallenge());
        if (expectedChallenge == null || !expectedChallenge.equals(request.challenge())) {
            throw new ValidationException("Passkey challenge 不匹配或已过期");
        }

        PasskeyRegistrationVerifier.VerifiedRegistration verified = null;
        String credentialId = request.credentialId();
        String credentialPublicKey = request.publicKey();
        Long signCount = request.signCount();
        String aaguid = request.aaguid();
        Boolean backupEligible = request.backupEligible();
        Boolean backupState = request.backupState();
        if (passkeyRegistrationVerifier.hasAttestation(request)) {
            verified = passkeyRegistrationVerifier.verify(request, expectedChallenge, expectedRpId(factor));
            credentialId = verified.credentialId();
            credentialPublicKey = verified.credentialPublicKey();
            signCount = verified.signCount();
            aaguid = verified.aaguid();
            backupEligible = verified.backupEligible();
            backupState = verified.backupState();
        } else if (credentialId == null || credentialId.isBlank()
                || credentialPublicKey == null || credentialPublicKey.isBlank()) {
            throw new ValidationException("Passkey 注册必须提供 attestationObject/clientDataJSON，或兼容模式 credentialId/publicKey");
        }

        authFactorRepository.findByCredentialId(credentialId)
                .filter(existing -> !existing.getId().equals(factorId))
                .ifPresent(existing -> {
                    throw new ValidationException("Passkey credentialId 已存在");
                });

        factor.setName(defaultIfBlank(request.name(), factor.getName()));
        factor.setCredentialId(credentialId);
        factor.setCredentialPublicKey(credentialPublicKey);
        factor.setSignCount(signCount != null ? signCount : 0L);
        factor.setTransports(request.transports());
        factor.setAaguid(aaguid);
        factor.setWebauthnUserHandle(defaultIfBlank(request.userHandle(), factor.getWebauthnUserHandle()));
        factor.setBackupEligible(backupEligible);
        factor.setBackupState(backupState);
        factor.setWebauthnChallenge(null);
        factor.setSecret(null);
        factor.setVerified(true);
        factor.setEnabled(true);
        factor = authFactorRepository.save(factor);

        auditEventService.record("PASSKEY_REGISTERED",
                AuditContext.getCurrentPrincipal(), "auth_factor", factor.getId(),
                verified != null
                        ? "用户完成 Passkey attestation 注册验证: fmt=" + verified.attestationFormat()
                        : "用户保存 Passkey 凭据元数据（兼容模式）",
                AuditContext.getClientIp());

        return toFactorResponse(factor);
    }

    @Transactional
    public void verifyFactor(String username, String factorId, String code) {
        UserInfo user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户", username));

        AuthFactor factor = authFactorRepository.findById(factorId)
                .orElseThrow(() -> new ResourceNotFoundException("MFA 因子", factorId));

        if (!factor.getUserId().equals(user.getId())) {
            throw new ResourceNotFoundException("MFA 因子", factorId);
        }

        if (FACTOR_TYPE_TOTP.equals(factor.getFactorType())) {
            if (!totpService.verifyCode(secretCryptoService.reveal(factor.getSecret()), code)) {
                throw new ValidationException("验证码不正确");
            }
        }

        factor.setVerified(true);
        factor.setEnabled(true);
        factor.setLastUsedAt(Instant.now());
        authFactorRepository.save(factor);

        auditEventService.record("MFA_FACTOR_VERIFIED",
                AuditContext.getCurrentPrincipal(), "auth_factor", factorId,
                "用户验证 MFA 因子: " + factor.getFactorType(), AuditContext.getClientIp());
    }

    @Transactional
    public void deleteFactor(String username, String factorId) {
        UserInfo user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户", username));

        AuthFactor factor = authFactorRepository.findById(factorId)
                .orElseThrow(() -> new ResourceNotFoundException("MFA 因子", factorId));

        if (!factor.getUserId().equals(user.getId())) {
            throw new ResourceNotFoundException("MFA 因子", factorId);
        }

        authFactorRepository.delete(factor);

        if (isReadyRuntimeFactor(factor)) {
            boolean hasOtherReadyFactor = authFactorRepository.findByUserId(user.getId()).stream()
                    .filter(existing -> !factorId.equals(existing.getId()))
                    .anyMatch(this::isReadyRuntimeFactor);
            if (!hasOtherReadyFactor && Boolean.TRUE.equals(user.getMfaEnabled())) {
                user.setMfaEnabled(false);
                user.setUpdateTime(Instant.now());
                userRepository.save(user);
            }
        }

        auditEventService.record("MFA_FACTOR_DELETED",
                AuditContext.getCurrentPrincipal(), "auth_factor", factorId,
                "用户删除 MFA 因子: " + factor.getFactorType(), AuditContext.getClientIp());
    }

    private AccountDTO.ProfileResponse toProfileResponse(UserInfo user) {
        return new AccountDTO.ProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                user.getPhone(),
                user.getAvatar(),
                Boolean.TRUE.equals(user.getMfaEnabled()),
                user.getEmailVerified(),
                user.getPhoneVerified(),
                user.getLastLoginTime(),
                user.getProfile()
        );
    }

    private AccountDTO.SecurityResponse toSecurityResponse(UserInfo user, List<AuthFactor> factors) {
        boolean hasVerifiedFactor = factors.stream()
                .anyMatch(factor -> Boolean.TRUE.equals(factor.getVerified()));
        boolean hasRuntimeFactor = hasReadyRuntimeFactor(factors);
        return new AccountDTO.SecurityResponse(
                Boolean.TRUE.equals(user.getMfaEnabled()),
                hasVerifiedFactor,
                hasRuntimeFactor,
                Boolean.TRUE.equals(user.getMfaEnabled()) && hasRuntimeFactor,
                factors.size(),
                factors.stream().map(this::toFactorResponse).toList()
        );
    }

    private AccountDTO.FactorResponse toFactorResponse(AuthFactor factor) {
        return new AccountDTO.FactorResponse(
                factor.getId(),
                factor.getFactorType(),
                factor.getName(),
                factor.getVerified(),
                factor.getEnabled(),
                factor.getCreatedAt(),
                factor.getLastUsedAt(),
                runtimeSupport(factor)
        );
    }

    private boolean hasReadyRuntimeFactor(List<AuthFactor> factors) {
        return factors.stream().anyMatch(this::isReadyRuntimeFactor);
    }

    private boolean isReadyRuntimeFactor(AuthFactor factor) {
        return Boolean.TRUE.equals(factor.getEnabled())
                && Boolean.TRUE.equals(factor.getVerified())
                && RUNTIME_MFA_FACTOR_TYPES.contains(factor.getFactorType());
    }

    private String runtimeSupport(AuthFactor factor) {
        return switch (factor.getFactorType()) {
            case FACTOR_TYPE_TOTP -> "ready";
            case FACTOR_TYPE_WEBAUTHN -> factor.getCredentialPublicKey() != null && !factor.getCredentialPublicKey().isBlank()
                    ? "passkey_assertion_ready"
                    : "metadata_only_runtime_pending";
            default -> "unknown";
        };
    }

    private String randomBase64Url(int byteLength) {
        byte[] bytes = new byte[byteLength];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String passkeyRegistrationState(String challenge, String rpId, String rpName) {
        return challenge + "\n" + rpId + "\n" + rpName;
    }

    private String expectedRpId(AuthFactor factor) {
        String secret = secretCryptoService.reveal(factor.getSecret());
        if (secret == null || secret.isBlank()) {
            return "127.0.0.1";
        }
        String[] parts = secret.split("\\R", -1);
        return parts.length > 1 && !parts[1].isBlank() ? parts[1] : "127.0.0.1";
    }

    private ContactTarget contactTarget(UserInfo user, String channel) {
        String normalized = channel == null ? "" : channel.trim().toLowerCase();
        return switch (normalized) {
            case "email" -> new ContactTarget("email", "邮箱", user.getEmail());
            case "phone" -> new ContactTarget("phone", "手机号", user.getPhone());
            default -> throw new ValidationException("不支持的验证类型");
        };
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private boolean equalsNullable(String left, String right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }

    private String maskContact(String channel, String value) {
        if (!StringUtils.hasText(value)) {
            return "-";
        }
        if ("email".equals(channel)) {
            int at = value.indexOf('@');
            if (at <= 1) {
                return "***" + (at >= 0 ? value.substring(at) : "");
            }
            return value.substring(0, Math.min(2, at)) + "***" + value.substring(at);
        }
        if (value.length() < 7) {
            return "***";
        }
        return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
    }

    private String toQrCodeDataUri(String value) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix matrix = qrCodeWriter.encode(value, BarcodeFormat.QR_CODE, 220, 220);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                ImageIO.write(image, "PNG", output);
                return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
            }
        } catch (IOException | WriterException ex) {
            return null;
        }
    }

    private record ContactTarget(String channel, String displayName, String value) {
    }
}
