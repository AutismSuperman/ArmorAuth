package com.armorauth;

import com.armorauth.jose.Jwks;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.RSAKey;
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

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

@Slf4j
public class JwtSignatureAlgorithmEncodeAndDecodeTest {


    private final String clientId = "8a349006-b8e3-427b-8814-bc4b32e8930a";

    @Test
    public void encodeAndDecode() throws JOSEException {
        //************************encode************************/
        // Instant
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(Duration.ofSeconds(60));
        // JWK
        RSAKey rsaKey = Jwks.generateRsa();
        KeyPair keyPair = rsaKey.toKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();
        log.info("keyPair.getPrivate: {}", privateKey);
        log.info("keyPair.getPublic: {}", publicKey);
        // resolveAlgorithm
        JwsAlgorithm jwsAlgorithm = resolveAlgorithm(rsaKey);
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
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(rsaKey));
        JwtEncoder jwsEncoder = new NimbusJwtEncoder(jwkSource);
        Jwt jws = jwsEncoder.encode(JwtEncoderParameters.from(jwsHeader, jwtClaimsSet));
        log.info("private key jwt parameter:{} jwt: {}", OAuth2ParameterNames.CLIENT_ASSERTION, jws.getTokenValue());
        //************************decode************************/
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withPublicKey((RSAPublicKey) publicKey)
                .signatureAlgorithm(SignatureAlgorithm.RS256).build();
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
