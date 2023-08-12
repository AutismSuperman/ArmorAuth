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
package com.armorauth.demo;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithm;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import jakarta.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

/**
 * MacAlgorithm 对称加密
 */
@Slf4j
public class JwtMacAlgorithmEncodeAndDecodeTest {

    private final String clientId = "8a349006-b8e3-427b-8814-bc4b32e8930a";

    private final String clientSecret = "0c1501f4a8a35db0a725d1f547f5466f";


    @Test
    public void encodeAndDecode() {
        //************************encode************************/
        // Instant
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(Duration.ofSeconds(60));
        // JWK
        SecretKeySpec secretKey = new SecretKeySpec(
                clientSecret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256");
        JWK jwk = new OctetSequenceKey.Builder(secretKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        // resolveAlgorithm
        JwsAlgorithm jwsAlgorithm = resolveAlgorithm(jwk);
        // JwtClaimsSet
        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
                .issuer(clientId)
                .subject(clientId)
                .audience(Collections.singletonList("http://127.0.0.1:9000/auth/token"))
                .id(UUID.randomUUID().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt);
        JwtClaimsSet jwtClaimsSet = claimsBuilder.build();
        // JwsHeader
        JwsHeader.Builder headersBuilder = JwsHeader.with(jwsAlgorithm);
        JwsHeader jwsHeader = headersBuilder.build();
        // JwtEncoder
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(jwk));
        JwtEncoder jwsEncoder = new NimbusJwtEncoder(jwkSource);
        Jwt jws = jwsEncoder.encode(JwtEncoderParameters.from(jwsHeader, jwtClaimsSet));
        log.info("client secret jwt parameter:{} jwt: {}", OAuth2ParameterNames.CLIENT_ASSERTION, jws.getTokenValue());
        //************************encode************************/
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtClaimValidator<>(JwtClaimNames.ISS, clientId::equals),
                new JwtClaimValidator<>(JwtClaimNames.SUB, clientId::equals),
                new JwtClaimValidator<>(JwtClaimNames.EXP, Objects::nonNull),
                new JwtTimestampValidator()
        ));
        decoder.decode(jws.getTokenValue());
    }

    private static JwsAlgorithm resolveAlgorithm(JWK jwk) {
        JwsAlgorithm jwsAlgorithm = null;
        if (jwk.getAlgorithm() != null) {
            jwsAlgorithm = SignatureAlgorithm.from(jwk.getAlgorithm().getName());
            if (jwsAlgorithm == null) {
                jwsAlgorithm = MacAlgorithm.from(jwk.getAlgorithm().getName());
            }
        }
        if (jwsAlgorithm == null) {
            if (KeyType.RSA.equals(jwk.getKeyType())) {
                jwsAlgorithm = SignatureAlgorithm.RS256;
            } else if (KeyType.EC.equals(jwk.getKeyType())) {
                jwsAlgorithm = SignatureAlgorithm.ES256;
            } else if (KeyType.OCT.equals(jwk.getKeyType())) {
                jwsAlgorithm = MacAlgorithm.HS256;
            }
        }
        return jwsAlgorithm;
    }

}
