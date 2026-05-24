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
package com.armorauth.crypto;

import com.armorauth.mfa.TotpService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecretCryptoServiceTest {

    private final SecretCryptoService cryptoService =
            new SecretCryptoService("test encryption key with enough entropy");

    @Test
    void protectEncryptsAndRevealDecrypts() {
        String encrypted = cryptoService.protect("client-secret");

        assertThat(encrypted).startsWith(SecretCryptoService.PREFIX);
        assertThat(encrypted).startsWith("{enc}v1:");
        assertThat(encrypted).doesNotContain("client-secret");
        assertThat(cryptoService.reveal(encrypted)).isEqualTo("client-secret");
        assertThat(cryptoService.protectedKeyId(encrypted)).isEqualTo("v1");
        assertThat(cryptoService.isProtectedWithActiveKey(encrypted)).isTrue();
    }

    @Test
    void protectIsIdempotentAndRevealKeepsLegacyPlaintext() {
        String encrypted = cryptoService.protect("webhook-secret");

        assertThat(cryptoService.protect(encrypted)).isEqualTo(encrypted);
        assertThat(cryptoService.reveal("legacy-secret")).isEqualTo("legacy-secret");
    }

    @Test
    void totpServiceAcceptsProtectedSecret() {
        TotpService totpService = new TotpService(cryptoService);
        String secret = totpService.generateSecret();
        String code = totpService.generateCode(secret);

        assertThat(totpService.verifyCode(cryptoService.protect(secret), code)).isTrue();
    }

    @Test
    void keyRingCanRevealOldVersionAndProtectWithActiveVersion() {
        SecretCryptoService v1Crypto = new SecretCryptoService("legacy encryption key");
        String v1Encrypted = v1Crypto.protect("rotated-secret");

        SecretCryptoService rotatedCrypto = new SecretCryptoService("v2", Map.of(
                "v1", "legacy encryption key",
                "v2", "new encryption key"
        ));
        String v2Encrypted = rotatedCrypto.protect("new-secret");

        assertThat(v1Encrypted).startsWith("{enc}v1:");
        assertThat(v2Encrypted).startsWith("{enc}v2:");
        assertThat(rotatedCrypto.reveal(v1Encrypted)).isEqualTo("rotated-secret");
        assertThat(rotatedCrypto.reveal(v2Encrypted)).isEqualTo("new-secret");
        assertThat(rotatedCrypto.protectedKeyId(v1Encrypted)).isEqualTo("v1");
        assertThat(rotatedCrypto.protectedKeyId(v2Encrypted)).isEqualTo("v2");
        assertThat(rotatedCrypto.isProtectedWithActiveKey(v1Encrypted)).isFalse();
        assertThat(rotatedCrypto.isProtectedWithActiveKey(v2Encrypted)).isTrue();
        assertThat(rotatedCrypto.activeKeyId()).isEqualTo("v2");
        assertThat(rotatedCrypto.keyIds()).containsExactlyInAnyOrder("v1", "v2");
    }

    @Test
    void fromPropertiesAddsLegacySecretKeyAsV1Fallback() {
        SecretCryptoService legacyCrypto = new SecretCryptoService("legacy fallback key");
        String v1Encrypted = legacyCrypto.protect("legacy-value");

        SecretCryptoService rotatedCrypto = SecretCryptoService.fromProperties(
                "legacy fallback key", "v2=active key", "v2");

        assertThat(rotatedCrypto.reveal(v1Encrypted)).isEqualTo("legacy-value");
        assertThat(rotatedCrypto.protect("active-value")).startsWith("{enc}v2:");
    }

    @Test
    void revealFailsWhenProtectedValueUsesUnknownKeyId() {
        String encrypted = cryptoService.protect("client-secret");
        String rekeyed = encrypted.replace("{enc}v1:", "{enc}missing:");

        assertThatThrownBy(() -> cryptoService.reveal(rekeyed))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No secret encryption key configured");
    }
}
