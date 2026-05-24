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
package com.armorauth.webauthn;

import com.armorauth.common.exception.ResourceNotFoundException;
import com.armorauth.common.exception.ValidationException;
import com.armorauth.data.entity.AuthFactor;
import com.armorauth.data.entity.UserInfo;
import com.armorauth.data.repository.AuthFactorRepository;
import com.armorauth.data.repository.UserInfoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class WebAuthnAssertionService {

    public static final String SESSION_CHALLENGE = "PENDING_PASSKEY_ASSERTION_CHALLENGE";
    public static final String SESSION_USERNAME = "PENDING_PASSKEY_ASSERTION_USERNAME";
    public static final String SESSION_RP_ID = "PENDING_PASSKEY_ASSERTION_RP_ID";
    public static final String SESSION_CREATED_AT = "PENDING_PASSKEY_ASSERTION_CREATED_AT";
    public static final String SESSION_PASSWORDLESS_CHALLENGE = "PENDING_PASSKEY_PASSWORDLESS_CHALLENGE";
    public static final String SESSION_PASSWORDLESS_USERNAME = "PENDING_PASSKEY_PASSWORDLESS_USERNAME";
    public static final String SESSION_PASSWORDLESS_RP_ID = "PENDING_PASSKEY_PASSWORDLESS_RP_ID";
    public static final String SESSION_PASSWORDLESS_CREATED_AT = "PENDING_PASSKEY_PASSWORDLESS_CREATED_AT";

    private static final String FACTOR_TYPE_WEBAUTHN = "WEBAUTHN";
    private static final long ASSERTION_TIMEOUT_MILLIS = 60_000L;
    private static final Pattern PEM_BOUNDARY = Pattern.compile("-----BEGIN [^-]+-----|-----END [^-]+-----|\\s");

    private final UserInfoRepository userRepository;
    private final AuthFactorRepository authFactorRepository;
    private final ObjectMapper objectMapper;
    private final ObjectMapper cborObjectMapper = new ObjectMapper(new CBORFactory());

    public WebAuthnAssertionService(UserInfoRepository userRepository,
                                    AuthFactorRepository authFactorRepository,
                                    ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.authFactorRepository = authFactorRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public WebAuthnAssertionDTO.AssertionOptionsResponse beginAssertion(
            String username,
            HttpServletRequest request) {
        UserInfo user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户", username));

        List<AuthFactor> factors = authFactorRepository.findByUserIdAndEnabledTrue(user.getId()).stream()
                .filter(factor -> FACTOR_TYPE_WEBAUTHN.equals(factor.getFactorType()))
                .filter(factor -> Boolean.TRUE.equals(factor.getVerified()))
                .filter(factor -> factor.getCredentialId() != null && !factor.getCredentialId().isBlank())
                .filter(factor -> factor.getCredentialPublicKey() != null && !factor.getCredentialPublicKey().isBlank())
                .toList();
        if (factors.isEmpty()) {
            throw new ValidationException("当前用户没有可用于登录的 Passkey");
        }

        String challenge = randomBase64Url(32);
        String rpId = resolveRpId(request);
        HttpSession session = request.getSession(true);
        session.setAttribute(SESSION_CHALLENGE, challenge);
        session.setAttribute(SESSION_USERNAME, username);
        session.setAttribute(SESSION_RP_ID, rpId);
        session.setAttribute(SESSION_CREATED_AT, Instant.now().toEpochMilli());

        List<WebAuthnAssertionDTO.PublicKeyCredentialDescriptor> credentials = factors.stream()
                .map(factor -> new WebAuthnAssertionDTO.PublicKeyCredentialDescriptor(
                        "public-key",
                        normalizeBase64Url(factor.getCredentialId()),
                        splitTransports(factor.getTransports())))
                .toList();

        return new WebAuthnAssertionDTO.AssertionOptionsResponse(
                challenge,
                rpId,
                ASSERTION_TIMEOUT_MILLIS,
                "preferred",
                credentials
        );
    }

    @Transactional
    public WebAuthnAssertionDTO.VerifiedAssertion finishAssertion(
            String username,
            WebAuthnAssertionDTO.AssertionFinishRequest request,
            HttpServletRequest servletRequest) {
        if (request == null) {
            throw new ValidationException("Passkey 断言请求不能为空");
        }
        HttpSession session = servletRequest.getSession(false);
        if (session == null) {
            throw new ValidationException("Passkey challenge 不存在或已过期");
        }
        String expectedUsername = stringSessionAttribute(session, SESSION_USERNAME);
        if (!Objects.equals(username, expectedUsername)) {
            throw new ValidationException("Passkey challenge 与当前登录用户不匹配");
        }
        String expectedChallenge = stringSessionAttribute(session, SESSION_CHALLENGE);
        String expectedRpId = stringSessionAttribute(session, SESSION_RP_ID);
        long createdAt = longSessionAttribute(session, SESSION_CREATED_AT);
        if (expectedChallenge == null || expectedChallenge.isBlank()
                || expectedRpId == null || expectedRpId.isBlank()
                || createdAt <= 0 || Instant.now().toEpochMilli() - createdAt > ASSERTION_TIMEOUT_MILLIS) {
            throw new ValidationException("Passkey challenge 不存在或已过期");
        }

        UserInfo user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户", username));
        AuthFactor factor = resolveCredentialFactor(user.getId(), request.credentialId())
                .orElseThrow(() -> new ValidationException("Passkey credential 不属于当前用户"));

        byte[] clientDataJson = decodeBase64Url(request.clientDataJSON(), "clientDataJSON");
        byte[] authenticatorData = decodeBase64Url(request.authenticatorData(), "authenticatorData");
        byte[] signature = decodeBase64Url(request.signature(), "signature");
        validateClientData(clientDataJson, expectedChallenge, servletRequest);
        long signCount = validateAuthenticatorData(authenticatorData, expectedRpId, factor);
        verifySignature(factor.getCredentialPublicKey(), authenticatorData, clientDataJson, signature);
        validateUserHandle(factor, request.userHandle());

        factor.setSignCount(signCount);
        factor.setLastUsedAt(Instant.now());
        authFactorRepository.save(factor);
        clearSessionChallenge(session);

        return new WebAuthnAssertionDTO.VerifiedAssertion(factor.getId(), factor.getCredentialId(), signCount);
    }

    @Transactional(readOnly = true)
    public WebAuthnAssertionDTO.AssertionOptionsResponse beginPasswordlessAssertion(
            WebAuthnAssertionDTO.PasswordlessOptionsRequest optionsRequest,
            HttpServletRequest request) {
        String username = optionsRequest != null ? trimToNull(optionsRequest.username()) : null;
        List<WebAuthnAssertionDTO.PublicKeyCredentialDescriptor> credentials = null;
        if (username != null) {
            UserInfo user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("用户", username));
            credentials = findReadyCredentials(user.getId()).stream()
                    .map(factor -> new WebAuthnAssertionDTO.PublicKeyCredentialDescriptor(
                            "public-key",
                            normalizeBase64Url(factor.getCredentialId()),
                            splitTransports(factor.getTransports())))
                    .toList();
            if (credentials.isEmpty()) {
                throw new ValidationException("当前用户没有可用于登录的 Passkey");
            }
        }

        String challenge = randomBase64Url(32);
        String rpId = resolveRpId(request);
        HttpSession session = request.getSession(true);
        session.setAttribute(SESSION_PASSWORDLESS_CHALLENGE, challenge);
        session.setAttribute(SESSION_PASSWORDLESS_USERNAME, username);
        session.setAttribute(SESSION_PASSWORDLESS_RP_ID, rpId);
        session.setAttribute(SESSION_PASSWORDLESS_CREATED_AT, Instant.now().toEpochMilli());

        return new WebAuthnAssertionDTO.AssertionOptionsResponse(
                challenge,
                rpId,
                ASSERTION_TIMEOUT_MILLIS,
                "preferred",
                credentials
        );
    }

    @Transactional
    public WebAuthnAssertionDTO.VerifiedPasswordlessAssertion finishPasswordlessAssertion(
            WebAuthnAssertionDTO.AssertionFinishRequest request,
            HttpServletRequest servletRequest) {
        if (request == null) {
            throw new ValidationException("Passkey 断言请求不能为空");
        }
        HttpSession session = servletRequest.getSession(false);
        if (session == null) {
            throw new ValidationException("Passkey challenge 不存在或已过期");
        }
        String expectedChallenge = stringSessionAttribute(session, SESSION_PASSWORDLESS_CHALLENGE);
        String expectedUsername = stringSessionAttribute(session, SESSION_PASSWORDLESS_USERNAME);
        String expectedRpId = stringSessionAttribute(session, SESSION_PASSWORDLESS_RP_ID);
        long createdAt = longSessionAttribute(session, SESSION_PASSWORDLESS_CREATED_AT);
        if (expectedChallenge == null || expectedChallenge.isBlank()
                || expectedRpId == null || expectedRpId.isBlank()
                || createdAt <= 0 || Instant.now().toEpochMilli() - createdAt > ASSERTION_TIMEOUT_MILLIS) {
            throw new ValidationException("Passkey challenge 不存在或已过期");
        }

        AuthFactor factor = authFactorRepository.findByCredentialId(normalizeBase64Url(request.credentialId()))
                .filter(this::isReadyWebAuthnFactor)
                .orElseThrow(() -> new ValidationException("Passkey credential 不存在或不可用"));
        UserInfo user = userRepository.findById(factor.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("用户", factor.getUserId()));
        if (expectedUsername != null && !expectedUsername.equals(user.getUsername())) {
            throw new ValidationException("Passkey credential 与用户名不匹配");
        }

        byte[] clientDataJson = decodeBase64Url(request.clientDataJSON(), "clientDataJSON");
        byte[] authenticatorData = decodeBase64Url(request.authenticatorData(), "authenticatorData");
        byte[] signature = decodeBase64Url(request.signature(), "signature");
        validateClientData(clientDataJson, expectedChallenge, servletRequest);
        long signCount = validateAuthenticatorData(authenticatorData, expectedRpId, factor);
        verifySignature(factor.getCredentialPublicKey(), authenticatorData, clientDataJson, signature);
        validateUserHandle(factor, request.userHandle());

        factor.setSignCount(signCount);
        factor.setLastUsedAt(Instant.now());
        authFactorRepository.save(factor);
        clearPasswordlessSessionChallenge(session);

        return new WebAuthnAssertionDTO.VerifiedPasswordlessAssertion(
                user.getUsername(), factor.getId(), factor.getCredentialId(), signCount);
    }

    public String resolveRpId(HttpServletRequest request) {
        return hostWithoutPort(resolveHost(request)).toLowerCase(Locale.ROOT);
    }

    public String resolveOrigin(HttpServletRequest request) {
        String forwardedProto = trimToNull(request.getHeader("X-Forwarded-Proto"));
        String proto = forwardedProto != null ? forwardedProto.split(",")[0].trim() : request.getScheme();
        return proto + "://" + resolveHost(request);
    }

    public void clearSessionChallenge(HttpSession session) {
        if (session == null) {
            return;
        }
        session.removeAttribute(SESSION_CHALLENGE);
        session.removeAttribute(SESSION_USERNAME);
        session.removeAttribute(SESSION_RP_ID);
        session.removeAttribute(SESSION_CREATED_AT);
    }

    public void clearPasswordlessSessionChallenge(HttpSession session) {
        if (session == null) {
            return;
        }
        session.removeAttribute(SESSION_PASSWORDLESS_CHALLENGE);
        session.removeAttribute(SESSION_PASSWORDLESS_USERNAME);
        session.removeAttribute(SESSION_PASSWORDLESS_RP_ID);
        session.removeAttribute(SESSION_PASSWORDLESS_CREATED_AT);
    }

    private List<AuthFactor> findReadyCredentials(String userId) {
        return authFactorRepository.findByUserIdAndEnabledTrue(userId).stream()
                .filter(this::isReadyWebAuthnFactor)
                .toList();
    }

    private boolean isReadyWebAuthnFactor(AuthFactor factor) {
        return FACTOR_TYPE_WEBAUTHN.equals(factor.getFactorType())
                && Boolean.TRUE.equals(factor.getVerified())
                && factor.getCredentialId() != null && !factor.getCredentialId().isBlank()
                && factor.getCredentialPublicKey() != null && !factor.getCredentialPublicKey().isBlank();
    }

    private Optional<AuthFactor> resolveCredentialFactor(String userId, String credentialId) {
        String normalizedCredentialId = normalizeBase64Url(credentialId);
        return authFactorRepository.findByUserIdAndEnabledTrue(userId).stream()
                .filter(factor -> FACTOR_TYPE_WEBAUTHN.equals(factor.getFactorType()))
                .filter(factor -> Boolean.TRUE.equals(factor.getVerified()))
                .filter(factor -> normalizeBase64Url(factor.getCredentialId()).equals(normalizedCredentialId))
                .findFirst();
    }

    private void validateClientData(byte[] clientDataJson, String expectedChallenge, HttpServletRequest request) {
        try {
            JsonNode clientData = objectMapper.readTree(clientDataJson);
            if (!"webauthn.get".equals(clientData.path("type").asText())) {
                throw new ValidationException("Passkey clientData.type 不正确");
            }
            if (!normalizeBase64Url(expectedChallenge).equals(normalizeBase64Url(clientData.path("challenge").asText()))) {
                throw new ValidationException("Passkey challenge 不匹配");
            }
            String expectedOrigin = resolveOrigin(request);
            String actualOrigin = clientData.path("origin").asText();
            if (!expectedOrigin.equalsIgnoreCase(actualOrigin)) {
                throw new ValidationException("Passkey origin 不匹配: " + actualOrigin);
            }
        } catch (ValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ValidationException("Passkey clientDataJSON 无法解析");
        }
    }

    private long validateAuthenticatorData(byte[] authenticatorData, String expectedRpId, AuthFactor factor) {
        if (authenticatorData.length < 37) {
            throw new ValidationException("Passkey authenticatorData 长度不正确");
        }
        byte[] expectedRpIdHash = sha256(expectedRpId.getBytes(StandardCharsets.UTF_8));
        for (int i = 0; i < expectedRpIdHash.length; i++) {
            if (authenticatorData[i] != expectedRpIdHash[i]) {
                throw new ValidationException("Passkey rpIdHash 不匹配");
            }
        }
        int flags = authenticatorData[32] & 0xff;
        if ((flags & 0x01) == 0) {
            throw new ValidationException("Passkey 缺少 user present 标记");
        }
        long signCount = ((authenticatorData[33] & 0xffL) << 24)
                | ((authenticatorData[34] & 0xffL) << 16)
                | ((authenticatorData[35] & 0xffL) << 8)
                | (authenticatorData[36] & 0xffL);
        Long previousSignCount = factor.getSignCount();
        if (previousSignCount != null && previousSignCount > 0 && signCount > 0 && signCount <= previousSignCount) {
            throw new ValidationException("Passkey signCount 回退，可能存在克隆凭据");
        }
        return signCount;
    }

    private void verifySignature(String publicKeyValue, byte[] authenticatorData, byte[] clientDataJson, byte[] signature) {
        PublicKey publicKey = parsePublicKey(publicKeyValue);
        byte[] clientDataHash = sha256(clientDataJson);
        byte[] signedData = new byte[authenticatorData.length + clientDataHash.length];
        System.arraycopy(authenticatorData, 0, signedData, 0, authenticatorData.length);
        System.arraycopy(clientDataHash, 0, signedData, authenticatorData.length, clientDataHash.length);
        try {
            Signature verifier = Signature.getInstance(signatureAlgorithm(publicKey));
            verifier.initVerify(publicKey);
            verifier.update(signedData);
            if (!verifier.verify(signature)) {
                throw new ValidationException("Passkey 签名验证失败");
            }
        } catch (ValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ValidationException("Passkey 签名验证失败: " + ex.getMessage());
        }
    }

    private void validateUserHandle(AuthFactor factor, String requestUserHandle) {
        if (requestUserHandle == null || requestUserHandle.isBlank() || factor.getWebauthnUserHandle() == null) {
            return;
        }
        if (!normalizeBase64Url(factor.getWebauthnUserHandle()).equals(normalizeBase64Url(requestUserHandle))) {
            throw new ValidationException("Passkey userHandle 不匹配");
        }
    }

    private PublicKey parsePublicKey(String publicKeyValue) {
        if (publicKeyValue == null || publicKeyValue.isBlank()) {
            throw new ValidationException("Passkey public key 未配置");
        }
        String trimmed = publicKeyValue.trim();
        if (trimmed.startsWith("{")) {
            return parseJwkPublicKey(trimmed);
        }
        String compact = PEM_BOUNDARY.matcher(trimmed).replaceAll("");
        byte[] bytes = decodeFlexibleBase64(compact, "publicKey");
        return parseSpkiPublicKey(bytes)
                .or(() -> parseCosePublicKey(bytes))
                .orElseThrow(() -> new ValidationException("Passkey public key 格式不支持"));
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
                RSAPublicKeySpec keySpec = new RSAPublicKeySpec(new BigInteger(1, n), new BigInteger(1, e));
                return Optional.of(KeyFactory.getInstance("RSA").generatePublic(keySpec));
            }
            return Optional.empty();
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private Optional<PublicKey> parseSpkiPublicKey(byte[] bytes) {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(bytes);
        for (String algorithm : List.of("EC", "RSA")) {
            try {
                return Optional.of(KeyFactory.getInstance(algorithm).generatePublic(keySpec));
            } catch (Exception ignored) {
                // try next algorithm
            }
        }
        return Optional.empty();
    }

    private PublicKey parseJwkPublicKey(String json) {
        try {
            JsonNode jwk = objectMapper.readTree(json);
            String kty = jwk.path("kty").asText();
            if ("EC".equals(kty)) {
                byte[] x = decodeBase64Url(jwk.path("x").asText(), "jwk.x");
                byte[] y = decodeBase64Url(jwk.path("y").asText(), "jwk.y");
                AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
                parameters.init(new ECGenParameterSpec("secp256r1"));
                ECParameterSpec ecSpec = parameters.getParameterSpec(ECParameterSpec.class);
                ECPublicKeySpec keySpec = new ECPublicKeySpec(
                        new ECPoint(new BigInteger(1, x), new BigInteger(1, y)), ecSpec);
                return KeyFactory.getInstance("EC").generatePublic(keySpec);
            }
            if ("RSA".equals(kty)) {
                RSAPublicKeySpec keySpec = new RSAPublicKeySpec(
                        new BigInteger(1, decodeBase64Url(jwk.path("n").asText(), "jwk.n")),
                        new BigInteger(1, decodeBase64Url(jwk.path("e").asText(), "jwk.e")));
                return KeyFactory.getInstance("RSA").generatePublic(keySpec);
            }
            throw new ValidationException("Passkey JWK kty 不支持: " + kty);
        } catch (ValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ValidationException("Passkey JWK public key 无法解析");
        }
    }

    private String signatureAlgorithm(PublicKey publicKey) {
        if (publicKey instanceof ECPublicKey) {
            return "SHA256withECDSA";
        }
        if (publicKey instanceof RSAPublicKey) {
            return "SHA256withRSA";
        }
        throw new ValidationException("Passkey public key 算法不支持: " + publicKey.getAlgorithm());
    }

    private List<String> splitTransports(String transports) {
        if (transports == null || transports.isBlank()) {
            return null;
        }
        List<String> result = new ArrayList<>();
        for (String transport : transports.split(",")) {
            String trimmed = transport.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result.isEmpty() ? null : result;
    }

    private byte[] decodeBase64Url(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("Passkey " + fieldName + " 不能为空");
        }
        try {
            return Base64.getUrlDecoder().decode(padBase64(normalizeBase64Url(value)));
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("Passkey " + fieldName + " 不是合法 base64url");
        }
    }

    private byte[] decodeFlexibleBase64(String value, String fieldName) {
        String normalized = normalizeBase64Url(value);
        try {
            return Base64.getUrlDecoder().decode(padBase64(normalized));
        } catch (IllegalArgumentException ignored) {
            try {
                return Base64.getDecoder().decode(padBase64(value));
            } catch (IllegalArgumentException ex) {
                throw new ValidationException("Passkey " + fieldName + " 不是合法 base64/base64url");
            }
        }
    }

    private String normalizeBase64Url(String value) {
        return value == null ? "" : value.trim().replace('+', '-').replace('/', '_').replace("=", "");
    }

    private String padBase64(String value) {
        int padding = (4 - value.length() % 4) % 4;
        return value + "=".repeat(padding);
    }

    private byte[] sha256(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (Exception ex) {
            throw new ValidationException("SHA-256 不可用");
        }
    }

    private String randomBase64Url(int byteLength) {
        byte[] bytes = new byte[byteLength];
        new java.security.SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String resolveHost(HttpServletRequest request) {
        String forwardedHost = trimToNull(request.getHeader("X-Forwarded-Host"));
        if (forwardedHost != null) {
            return forwardedHost.split(",")[0].trim();
        }
        String host = trimToNull(request.getHeader("Host"));
        if (host != null) {
            return host;
        }
        int port = request.getServerPort();
        boolean defaultPort = ("https".equalsIgnoreCase(request.getScheme()) && port == 443)
                || ("http".equalsIgnoreCase(request.getScheme()) && port == 80);
        return defaultPort ? request.getServerName() : request.getServerName() + ":" + port;
    }

    private String hostWithoutPort(String host) {
        if (host.startsWith("[")) {
            int end = host.indexOf(']');
            return end > 0 ? host.substring(1, end) : host;
        }
        int colon = host.indexOf(':');
        return colon > 0 ? host.substring(0, colon) : host;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String stringSessionAttribute(HttpSession session, String name) {
        Object value = session.getAttribute(name);
        return value instanceof String string ? string : null;
    }

    private long longSessionAttribute(HttpSession session, String name) {
        Object value = session.getAttribute(name);
        return value instanceof Number number ? number.longValue() : 0L;
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
}
