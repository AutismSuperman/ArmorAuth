package com.armorauth;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.endpoint.NimbusJwtClientAuthenticationParametersConverter;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithm;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import javax.crypto.spec.SecretKeySpec;
import javax.sound.midi.Soundbank;
import javax.swing.plaf.PanelUI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

@Slf4j
public class JwtEncodeAndDecodeTest {

    private final String clientId = "8a349006-b8e3-427b-8814-bc4b32e8930a";

    private final String clientSecret = "0c1501f4a8a35db0a725d1f547f5466f";

    @Test
    public void encode1() {
        String PATTERN_FORMAT = "yyyy-MM-dd HH:mm:ss";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(PATTERN_FORMAT) .withZone(ZoneId.systemDefault());
        Instant issuedAt = Instant.now();
        System.out.println(formatter.format(issuedAt));
    }

    @Test
    public void encode() {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(Duration.ofSeconds(60));
        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
                .issuer(clientId)
                .subject(clientId)
                .audience(Collections.singletonList("http://127.0.0.1:9000/auth/token"))
                .id(UUID.randomUUID().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt);
        SecretKeySpec secretKey = new SecretKeySpec(
                clientSecret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256");
        OctetSequenceKey octetSequenceKey = new OctetSequenceKey.Builder(secretKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        JwsAlgorithm jwsAlgorithm = resolveAlgorithm(octetSequenceKey);
        JwsHeader.Builder headersBuilder = JwsHeader.with(jwsAlgorithm);
        JwsHeader jwsHeader = headersBuilder.build();
        JwtClaimsSet jwtClaimsSet = claimsBuilder.build();
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(octetSequenceKey));
        JwtEncoder jwsEncoder = new NimbusJwtEncoder(jwkSource);
        Jwt jws = jwsEncoder.encode(JwtEncoderParameters.from(jwsHeader, jwtClaimsSet));
        log.info("parameter:{} jwt: {}", OAuth2ParameterNames.CLIENT_ASSERTION, jws.getTokenValue());
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

    @Test
    public void decode() {
        SecretKeySpec secretKeySpec = new SecretKeySpec(clientSecret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256");
        NimbusJwtDecoder build = NimbusJwtDecoder.withSecretKey(secretKeySpec).macAlgorithm(MacAlgorithm.HS256).build();
        build.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtClaimValidator<>(JwtClaimNames.ISS, clientId::equals),
                new JwtClaimValidator<>(JwtClaimNames.SUB, clientId::equals),
                new JwtClaimValidator<>(JwtClaimNames.EXP, Objects::nonNull),
                new JwtTimestampValidator()
        ));
        build.decode("your client_assertion jwt value");
    }


}
