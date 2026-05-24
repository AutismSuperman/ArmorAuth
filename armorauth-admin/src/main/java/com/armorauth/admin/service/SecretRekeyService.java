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

import com.armorauth.admin.dto.SecretProtectionDTO;
import com.armorauth.common.audit.AuditContext;
import com.armorauth.crypto.SecretCryptoService;
import com.armorauth.data.entity.AuthFactor;
import com.armorauth.data.entity.IdentityProvider;
import com.armorauth.data.entity.JwkKey;
import com.armorauth.data.entity.WebhookEndpoint;
import com.armorauth.data.repository.AuthFactorRepository;
import com.armorauth.data.repository.IdentityProviderRepository;
import com.armorauth.data.repository.JwkKeyRepository;
import com.armorauth.data.repository.WebhookEndpointRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Rewrites protected secrets with the currently active crypto key.
 */
@Service
public class SecretRekeyService {

    private static final Logger log = LoggerFactory.getLogger(SecretRekeyService.class);

    private final SecretCryptoService secretCryptoService;
    private final IdentityProviderRepository identityProviderRepository;
    private final WebhookEndpointRepository webhookEndpointRepository;
    private final AuthFactorRepository authFactorRepository;
    private final JwkKeyRepository jwkKeyRepository;
    private final AuditEventService auditEventService;

    public SecretRekeyService(SecretCryptoService secretCryptoService,
                              IdentityProviderRepository identityProviderRepository,
                              WebhookEndpointRepository webhookEndpointRepository,
                              AuthFactorRepository authFactorRepository,
                              JwkKeyRepository jwkKeyRepository,
                              AuditEventService auditEventService) {
        this.secretCryptoService = secretCryptoService;
        this.identityProviderRepository = identityProviderRepository;
        this.webhookEndpointRepository = webhookEndpointRepository;
        this.authFactorRepository = authFactorRepository;
        this.jwkKeyRepository = jwkKeyRepository;
        this.auditEventService = auditEventService;
    }

    @Transactional
    public SecretProtectionDTO.RekeyResponse rekey(boolean dryRun) {
        MutableRekeyStats identityProviders = new MutableRekeyStats();
        MutableRekeyStats webhookEndpoints = new MutableRekeyStats();
        MutableRekeyStats authFactors = new MutableRekeyStats();
        MutableRekeyStats jwkKeys = new MutableRekeyStats();

        rekeyIdentityProviderSecrets(identityProviders, dryRun);
        rekeyWebhookSecrets(webhookEndpoints, dryRun);
        rekeyAuthFactorSecrets(authFactors, dryRun);
        rekeyJwkPrivateKeys(jwkKeys, dryRun);

        MutableRekeyStats total = MutableRekeyStats.totalOf(
                identityProviders, webhookEndpoints, authFactors, jwkKeys);
        recordAuditEvent(dryRun, total);

        return new SecretProtectionDTO.RekeyResponse(
                secretCryptoService.activeKeyId(),
                new ArrayList<>(secretCryptoService.keyIds()),
                dryRun,
                identityProviders.toDto(),
                webhookEndpoints.toDto(),
                authFactors.toDto(),
                jwkKeys.toDto(),
                total.toDto()
        );
    }

    private void rekeyIdentityProviderSecrets(MutableRekeyStats stats, boolean dryRun) {
        for (IdentityProvider provider : identityProviderRepository.findAll()) {
            String rekeyed = rekeySecret("identity_provider", provider.getId(), "client_secret",
                    provider.getClientSecret(), stats, dryRun);
            if (rekeyed != null) {
                provider.setClientSecret(rekeyed);
                identityProviderRepository.save(provider);
            }
            String rekeyedLdapBindPassword = rekeySecret("identity_provider", provider.getId(), "ldap_bind_password",
                    provider.getLdapBindPassword(), stats, dryRun);
            if (rekeyedLdapBindPassword != null) {
                provider.setLdapBindPassword(rekeyedLdapBindPassword);
                identityProviderRepository.save(provider);
            }
        }
    }

    private void rekeyWebhookSecrets(MutableRekeyStats stats, boolean dryRun) {
        for (WebhookEndpoint endpoint : webhookEndpointRepository.findAll()) {
            String rekeyed = rekeySecret("webhook_endpoint", endpoint.getId(), "secret",
                    endpoint.getSecret(), stats, dryRun);
            if (rekeyed != null) {
                endpoint.setSecret(rekeyed);
                webhookEndpointRepository.save(endpoint);
            }
        }
    }

