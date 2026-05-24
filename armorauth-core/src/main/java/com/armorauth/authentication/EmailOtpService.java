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
package com.armorauth.authentication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 邮箱 OTP 服务
 * <p>
 * 生成和验证6位邮箱验证码，5分钟有效期，一次性使用。
 * 实际发送邮件需集成邮件服务（当前为框架实现）。
 *
 * @author fulin
 * @since 2026-05-23
 */
@Service
public class EmailOtpService {

    private static final Logger log = LoggerFactory.getLogger(EmailOtpService.class);
    private static final int OTP_LENGTH = 6;
    private static final long EXPIRY_MS = 5 * 60 * 1000; // 5 minutes
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();

    /**
     * 生成并发送 OTP 到指定邮箱
     *
     * @param email 邮箱地址
     * @return 生成的 OTP（生产环境不应返回）
     */
    public String generateOtp(String email) {
        String otp = String.format("%0" + OTP_LENGTH + "d", RANDOM.nextInt((int) Math.pow(10, OTP_LENGTH)));
        otpStore.put(email, new OtpEntry(otp, System.currentTimeMillis() + EXPIRY_MS));

        // TODO: 集成实际邮件发送服务
        log.info("Email OTP generated for {}: {}", maskEmail(email), otp);

        return otp;
    }

    /**
     * 验证 OTP
     *
     * @param email 邮箱地址
     * @param otp   用户输入的验证码
     * @return true 如果验证成功
     */
    public boolean verifyOtp(String email, String otp) {
        if (email == null || otp == null) {
            return false;
        }

        OtpEntry entry = otpStore.get(email);
        if (entry == null) {
            return false;
        }

        if (System.currentTimeMillis() > entry.expiryMs) {
            otpStore.remove(email);
            return false;
        }

        if (otp.equals(entry.code)) {
            otpStore.remove(email); // 一次性使用
            return true;
        }

        return false;
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) return "***" + email.substring(atIndex);
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }

    private record OtpEntry(String code, long expiryMs) {}
}
