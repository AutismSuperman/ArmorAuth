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

import com.armorauth.admin.dto.AccountDTO;
import com.armorauth.common.exception.ValidationException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasskeyRegistrationVerifier {

    private static final int FLAG_USER_PRESENT = 0x01;
    private static final int FLAG_BACKUP_ELIGIBLE = 0x08;
    private static final int FLAG_BACKUP_STATE = 0x10;
    private static final int FLAG_ATTESTED_CREDENTIAL_DATA = 0x40;

    private final ObjectMapper objectMapper;
    private final ObjectMapper cborObjectMapper = new ObjectMapper(new CBORFactory());
    private final CBORFactory cborFactory = new CBORFactory();

    public PasskeyRegistrationVerifier(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public boolean hasAttestation(AccountDTO.PasskeyFinishRegistrationRequest request) {
        return request != null
                && hasText(request.clientDataJSON())
                && hasText(request.attestationObject());
    }

    public VerifiedRegistration verify(AccountDTO.PasskeyFinishRegistrationRequest request,
                                       String expectedChallenge,
                                       String expectedRpId) {
        if (!hasAttestation(request)) {
            throw new ValidationException("Passkey attestationObject 和 clientDataJSON 不能为空");
        }
        byte[] clientDataJson = decodeBase64Url(request.clientDataJSON(), "clientDataJSON");
        byte[] attestationObject = decodeBase64Url(request.attestationObject(), "attestationObject");
        validateClientData(clientDataJson, expectedChallenge, effectiveRpId(request, expectedRpId));

        AttestationObject attestation = parseAttestationObject(attestationObject);
        ParsedAuthenticatorData authData = parseAuthenticatorData(attestation.authData(), effectiveRpId(request, expectedRpId));
        parseCosePublicKey(authData.credentialPublicKey())
                .orElseThrow(() -> new ValidationException("Passkey credentialPublicKey 格式不支持"));
        verifyAttestationStatement(attestation, authData, clientDataJson);

        String credentialId = base64Url(authData.credentialId());
        if (hasText(request.credentialId())
                && !normalizeBase64Url(request.credentialId()).equals(normalizeBase64Url(credentialId))) {
            throw new ValidationException("Passkey credentialId 与 attestationObject 不匹配");
        }

        return new VerifiedRegistration(
                credentialId,
                base64Url(authData.credentialPublicKey()),
                authData.signCount(),
                authData.aaguid(),
                authData.backupEligible(),
                authData.backupState(),
                attestation.format());
    }

    private void validateClientData(byte[] clientDataJson, String expectedChallenge, String expectedRpId) {
        try {
            JsonNode clientData = objectMapper.readTree(clientDataJson);
            if (!"webauthn.create".equals(clientData.path("type").asText())) {
                throw new ValidationException("Passkey clientData.type 必须为 webauthn.create");
            }
            String actualChallenge = clientData.path("challenge").asText();
            if (!normalizeBase64Url(expectedChallenge).equals(normalizeBase64Url(actualChallenge))) {
                throw new ValidationException("Passkey challenge 不匹配");
            }
            if (clientData.path("crossOrigin").asBoolean(false)) {
                throw new ValidationException("Passkey crossOrigin 请求不允许");
            }
            String actualOrigin = clientData.path("origin").asText();
            if (!originMatchesRpId(actualOrigin, expectedRpId)) {
                throw new ValidationException("Passkey origin 与 RP ID 不匹配: " + actualOrigin);
            }
        } catch (ValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ValidationException("Passkey clientDataJSON 无法解析");
        }
    }

    @SuppressWarnings("unchecked")
    private AttestationObject parseAttestationObject(byte[] value) {
        try {
            Map<String, Object> attestation = cborObjectMapper.readValue(value, LinkedHashMap.class);
            String format = stringValue(attestation.get("fmt"));
            byte[] authData = bytesValue(attestation.get("authData"));
            Object attStmtObj = attestation.get("attStmt");
            Map<Object, Object> attStmt = attStmtObj instanceof Map<?, ?> map
                    ? (Map<Object, Object>) map
                    : Map.of();
            if (!hasText(format)) {
                throw new ValidationException("Passkey attestation fmt 缺失");
            }
            if (authData.length == 0) {
                throw new ValidationException("Passkey attestation authData 缺失");
            }
            return new AttestationObject(format, authData, attStmt);
        } catch (ValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ValidationException("Passkey attestationObject 无法解析");
        }
    }

    private ParsedAuthenticatorData parseAuthenticatorData(byte[] authData, String expectedRpId) {
        if (authData.length < 37 + 16 + 2) {
            throw new ValidationException("Passkey authenticatorData 长度不正确");
        }
        byte[] expectedRpIdHash = sha256(expectedRpId.getBytes(StandardCharsets.UTF_8));
        for (int i = 0; i < expectedRpIdHash.length; i++) {
            if (authData[i] != expectedRpIdHash[i]) {
                throw new ValidationException("Passkey rpIdHash 不匹配");
            }
        }

        int flags = authData[32] & 0xff;
        if ((flags & FLAG_USER_PRESENT) == 0) {
            throw new ValidationException("Passkey 注册缺少 user present 标记");
        }
        if ((flags & FLAG_ATTESTED_CREDENTIAL_DATA) == 0) {
            throw new ValidationException("Passkey 注册缺少 attested credential data");
        }

        long signCount = ((authData[33] & 0xffL) << 24)
                | ((authData[34] & 0xffL) << 16)
                | ((authData[35] & 0xffL) << 8)
                | (authData[36] & 0xffL);
        byte[] aaguidBytes = Arrays.copyOfRange(authData, 37, 53);
        int credentialIdLength = ((authData[53] & 0xff) << 8) | (authData[54] & 0xff);
        int credentialIdStart = 55;
        int credentialIdEnd = credentialIdStart + credentialIdLength;
        if (credentialIdLength <= 0 || credentialIdEnd > authData.length) {
            throw new ValidationException("Passkey credentialId 长度不正确");
        }
        byte[] credentialId = Arrays.copyOfRange(authData, credentialIdStart, credentialIdEnd);
        byte[] coseAndExtensions = Arrays.copyOfRange(authData, credentialIdEnd, authData.length);
        byte[] coseKey = firstCborObjectBytes(coseAndExtensions);
        if (coseKey.length == 0) {
            throw new ValidationException("Passkey credentialPublicKey 缺失");
        }

        return new ParsedAuthenticatorData(
                authData,
                credentialId,
                coseKey,
                signCount,
                uuid(aaguidBytes),
                (flags & FLAG_BACKUP_ELIGIBLE) != 0,
                (flags & FLAG_BACKUP_STATE) != 0);
    }

    private void verifyAttestationStatement(AttestationObject attestation,
                                            ParsedAuthenticatorData authData,
                                            byte[] clientDataJson) {
        if ("none".equals(attestation.format())) {
            return;
        }
        if (!"packed".equals(attestation.format())) {
            throw new ValidationException("Passkey attestation 格式暂不支持: " + attestation.format());
        }

        byte[] signature = bytesValue(attestation.attStmt().get("sig"));
        if (signature.length == 0) {
            throw new ValidationException("Passkey packed attestation 缺少签名");
        }
        int alg = intValue(attestation.attStmt().get("alg"));
        byte[] signedData = concat(authData.raw(), sha256(clientDataJson));
        PublicKey verificationKey = packedAttestationPublicKey(attestation, authData)
                .orElseThrow(() -> new ValidationException("Passkey packed attestation 缺少验证公钥"));
        verifySignature(verificationKey, alg, signedData, signature);
    }

    private Optional<PublicKey> packedAttestationPublicKey(AttestationObject attestation,
                                                           ParsedAuthenticatorData authData) {
        Object x5c = attestation.attStmt().get("x5c");
        if (x5c instanceof List<?> chain && !chain.isEmpty()) {
            try {
                X509Certificate certificate = certificate(bytesValue(chain.get(0)));
                certificate.checkValidity(java.util.Date.from(Instant.now()));
                return Optional.of(certificate.getPublicKey());
            } catch (Exception ex) {
                throw new ValidationException("Passkey packed attestation 证书无效");
            }
        }
        return parseCosePublicKey(authData.credentialPublicKey());
    }

    @SuppressWarnings("unchecked")
    private Optional<PublicKey> parseCosePublicKey(byte[] bytes) {
        try {
            Map<Object, Object> cose = cborObjectMapper.readValue(bytes, LinkedHashMap.class);
            int kty = intValue(coseValue(cose, 1));
            if (kty == 2) {
                byte[] x = bytesValue(coseValue(cose, -2));
                byte[] y = bytesValue(coseValue(cose, -3));
                AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
                parameters.init(new ECGenParameterSpec("secp256r1"));
                ECParameterSpec ecSpec = parameters.getParameterSpec(ECParameterSpec.class);
                ECPublicKeySpec keySpec = new ECPublicKeySpec(
                        new ECPoint(new BigInteger(1, x), new BigInteger(1, y)), ecSpec);
                return Optional.of(KeyFactory.getInstance("EC").generatePublic(keySpec));
            }
            if (kty == 3) {
                byte[] n = bytesValue(coseValue(cose, -1));
                byte[] e = bytesValue(coseValue(cose, -2));
                return Optional.of(KeyFactory.getInstance("RSA")
                        .generatePublic(new RSAPublicKeySpec(new BigInteger(1, n), new BigInteger(1, e))));
            }
            return Optional.empty();
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private void verifySignature(PublicKey publicKey, int alg, byte[] signedData, byte[] signature) {
        try {
            Signature verifier = Signature.getInstance(signatureAlgorithm(publicKey, alg));
            verifier.initVerify(publicKey);
            verifier.update(signedData);
            if (!verifier.verify(signature)) {
                throw new ValidationException("Passkey attestation 签名验证失败");
            }
        } catch (ValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ValidationException("Passkey attestation 签名验证失败");
        }
    }

    private String signatureAlgorithm(PublicKey publicKey, int alg) {
        if (alg == -7 && publicKey instanceof ECPublicKey) {
            return "SHA256withECDSA";
        }
        if ((alg == -257 || alg == -37 || alg == -38 || alg == -39) && publicKey instanceof RSAPublicKey) {
            return "SHA256withRSA";
        }
        throw new ValidationException("Passkey attestation alg 与公钥算法不匹配");
    }

    private X509Certificate certificate(byte[] der) throws Exception {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(der));
    }

    private byte[] firstCborObjectBytes(byte[] bytes) {
        try (JsonParser parser = cborFactory.createParser(bytes)) {
            cborObjectMapper.readValue(parser, LinkedHashMap.class);
            long offset = parser.getCurrentLocation().getByteOffset();
            if (offset > 0 && offset <= bytes.length) {
                return Arrays.copyOf(bytes, (int) offset);
            }
        } catch (Exception ignored) {
            // fall through to using the complete remaining buffer
        }
        return bytes;
    }

    private boolean originMatchesRpId(String origin, String rpId) {
        if (!hasText(origin) || !hasText(rpId)) {
            return false;
        }
        try {
            URI uri = URI.create(origin);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) {
                return false;
            }
            if (!"https".equalsIgnoreCase(scheme)
                    && !"localhost".equalsIgnoreCase(host)
                    && !"127.0.0.1".equals(host)
                    && !"::1".equals(host)) {
                return false;
            }
            String normalizedHost = host.toLowerCase(Locale.ROOT);
            String normalizedRpId = rpId.toLowerCase(Locale.ROOT);
            return normalizedHost.equals(normalizedRpId)
                    || normalizedHost.endsWith("." + normalizedRpId);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private String effectiveRpId(AccountDTO.PasskeyFinishRegistrationRequest request, String expectedRpId) {
        return hasText(expectedRpId) ? expectedRpId : request.rpId();
    }

    private byte[] decodeBase64Url(String value, String fieldName) {
        if (!hasText(value)) {
            throw new ValidationException("Passkey " + fieldName + " 不能为空");
        }
        try {
            return Base64.getUrlDecoder().decode(padBase64(normalizeBase64Url(value)));
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("Passkey " + fieldName + " 不是合法 base64url");
        }
    }

    private String normalizeBase64Url(String value) {
        return value == null ? "" : value.trim().replace('+', '-').replace('/', '_').replace("=", "");
    }

    private String padBase64(String value) {
        int padding = (4 - value.length() % 4) % 4;
        return value + "=".repeat(padding);
    }

    private String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private byte[] sha256(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (Exception ex) {
            throw new ValidationException("SHA-256 不可用");
        }
    }

    private byte[] concat(byte[] first, byte[] second) {
        byte[] result = new byte[first.length + second.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    private String uuid(byte[] value) {
        ByteBuffer buffer = ByteBuffer.wrap(value);
        return new UUID(buffer.getLong(), buffer.getLong()).toString();
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private int intValue(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private Object coseValue(Map<Object, Object> cose, int key) {
        Object value = cose.get(key);
        if (value != null || cose.containsKey(key)) {
            return value;
        }
        for (Map.Entry<Object, Object> entry : cose.entrySet()) {
            Object mapKey = entry.getKey();
            if (mapKey instanceof Number number && number.intValue() == key) {
                return entry.getValue();
            }
            if (mapKey instanceof String string && String.valueOf(key).equals(string)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private byte[] bytesValue(Object value) {
        return value instanceof byte[] bytes ? bytes : new byte[0];
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public record VerifiedRegistration(
            String credentialId,
            String credentialPublicKey,
            Long signCount,
            String aaguid,
            Boolean backupEligible,
            Boolean backupState,
            String attestationFormat
    ) {}

    private record AttestationObject(
            String format,
            byte[] authData,
            Map<Object, Object> attStmt
    ) {}

    private record ParsedAuthenticatorData(
            byte[] raw,
            byte[] credentialId,
            byte[] credentialPublicKey,
            Long signCount,
            String aaguid,
            Boolean backupEligible,
            Boolean backupState
    ) {}
}
