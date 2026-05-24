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
package com.armorauth.jose;

import com.armorauth.crypto.SecretCryptoService;
import com.armorauth.data.entity.JwkKey;
import com.armorauth.data.repository.JwkKeyRepository;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PersistentJwkSourceTest {

    private final SecretCryptoService secretCryptoService =
            new SecretCryptoService("persistent-jwk-source-test-key");

    @Test
    void generatedRsaPrivateKeyIsProtectedAtRestAndStillUsable() {
        JwkKeyRepository repository = mock(JwkKeyRepository.class);
        when(repository.findByStatus(JwkKey.JwkKeyStatus.active)).thenReturn(List.of());
        when(repository.save(any(JwkKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PersistentJwkSource source = new PersistentJwkSource(repository, secretCryptoService);

        ArgumentCaptor<JwkKey> keyCaptor = ArgumentCaptor.forClass(JwkKey.class);
        verify(repository).save(keyCaptor.capture());
        JwkKey storedKey = keyCaptor.getValue();
        String revealedPrivateKey = secretCryptoService.reveal(storedKey.getPrivateKey());

        assertThat(storedKey.getPrivateKey()).startsWith(SecretCryptoService.PREFIX);
        assertThatCode(() -> Base64.getDecoder().decode(revealedPrivateKey)).doesNotThrowAnyException();
        assertThat(source.get(allKeys(), null)).hasSize(1);
        assertThat(source.get(allKeys(), null).get(0).isPrivate()).isTrue();
    }

    @Test
    void legacyPlaintextPrivateKeyStillLoads() throws Exception {
        RSAKey rsaKey = Jwks.generateRsa();
        JwkKey legacyKey = toEntity(rsaKey, Base64.getEncoder().encodeToString(rsaKey.toPrivateKey().getEncoded()));
        JwkKeyRepository repository = mock(JwkKeyRepository.class);
        when(repository.findByStatus(JwkKey.JwkKeyStatus.active)).thenReturn(List.of(legacyKey));

        PersistentJwkSource source = new PersistentJwkSource(repository, secretCryptoService);

        assertThat(legacyKey.getPrivateKey()).doesNotStartWith(SecretCryptoService.PREFIX);
        assertThat(source.get(allKeys(), null)).hasSize(1);
        assertThat(source.get(allKeys(), null).get(0).isPrivate()).isTrue();
    }

    private JWKSelector allKeys() {
        return new JWKSelector(new JWKMatcher.Builder().build());
    }

    private JwkKey toEntity(RSAKey rsaKey, String privateKey) throws Exception {
        JwkKey jwkKey = new JwkKey();
        jwkKey.setId(UUID.randomUUID().toString());
        jwkKey.setKid(rsaKey.getKeyID());
        jwkKey.setKeyType("RSA");
        jwkKey.setAlgorithm("RS256");
        jwkKey.setPublicKey(Base64.getEncoder().encodeToString(rsaKey.toPublicKey().getEncoded()));
        jwkKey.setPrivateKey(privateKey);
        jwkKey.setStatus(JwkKey.JwkKeyStatus.active);
        jwkKey.setCreatedAt(Instant.now());
        return jwkKey;
    }
}
