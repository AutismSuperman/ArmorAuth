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
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * 持久化 JWK 密钥源，启动时从数据库加载密钥，若不存在则自动生成并持久化。
 * <p>
 * 支持密钥轮换：生成新密钥时，旧密钥状态从 active 变为 standby，
 * 经过指定时间后可将 standby 密钥标记为 retired。
 *
 * @author fulin
 * @since 2026-05-22
 */
public class PersistentJwkSource implements JWKSource<SecurityContext> {

    private static final Logger log = LoggerFactory.getLogger(PersistentJwkSource.class);

    private final JwkKeyRepository jwkKeyRepository;
    private final SecretCryptoService secretCryptoService;
    private volatile JWKSet jwkSet;

    public PersistentJwkSource(JwkKeyRepository jwkKeyRepository) {
        this(jwkKeyRepository, null);
    }

    public PersistentJwkSource(JwkKeyRepository jwkKeyRepository,
                               SecretCryptoService secretCryptoService) {
        this.jwkKeyRepository = jwkKeyRepository;
        this.secretCryptoService = secretCryptoService;
        this.jwkSet = loadOrCreateJwkSet();
    }

    private JWKSet loadOrCreateJwkSet() {
        List<JwkKey> activeKeys = jwkKeyRepository.findByStatus(JwkKey.JwkKeyStatus.active);
        if (!activeKeys.isEmpty()) {
            log.info("Loaded {} active JWK key(s) from database", activeKeys.size());
            List<JWK> jwkList = activeKeys.stream()
                    .map(this::toJwk)
                    .toList();
            return new JWKSet(jwkList);
        }

        log.info("No JWK keys found in database, generating new RSA key pair");
        JwkKey jwkKey = generateAndPersistRsaKey();
        return new JWKSet(toJwk(jwkKey));
    }

    /**
     * 轮换密钥：生成新的 active 密钥，将当前 active 密钥降级为 standby
     *
     * @return 新生成的 active 密钥的 kid
     */
    public String rotateKey() {
        log.info("Starting JWK key rotation");

        // 将当前 active 密钥降级为 standby
        List<JwkKey> activeKeys = jwkKeyRepository.findByStatus(JwkKey.JwkKeyStatus.active);
        for (JwkKey key : activeKeys) {
            key.setStatus(JwkKey.JwkKeyStatus.standby);
            jwkKeyRepository.save(key);
            log.info("Demoted key to standby: kid={}", key.getKid());
        }

        // 生成新的 active 密钥
        JwkKey newKey = generateAndPersistRsaKey();
        refresh();

        log.info("JWK key rotation completed, new active key: kid={}", newKey.getKid());
        return newKey.getKid();
    }

    /**
     * 将 standby 密钥标记为 retired（废弃）
     *
     * @param kid 密钥 ID
     */
    public void retireKey(String kid) {
        jwkKeyRepository.findByKid(kid).ifPresent(key -> {
            if (key.getStatus() == JwkKey.JwkKeyStatus.standby) {
                key.setStatus(JwkKey.JwkKeyStatus.retired);
                jwkKeyRepository.save(key);
                log.info("Key retired: kid={}", kid);
            }
        });
    }

    /**
     * 获取所有密钥（用于管理台展示）
     */
    public List<JwkKey> listAllKeys() {
        return jwkKeyRepository.findAll();
    }

    private JwkKey generateAndPersistRsaKey() {
        try {
            RSAKey rsaKey = Jwks.generateRsa();
            JwkKey jwkKey = new JwkKey();
            jwkKey.setId(UUID.randomUUID().toString());
            jwkKey.setKid(rsaKey.getKeyID());
            jwkKey.setKeyType("RSA");
            jwkKey.setAlgorithm("RS256");
            jwkKey.setPublicKey(Base64.getEncoder().encodeToString(rsaKey.toPublicKey().getEncoded()));
            jwkKey.setPrivateKey(protectPrivateKey(
                    Base64.getEncoder().encodeToString(rsaKey.toPrivateKey().getEncoded())));
            jwkKey.setStatus(JwkKey.JwkKeyStatus.active);
            jwkKey.setCreatedAt(Instant.now());
            jwkKey = jwkKeyRepository.save(jwkKey);
            log.info("Generated and persisted new RSA JWK key: kid={}", jwkKey.getKid());
            return jwkKey;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate RSA key pair", e);
        }
    }

    private JWK toJwk(JwkKey jwkKey) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(Base64.getDecoder().decode(jwkKey.getPublicKey()));
            RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(pubSpec);
            PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(
                    Base64.getDecoder().decode(revealPrivateKey(jwkKey.getPrivateKey())));
            RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(privSpec);
            return new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(jwkKey.getKid())
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to reconstruct JWK from database: kid=" + jwkKey.getKid(), e);
        }
    }

    private String protectPrivateKey(String privateKey) {
        return secretCryptoService != null ? secretCryptoService.protect(privateKey) : privateKey;
    }

    private String revealPrivateKey(String privateKey) {
        return secretCryptoService != null ? secretCryptoService.reveal(privateKey) : privateKey;
    }

    @Override
    public List<JWK> get(JWKSelector jwkSelector, SecurityContext context) {
        return jwkSelector.select(jwkSet);
    }

    /**
     * 重新加载密钥集
     */
    public void refresh() {
        this.jwkSet = loadOrCreateJwkSet();
    }
}
