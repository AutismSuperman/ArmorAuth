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

import com.armorauth.crypto.SecretCryptoService;
import com.armorauth.data.entity.AuthFactor;
import com.armorauth.data.entity.IdentityProvider;
import com.armorauth.data.entity.JwkKey;
import com.armorauth.data.entity.WebhookEndpoint;
import com.armorauth.data.repository.AuthFactorRepository;
import com.armorauth.data.repository.IdentityProviderRepository;
import com.armorauth.data.repository.JwkKeyRepository;
import com.armorauth.data.repository.WebhookEndpointRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Protects legacy plaintext secrets after the application starts.
 */
@Component
public class SecretProtectionBackfillService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SecretProtectionBackfillService.class);

    private final SecretCryptoService secretCryptoService;
    private final IdentityProviderRepository identityProviderRepository;
    private final WebhookEndpointRepository webhookEndpointRepository;
    private final AuthFactorRepository authFactorRepository;
    private final JwkKeyRepository jwkKeyRepository;

    public SecretProtectionBackfillService(SecretCryptoService secretCryptoService,
                                           IdentityProviderRepository identityProviderRepository,
                                           WebhookEndpointRepository webhookEndpointRepository,
                                           AuthFactorRepository authFactorRepository,
                                           JwkKeyRepository jwkKeyRepository) {
        this.secretCryptoService = secretCryptoService;
        this.identityProviderRepository = identityProviderRepository;
        this.webhookEndpointRepository = webhookEndpointRepository;
        this.authFactorRepository = authFactorRepository;
        this.jwkKeyRepository = jwkKeyRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int identityProviders = protectIdentityProviderSecrets();
        int webhookEndpoints = protectWebhookSecrets();
        int authFactors = protectAuthFactorSecrets();
        int jwkKeys = protectJwkPrivateKeys();

        int total = identityProviders + webhookEndpoints + authFactors + jwkKeys;
        if (total > 0) {
            log.info(
                    "Protected {} legacy plaintext secret(s): identityProviders={}, webhookEndpoints={}, authFactors={}, jwkKeys={}",
                    total, identityProviders, webhookEndpoints, authFactors, jwkKeys);
        }
    }

    private int protectIdentityProviderSecrets() {
        int count = 0;
        for (IdentityProvider provider : identityProviderRepository.findAll()) {
            String protectedSecret = protectIfNeeded(provider.getClientSecret());
            if (protectedSecret != null) {
                provider.setClientSecret(protectedSecret);
                count++;
            }
            String protectedLdapBindPassword = protectIfNeeded(provider.getLdapBindPassword());
            if (protectedLdapBindPassword != null) {
                provider.setLdapBindPassword(protectedLdapBindPassword);
                count++;
            }
        }
        return count;
    }

    private int protectWebhookSecrets() {
        int count = 0;
        for (WebhookEndpoint endpoint : webhookEndpointRepository.findAll()) {
            String protectedSecret = protectIfNeeded(endpoint.getSecret());
            if (protectedSecret != null) {
                endpoint.setSecret(protectedSecret);
                count++;
            }
        }
        return count;
    }

    private int protectAuthFactorSecrets() {
        int count = 0;
        for (AuthFactor factor : authFactorRepository.findAll()) {
            String protectedSecret = protectIfNeeded(factor.getSecret());
            if (protectedSecret != null) {
                factor.setSecret(protectedSecret);
                count++;
            }
            String protectedWebauthnChallenge = protectIfNeeded(factor.getWebauthnChallenge());
            if (protectedWebauthnChallenge != null) {
                factor.setWebauthnChallenge(protectedWebauthnChallenge);
                count++;
            }
        }
        return count;
    }

    private int protectJwkPrivateKeys() {
        int count = 0;
        for (JwkKey key : jwkKeyRepository.findAll()) {
            String protectedPrivateKey = protectIfNeeded(key.getPrivateKey());
            if (protectedPrivateKey != null) {
                key.setPrivateKey(protectedPrivateKey);
                count++;
            }
        }
        return count;
    }

    private String protectIfNeeded(String secret) {
        if (secret == null || secret.isBlank() || secret.startsWith(SecretCryptoService.PREFIX)) {
            return null;
        }
        return secretCryptoService.protect(secret);
    }
}
