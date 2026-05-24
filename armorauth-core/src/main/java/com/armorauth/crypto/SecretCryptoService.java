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

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Encrypts reversible secrets before persistence.
 */
public class SecretCryptoService {

    public static final String PREFIX = "{enc}";
    public static final String DEFAULT_KEY_ID = "v1";

    private static final String AES = "AES";
    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int NONCE_BYTES = 12;
    private static final Pattern PROTECTED_VALUE_PATTERN = Pattern.compile("^\\{enc}([^:]+):(.+)$");

    private final String activeKeyId;
    private final Map<String, SecretKeySpec> keySpecs;
    private final SecureRandom random = new SecureRandom();

    public SecretCryptoService(String secretKey) {
        this(DEFAULT_KEY_ID, Map.of(DEFAULT_KEY_ID, secretKey));
    }

    public SecretCryptoService(String activeKeyId, Map<String, String> secretKeys) {
        if (activeKeyId == null || activeKeyId.isBlank()) {
            throw new IllegalArgumentException("armorauth.crypto.active-key-id cannot be blank");
        }
        this.activeKeyId = normalizeKeyId(activeKeyId);
        this.keySpecs = buildKeySpecs(secretKeys);
        if (!this.keySpecs.containsKey(this.activeKeyId)) {
            throw new IllegalArgumentException(
                    "armorauth.crypto.active-key-id must exist in configured crypto keys: " + this.activeKeyId);
        }
    }

    public static SecretCryptoService fromProperties(String legacySecretKey,
                                                     String keyRing,
                                                     String activeKeyId) {
        Map<String, String> keys = parseKeyRing(legacySecretKey, keyRing);
        String resolvedActiveKeyId =
                activeKeyId == null || activeKeyId.isBlank() ? DEFAULT_KEY_ID : activeKeyId;
        return new SecretCryptoService(resolvedActiveKeyId, keys);
    }

    public String activeKeyId() {
        return activeKeyId;
    }

    public Set<String> keyIds() {
        return keySpecs.keySet();
    }

    public String protectedKeyId(String value) {
        if (value == null || value.isBlank() || !isProtected(value)) {
            return null;
        }
        Matcher matcher = PROTECTED_VALUE_PATTERN.matcher(value);
        if (!matcher.matches()) {
            throw new IllegalStateException("Unsupported protected secret format");
        }
        return matcher.group(1);
    }

    public boolean isProtectedWithActiveKey(String value) {
        String keyId = protectedKeyId(value);
        return keyId != null && activeKeyId.equals(keyId);
    }

    public String protect(String value) {
        if (value == null || value.isBlank() || isProtected(value)) {
            return value;
        }
        try {
            byte[] nonce = new byte[NONCE_BYTES];
            random.nextBytes(nonce);

            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpecs.get(activeKeyId), new GCMParameterSpec(GCM_TAG_BITS, nonce));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(nonce.length + encrypted.length);
            buffer.put(nonce);
            buffer.put(encrypted);
            return PREFIX + activeKeyId + ":" + Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to encrypt secret", e);
        }
    }

    public String reveal(String value) {
        if (value == null || value.isBlank() || !isProtected(value)) {
            return value;
        }
        String keyId = protectedKeyId(value);
        Matcher matcher = PROTECTED_VALUE_PATTERN.matcher(value);
        matcher.matches();
        SecretKeySpec keySpec = keySpecs.get(keyId);
        if (keySpec == null) {
            throw new IllegalStateException("No secret encryption key configured for key id: " + keyId);
        }
        try {
            byte[] payload = Base64.getDecoder().decode(matcher.group(2));
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte[] nonce = new byte[NONCE_BYTES];
            buffer.get(nonce);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);

            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, nonce));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to decrypt protected secret", e);
        }
    }

    public boolean isProtected(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    private static Map<String, String> parseKeyRing(String legacySecretKey, String keyRing) {
        Map<String, String> keys = new LinkedHashMap<>();
        if (legacySecretKey == null || legacySecretKey.isBlank()) {
            throw new IllegalArgumentException("armorauth.crypto.secret-key cannot be blank");
        }
        keys.put(DEFAULT_KEY_ID, legacySecretKey.trim());
        if (keyRing == null || keyRing.isBlank()) {
            return keys;
        }

        for (String entry : keyRing.split(",")) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            int separator = entry.indexOf('=');
            if (separator <= 0 || separator == entry.length() - 1) {
                throw new IllegalArgumentException("armorauth.crypto.keys entries must use keyId=secret format");
            }
            String keyId = normalizeKeyId(entry.substring(0, separator));
            String secret = entry.substring(separator + 1).trim();
            if (secret.isBlank()) {
                throw new IllegalArgumentException("armorauth.crypto.keys secret cannot be blank for key id: " + keyId);
            }
            keys.put(keyId, secret);
        }
        return keys;
    }

    private static Map<String, SecretKeySpec> buildKeySpecs(Map<String, String> secretKeys) {
        if (secretKeys == null || secretKeys.isEmpty()) {
            throw new IllegalArgumentException("armorauth.crypto.keys cannot be empty");
        }
        Map<String, SecretKeySpec> specs = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : secretKeys.entrySet()) {
            String keyId = normalizeKeyId(entry.getKey());
            String secretKey = entry.getValue();
            if (secretKey == null || secretKey.isBlank()) {
                throw new IllegalArgumentException("armorauth.crypto.keys secret cannot be blank for key id: " + keyId);
            }
            specs.put(keyId, new SecretKeySpec(sha256(secretKey), AES));
        }
        return Collections.unmodifiableMap(specs);
    }

    private static String normalizeKeyId(String keyId) {
        String normalized = keyId == null ? "" : keyId.trim();
        if (normalized.isBlank() || normalized.contains(":")) {
            throw new IllegalArgumentException("armorauth.crypto key id cannot be blank or contain ':'");
        }
        return normalized;
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to derive secret encryption key", e);
        }
    }
}