    private void rekeyAuthFactorSecrets(MutableRekeyStats stats, boolean dryRun) {
        for (AuthFactor factor : authFactorRepository.findAll()) {
            String rekeyed = rekeySecret("auth_factor", factor.getId(), "secret",
                    factor.getSecret(), stats, dryRun);
            if (rekeyed != null) {
                factor.setSecret(rekeyed);
                authFactorRepository.save(factor);
            }
            String rekeyedWebauthnChallenge = rekeySecret("auth_factor", factor.getId(), "webauthn_challenge",
                    factor.getWebauthnChallenge(), stats, dryRun);
            if (rekeyedWebauthnChallenge != null) {
                factor.setWebauthnChallenge(rekeyedWebauthnChallenge);
                authFactorRepository.save(factor);
            }
        }
    }

    private void rekeyJwkPrivateKeys(MutableRekeyStats stats, boolean dryRun) {
        for (JwkKey key : jwkKeyRepository.findAll()) {
            String rekeyed = rekeySecret("jwk_key", key.getKid(), "private_key",
                    key.getPrivateKey(), stats, dryRun);
            if (rekeyed != null) {
                key.setPrivateKey(rekeyed);
                jwkKeyRepository.save(key);
            }
        }
    }

    private String rekeySecret(String resourceType,
                               String resourceId,
                               String field,
                               String value,
                               MutableRekeyStats stats,
                               boolean dryRun) {
        stats.scanned++;
        if (value == null || value.isBlank()) {
            stats.blank++;
            return null;
        }

        try {
            if (!secretCryptoService.isProtected(value)) {
                stats.plaintext++;
                return rekeyPlaintext(value, stats, dryRun);
            }

            String keyId = secretCryptoService.protectedKeyId(value);
            if (secretCryptoService.activeKeyId().equals(keyId)) {
                stats.alreadyActive++;
                return null;
            }

            stats.differentKey++;
            return rekeyPlaintext(secretCryptoService.reveal(value), stats, dryRun);
        } catch (RuntimeException e) {
            stats.failed++;
            log.warn("Unable to rekey {} {} field {}: {}",
                    resourceType, resourceId, field, e.getMessage());
            return null;
        }
    }

    private String rekeyPlaintext(String rawValue, MutableRekeyStats stats, boolean dryRun) {
        stats.wouldRekey++;
        if (dryRun) {
            return null;
        }
        stats.rekeyed++;
        return secretCryptoService.protect(rawValue);
    }

    private void recordAuditEvent(boolean dryRun, MutableRekeyStats total) {
        String eventType = dryRun ? "SECRET_REKEY_DRY_RUN" : "SECRET_REKEY_EXECUTED";
        String detail = "Secret rekey "
                + (dryRun ? "dry run" : "executed")
                + ": activeKeyId=" + secretCryptoService.activeKeyId()
                + ", wouldRekey=" + total.wouldRekey
                + ", rekeyed=" + total.rekeyed
                + ", failed=" + total.failed;
        auditEventService.record(eventType,
                AuditContext.getCurrentPrincipal(), "secret_protection", secretCryptoService.activeKeyId(),
                detail, AuditContext.getClientIp());
    }

    private static class MutableRekeyStats {
        private int scanned;
        private int blank;
        private int alreadyActive;
        private int plaintext;
        private int differentKey;
        private int wouldRekey;
        private int rekeyed;
        private int failed;

        static MutableRekeyStats totalOf(List<MutableRekeyStats> all) {
            MutableRekeyStats total = new MutableRekeyStats();
            for (MutableRekeyStats stats : all) {
                total.scanned += stats.scanned;
                total.blank += stats.blank;
                total.alreadyActive += stats.alreadyActive;
                total.plaintext += stats.plaintext;
                total.differentKey += stats.differentKey;
                total.wouldRekey += stats.wouldRekey;
                total.rekeyed += stats.rekeyed;
                total.failed += stats.failed;
            }
            return total;
        }

        static MutableRekeyStats totalOf(MutableRekeyStats... all) {
            return totalOf(List.of(all));
        }

        SecretProtectionDTO.RekeyStats toDto() {
            return new SecretProtectionDTO.RekeyStats(
                    scanned, blank, alreadyActive, plaintext, differentKey, wouldRekey, rekeyed, failed);
        }
    }
}
