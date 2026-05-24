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
 * 短信 OTP 服务
 * <p>
 * 生成和验证6位短信验证码，5分钟有效期，一次性使用。
 * 实际发送短信需集成短信服务商（当前为框架实现）。
 *
 * @author fulin
 * @since 2026-05-23
 */
@Service
public class SmsOtpService {

    private static final Logger log = LoggerFactory.getLogger(SmsOtpService.class);
    private static final int OTP_LENGTH = 6;
    private static final long EXPIRY_MS = 5 * 60 * 1000; // 5 minutes
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();

    /**
     * 生成并发送 OTP 到指定手机号
     *
     * @param phone 手机号
     * @return 生成的 OTP（生产环境不应返回）
     */
    public String generateOtp(String phone) {
        String otp = String.format("%0" + OTP_LENGTH + "d", RANDOM.nextInt((int) Math.pow(10, OTP_LENGTH)));
        otpStore.put(phone, new OtpEntry(otp, System.currentTimeMillis() + EXPIRY_MS));

        // TODO: 集成实际短信发送服务
        log.info("SMS OTP generated for {}: {}", maskPhone(phone), otp);

        return otp;
    }

    /**
     * 验证 OTP
     *
     * @param phone 手机号
     * @param otp   用户输入的验证码
     * @return true 如果验证成功
     */
    public boolean verifyOtp(String phone, String otp) {
        if (phone == null || otp == null) {
            return false;
        }

        OtpEntry entry = otpStore.get(phone);
        if (entry == null) {
            return false;
        }

        if (System.currentTimeMillis() > entry.expiryMs) {
            otpStore.remove(phone);
            return false;
        }

        if (otp.equals(entry.code)) {
            otpStore.remove(phone); // 一次性使用
            return true;
        }

        return false;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return "***";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private record OtpEntry(String code, long expiryMs) {}
}
