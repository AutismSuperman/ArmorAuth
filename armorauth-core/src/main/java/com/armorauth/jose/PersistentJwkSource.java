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

import com.armorauth.authorization.tenant.TenantIssuerContext;
import com.armorauth.crypto.SecretCryptoService;
import com.armorauth.data.entity.JwkKey;
import com.armorauth.data.repository.JwkKeyRepository;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.Set;
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
    private static final String DEFAULT_ALGORITHM = "RS256";
    private static final Set<String> RSA_ALGORITHMS = Set.of(
            "RS256", "RS384", "RS512", "PS256", "PS384", "PS512");
    private static final Map<String, String> EC_ALGORITHM_CURVES = Map.of(
            "ES256", "secp256r1",
            "ES384", "secp384r1",
            "ES512", "secp521r1");

    private final JwkKeyRepository jwkKeyRepository;
    private final SecretCryptoService secretCryptoService;
    private final ConcurrentMap<String, JWKSet> jwkSets = new ConcurrentHashMap<>();

    public PersistentJwkSource(JwkKeyRepository jwkKeyRepository) {
        this(jwkKeyRepository, null);
    }

    public PersistentJwkSource(JwkKeyRepository jwkKeyRepository,
                               SecretCryptoService secretCryptoService) {
        this.jwkKeyRepository = jwkKeyRepository;
        this.secretCryptoService = secretCryptoService;
        this.jwkSets.put(TenantIssuerContext.DEFAULT_TENANT_ID,
                loadOrCreateJwkSet(TenantIssuerContext.DEFAULT_TENANT_ID));
    }

    private JWKSet loadOrCreateJwkSet(String tenantId) {
        List<JwkKey> activeKeys = jwkKeyRepository.findByTenantIdAndStatus(tenantId, JwkKey.JwkKeyStatus.active);
        if (!activeKeys.isEmpty()) {
            log.info("Loaded {} active JWK key(s) from database for tenant {}", activeKeys.size(), tenantId);
            List<JWK> jwkList = activeKeys.stream()
                    .map(this::toJwk)
                    .toList();
            return new JWKSet(jwkList);
        }

        log.info("No active JWK keys found in database for tenant {}, generating new {} key pair",
                tenantId, DEFAULT_ALGORITHM);
        JwkKey jwkKey = generateAndPersistKey(DEFAULT_ALGORITHM, tenantId);
        return new JWKSet(toJwk(jwkKey));
    }

    /**
     * 轮换密钥：生成新的 active 密钥，将当前 active 密钥降级为 standby
     *
     * @return 新生成的 active 密钥的 kid
     */
    public String rotateKey() {
        return rotateKey(DEFAULT_ALGORITHM);
    }

    /**
     * 按指定 JWS 算法轮换密钥。
     *
     * @param algorithm JWS 算法，如 RS256、PS256、ES256
     * @return 新生成的 active 密钥的 kid
     */
    public String rotateKey(String algorithm) {
        String tenantId = TenantIssuerContext.tenantIdOrDefault();
        String normalizedAlgorithm = normalizeAlgorithm(algorithm);
        log.info("Starting JWK key rotation, tenant={}, algorithm={}", tenantId, normalizedAlgorithm);

        // 将当前 active 密钥降级为 standby
        List<JwkKey> activeKeys = jwkKeyRepository.findByTenantIdAndStatus(tenantId, JwkKey.JwkKeyStatus.active);
        for (JwkKey key : activeKeys) {
            key.setStatus(JwkKey.JwkKeyStatus.standby);
            jwkKeyRepository.save(key);
            log.info("Demoted key to standby: kid={}", key.getKid());
        }

        // 生成新的 active 密钥
        JwkKey newKey = generateAndPersistKey(normalizedAlgorithm, tenantId);
        refresh(tenantId);

        log.info("JWK key rotation completed, tenant={}, new active key: kid={}, algorithm={}",
                tenantId, newKey.getKid(), normalizedAlgorithm);
        return newKey.getKid();
    }

    /**
     * 将 standby 密钥标记为 retired（废弃）
     *
     * @param kid 密钥 ID
     */
    public void retireKey(String kid) {
        String tenantId = TenantIssuerContext.tenantIdOrDefault();
        jwkKeyRepository.findByTenantIdAndKid(tenantId, kid).ifPresent(key -> {
            if (key.getStatus() == JwkKey.JwkKeyStatus.standby) {
                key.setStatus(JwkKey.JwkKeyStatus.retired);
                jwkKeyRepository.save(key);
                refresh(tenantId);
                log.info("Key retired: kid={}", kid);
            }
        });
    }

    /**
     * 删除非 active 密钥。
     *
     * @param kid 密钥 ID
     */
    public void deleteKey(String kid) {
        String tenantId = TenantIssuerContext.tenantIdOrDefault();
        JwkKey key = jwkKeyRepository.findByTenantIdAndKid(tenantId, kid)
                .orElseThrow(() -> new IllegalArgumentException("JWK 密钥不存在: " + kid));
        if (key.getStatus() == JwkKey.JwkKeyStatus.active) {
            throw new IllegalArgumentException("Active 密钥不能删除，请先轮换后再删除");
        }
        jwkKeyRepository.delete(key);
        refresh(tenantId);
        log.info("Key deleted: kid={}", kid);
    }

    /**
     * 获取所有密钥（用于管理台展示）
     */
    public List<JwkKey> listAllKeys() {
        return jwkKeyRepository.findAll();
    }

    private JwkKey generateAndPersistKey(String algorithm, String tenantId) {
        String normalizedAlgorithm = normalizeAlgorithm(algorithm);
        try {
            if (RSA_ALGORITHMS.contains(normalizedAlgorithm)) {
                RSAKey rsaKey = Jwks.generateRsa();
                return persistKey(
                        tenantId,
                        rsaKey.getKeyID(),
                        "RSA",
                        normalizedAlgorithm,
                        Base64.getEncoder().encodeToString(rsaKey.toPublicKey().getEncoded()),
                        Base64.getEncoder().encodeToString(rsaKey.toPrivateKey().getEncoded()));
            }

            ECKey ecKey = Jwks.generateEc(EC_ALGORITHM_CURVES.get(normalizedAlgorithm));
            return persistKey(
                    tenantId,
                    ecKey.getKeyID(),
                    "EC",
                    normalizedAlgorithm,
                    Base64.getEncoder().encodeToString(ecKey.toPublicKey().getEncoded()),
                    Base64.getEncoder().encodeToString(ecKey.toPrivateKey().getEncoded()));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate JWK key pair: algorithm=" + normalizedAlgorithm, e);
        }
    }

    private JwkKey persistKey(String tenantId, String kid, String keyType, String algorithm,
                              String publicKey, String privateKey) {
        JwkKey jwkKey = new JwkKey();
        jwkKey.setId(UUID.randomUUID().toString());
        jwkKey.setTenantId(tenantId);
        jwkKey.setKid(kid);
        jwkKey.setKeyType(keyType);
        jwkKey.setAlgorithm(algorithm);
        jwkKey.setPublicKey(publicKey);
        jwkKey.setPrivateKey(protectPrivateKey(privateKey));
        jwkKey.setStatus(JwkKey.JwkKeyStatus.active);
        jwkKey.setCreatedAt(Instant.now());
        jwkKey = jwkKeyRepository.save(jwkKey);
        log.info("Generated and persisted new {} JWK key: kid={}, algorithm={}",
                keyType, jwkKey.getKid(), algorithm);
        return jwkKey;
    }

    private JWK toJwk(JwkKey jwkKey) {
        if ("EC".equalsIgnoreCase(jwkKey.getKeyType())) {
            return toEcJwk(jwkKey);
        }
        return toRsaJwk(jwkKey);
    }

    private RSAKey toRsaJwk(JwkKey jwkKey) {
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
                    .algorithm(JWSAlgorithm.parse(resolveStoredAlgorithm(jwkKey)))
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to reconstruct RSA JWK from database: kid=" + jwkKey.getKid(), e);
        }
    }

    private ECKey toEcJwk(JwkKey jwkKey) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(Base64.getDecoder().decode(jwkKey.getPublicKey()));
            ECPublicKey publicKey = (ECPublicKey) keyFactory.generatePublic(pubSpec);
            PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(
                    Base64.getDecoder().decode(revealPrivateKey(jwkKey.getPrivateKey())));
            ECPrivateKey privateKey = (ECPrivateKey) keyFactory.generatePrivate(privSpec);
            return new ECKey.Builder(Curve.forECParameterSpec(publicKey.getParams()), publicKey)
                    .privateKey(privateKey)
                    .keyID(jwkKey.getKid())
                    .algorithm(JWSAlgorithm.parse(resolveStoredAlgorithm(jwkKey)))
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to reconstruct EC JWK from database: kid=" + jwkKey.getKid(), e);
        }
    }

    private String normalizeAlgorithm(String algorithm) {
        String normalized = (algorithm == null || algorithm.isBlank())
                ? DEFAULT_ALGORITHM
                : algorithm.trim().toUpperCase(Locale.ROOT);
        if (!RSA_ALGORITHMS.contains(normalized) && !EC_ALGORITHM_CURVES.containsKey(normalized)) {
            throw new IllegalArgumentException("不支持的 JWK 签名算法: " + algorithm);
        }
        return normalized;
    }

    private String resolveStoredAlgorithm(JwkKey jwkKey) {
        return jwkKey.getAlgorithm() == null || jwkKey.getAlgorithm().isBlank()
                ? DEFAULT_ALGORITHM
                : jwkKey.getAlgorithm().trim().toUpperCase(Locale.ROOT);
    }

    private String protectPrivateKey(String privateKey) {
        return secretCryptoService != null ? secretCryptoService.protect(privateKey) : privateKey;
    }

    private String revealPrivateKey(String privateKey) {
        return secretCryptoService != null ? secretCryptoService.reveal(privateKey) : privateKey;
    }

    @Override
    public List<JWK> get(JWKSelector jwkSelector, SecurityContext context) {
        String tenantId = TenantIssuerContext.tenantIdOrDefault();
        JWKSet jwkSet = jwkSets.computeIfAbsent(tenantId, this::loadOrCreateJwkSet);
        return jwkSelector.select(jwkSet);
    }

    /**
     * 重新加载密钥集
     */
    public void refresh() {
        refresh(TenantIssuerContext.tenantIdOrDefault());
    }

    private void refresh(String tenantId) {
        this.jwkSets.put(tenantId, loadOrCreateJwkSet(tenantId));
    }
}
