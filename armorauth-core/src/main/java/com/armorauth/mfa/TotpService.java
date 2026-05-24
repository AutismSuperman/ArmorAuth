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
package com.armorauth.mfa;

import com.armorauth.crypto.SecretCryptoService;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * TOTP (Time-based One-Time Password) 服务
 * <p>
 * 基于 RFC 6238 实现，兼容 Google Authenticator 等标准 TOTP 应用。
 *
 * @author fulin
 * @since 2026-05-23
 */
public class TotpService {

    private static final String HMAC_SHA1 = "HmacSHA1";
    private static final int DIGITS = 6;
    private static final int PERIOD = 30; // seconds
    private static final int SECRET_BYTES = 20;
    private static final int RECOVERY_CODE_COUNT = 8;
    private static final int RECOVERY_CODE_LENGTH = 8;
    private static final char[] RECOVERY_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private final SecureRandom random = new SecureRandom();
    private final SecretCryptoService secretCryptoService;

    public TotpService() {
        this.secretCryptoService = null;
    }

    public TotpService(SecretCryptoService secretCryptoService) {
        this.secretCryptoService = secretCryptoService;
    }

    /**
     * 生成 TOTP 密钥（Base32 编码）
     *
     * @return Base32 编码的密钥
     */
    public String generateSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        random.nextBytes(bytes);
        return base32Encode(bytes);
    }

    /**
     * 生成 TOTP URI（用于二维码扫描）
     *
     * @param secret    Base32 编码的密钥
     * @param account   账户名（通常是邮箱）
     * @param issuer    发行者名称
     * @return otpauth:// URI
     */
    public String generateUri(String secret, String account, String issuer) {
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s&digits=%d&period=%d",
                issuer, account, secret, issuer, DIGITS, PERIOD);
    }

    /**
     * 生成当前时间的 TOTP 验证码
     *
     * @param secret Base32 编码的密钥
     * @return 6位验证码
     */
    public String generateCode(String secret) {
        long time = System.currentTimeMillis() / 1000 / PERIOD;
        return generateCode(secret, time);
    }

    /**
     * 验证 TOTP 验证码
     * <p>
     * 允许前后各一个时间窗口的误差（±30秒）
     *
     * @param secret Base32 编码的密钥
     * @param code   用户输入的验证码
     * @return 是否验证成功
     */
    public boolean verifyCode(String secret, String code) {
        if (secret == null || code == null || code.length() != DIGITS) {
            return false;
        }
        secret = revealSecret(secret);
        long time = System.currentTimeMillis() / 1000 / PERIOD;
        // 允许前后各一个时间窗口
        for (int i = -1; i <= 1; i++) {
            String expected = generateCode(secret, time + i);
            if (constantTimeEquals(expected, code)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 生成恢复码
     *
     * @return 恢复码列表
     */
    public List<String> generateRecoveryCodes() {
        List<String> codes = new ArrayList<>(RECOVERY_CODE_COUNT);
        for (int i = 0; i < RECOVERY_CODE_COUNT; i++) {
            StringBuilder sb = new StringBuilder(RECOVERY_CODE_LENGTH);
            for (int j = 0; j < RECOVERY_CODE_LENGTH; j++) {
                sb.append(RECOVERY_CHARS[random.nextInt(RECOVERY_CHARS.length)]);
            }
            codes.add(sb.toString());
        }
        return codes;
    }

    /**
     * 验证恢复码
     *
     * @param recoveryCodes 存储的恢复码（逗号分隔）
     * @param code          用户输入的恢复码
     * @return 验证后的恢复码列表（已使用的被移除），如果验证失败返回 null
     */
    public List<String> verifyRecoveryCode(String recoveryCodes, String code) {
        if (recoveryCodes == null || code == null) {
            return null;
        }
        List<String> codes = new ArrayList<>(List.of(recoveryCodes.split(",")));
        boolean removed = codes.removeIf(c -> constantTimeEquals(c.trim(), code.trim()));
        if (removed) {
            return codes;
        }
        return null;
    }

    private String generateCode(String secret, long time) {
        secret = revealSecret(secret);
        byte[] key = base32Decode(secret);
        byte[] timeBytes = new byte[8];
        for (int i = 7; i >= 0; i--) {
            timeBytes[i] = (byte) (time & 0xFF);
            time >>= 8;
        }

        try {
            Mac mac = Mac.getInstance(HMAC_SHA1);
            mac.init(new SecretKeySpec(key, HMAC_SHA1));
            byte[] hash = mac.doFinal(timeBytes);

            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            int otp = binary % (int) Math.pow(10, DIGITS);
            return String.format("%0" + DIGITS + "d", otp);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("TOTP 计算失败", e);
        }
    }

    /**
     * 恒定时间字符串比较，防止时序攻击
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    private String revealSecret(String secret) {
        return secretCryptoService != null ? secretCryptoService.reveal(secret) : secret;
    }

    // Base32 编码/解码（RFC 4648）

    private static final char[] BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();

    private static String base32Encode(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        int index = 0;
        int digit;
        while (i < data.length) {
            int currByte = data[i] & 0xFF;
            if (index > 3) {
                int nextByte = (i + 1 < data.length) ? (data[i + 1] & 0xFF) : 0;
                digit = currByte & (0xFF >> index);
                digit = digit << (index - 3);
                digit |= nextByte >> (8 - (index - 3));
                index -= 3;
                i++;
            } else {
                digit = (currByte >> (8 - (index + 5))) & 0x1F;
                index += 5;
                if (index >= 8) {
                    index -= 8;
                    i++;
                }
            }
            sb.append(BASE32_CHARS[digit]);
        }
        return sb.toString();
    }

    private static byte[] base32Decode(String encoded) {
        encoded = encoded.replaceAll("[= ]", "").toUpperCase();
        int length = encoded.length() * 5 / 8;
        byte[] result = new byte[length];
        int buffer = 0;
        int bitsLeft = 0;
        int count = 0;
        for (int i = 0; i < encoded.length(); i++) {
            char c = encoded.charAt(i);
            int val;
            if (c >= 'A' && c <= 'Z') {
                val = c - 'A';
            } else if (c >= '2' && c <= '7') {
                val = c - '2' + 26;
            } else {
                continue;
            }
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                result[count++] = (byte) (buffer >> (bitsLeft - 8));
                bitsLeft -= 8;
            }
        }
        return result;
    }
}
