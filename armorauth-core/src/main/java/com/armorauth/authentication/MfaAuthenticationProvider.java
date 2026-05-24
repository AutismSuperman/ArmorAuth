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
package com.armorauth.authentication;

import com.armorauth.crypto.SecretCryptoService;
import com.armorauth.data.entity.AuthFactor;
import com.armorauth.data.repository.AuthFactorRepository;
import com.armorauth.data.repository.UserInfoRepository;
import com.armorauth.mfa.TotpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * MFA 认证提供者
 * <p>
 * 验证 TOTP 验证码，支持恢复码
 *
 * @author fulin
 * @since 2026-05-23
 */
public class MfaAuthenticationProvider implements AuthenticationProvider {

    private static final Logger log = LoggerFactory.getLogger(MfaAuthenticationProvider.class);

    private final UserDetailsService userDetailsService;
    private final AuthFactorRepository authFactorRepository;
    private final TotpService totpService;
    private final UserInfoRepository userInfoRepository;
    private final SecretCryptoService secretCryptoService;

    public MfaAuthenticationProvider(UserDetailsService userDetailsService,
                                     AuthFactorRepository authFactorRepository,
                                     TotpService totpService) {
        this(userDetailsService, authFactorRepository, totpService, null, null);
    }

    public MfaAuthenticationProvider(UserDetailsService userDetailsService,
                                     AuthFactorRepository authFactorRepository,
                                     TotpService totpService,
                                     UserInfoRepository userInfoRepository,
                                     SecretCryptoService secretCryptoService) {
        this.userDetailsService = userDetailsService;
        this.authFactorRepository = authFactorRepository;
        this.totpService = totpService;
        this.userInfoRepository = userInfoRepository;
        this.secretCryptoService = secretCryptoService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (!(authentication instanceof MfaAuthenticationToken mfaToken)) {
            return null;
        }

        String principal = (String) mfaToken.getPrincipal();
        String mfaCode = (String) mfaToken.getCredentials();
        String factorId = mfaToken.getFactorId();
        String userId = resolveUserId(principal).orElse(principal);

        log.info("Processing MFA authentication for principal={}", principal);

        // 加载用户的 MFA 因子
        List<AuthFactor> factors;
        if (factorId != null && !factorId.isBlank()) {
            AuthFactor factor = authFactorRepository.findById(factorId).orElse(null);
            if (factor == null || !factor.getUserId().equals(userId)) {
                throw new BadCredentialsException("MFA factor not found");
            }
            factors = List.of(factor);
        } else {
            factors = authFactorRepository.findByUserIdAndEnabledTrue(userId);
        }

        if (factors.isEmpty()) {
            throw new BadCredentialsException("No MFA factors configured");
        }

        // 尝试验证 TOTP 码
        for (AuthFactor factor : factors) {
            if (!Boolean.TRUE.equals(factor.getVerified()) || !Boolean.TRUE.equals(factor.getEnabled())) {
                continue;
            }

            if ("TOTP".equals(factor.getFactorType())) {
                if (totpService.verifyCode(revealSecret(factor.getSecret()), mfaCode)) {
                    // TOTP 验证成功
                    factor.setLastUsedAt(Instant.now());
                    authFactorRepository.save(factor);
                    log.info("MFA TOTP authentication succeeded for principal={}", principal);
                    return createSuccessAuthentication(mfaToken);
                }
            }
        }

        // 尝试恢复码
        for (AuthFactor factor : factors) {
            if (factor.getRecoveryCodes() != null && !factor.getRecoveryCodes().isBlank()) {
                List<String> remainingCodes = totpService.verifyRecoveryCode(factor.getRecoveryCodes(), mfaCode);
                if (remainingCodes != null) {
                    // 恢复码验证成功，更新恢复码列表（移除已使用的）
                    factor.setRecoveryCodes(String.join(",", remainingCodes));
                    factor.setLastUsedAt(Instant.now());
                    authFactorRepository.save(factor);
                    log.info("MFA recovery code authentication succeeded for principal={}", principal);
                    return createSuccessAuthentication(mfaToken);
                }
            }
        }

        log.warn("MFA authentication failed for principal={}", principal);
        throw new BadCredentialsException("MFA code is incorrect");
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return MfaAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private Authentication createSuccessAuthentication(MfaAuthenticationToken token) {
        String principal = (String) token.getPrincipal();
        UserDetails userDetails = userDetailsService.loadUserByUsername(principal);
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        MfaAuthenticationToken authenticated = new MfaAuthenticationToken(principal, authorities);
        authenticated.setDetails(token.getDetails());
        return authenticated;
    }

    private Optional<String> resolveUserId(String username) {
        if (userInfoRepository == null) {
            return Optional.of(username);
        }
        return userInfoRepository.findByUsername(username)
                .map(user -> Optional.of(user.getId()))
                .orElseGet(() -> Optional.of(username));
    }

    private String revealSecret(String secret) {
        return secretCryptoService != null ? secretCryptoService.reveal(secret) : secret;
    }
}
