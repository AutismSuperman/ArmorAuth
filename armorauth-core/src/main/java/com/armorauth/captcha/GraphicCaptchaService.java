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
package com.armorauth.captcha;

import com.armorauth.authentication.CaptchaVerifyService;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图形验证码服务
 * <p>
 * 生成随机验证码图片并验证用户输入。验证码存储在内存中，带有过期时间。
 *
 * @author fulin
 * @since 2026-05-23
 */
public class GraphicCaptchaService implements CaptchaVerifyService {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final int DEFAULT_LENGTH = 4;
    private static final int DEFAULT_WIDTH = 120;
    private static final int DEFAULT_HEIGHT = 40;
    private static final long DEFAULT_EXPIRE_MS = 300_000L; // 5 minutes

    private final SecureRandom random = new SecureRandom();
    private final Map<String, CaptchaEntry> captchaStore = new ConcurrentHashMap<>();
    private final int length;
    private final int width;
    private final int height;
    private final long expireMs;

    public GraphicCaptchaService() {
        this(DEFAULT_LENGTH, DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_EXPIRE_MS);
    }

    public GraphicCaptchaService(int length, int width, int height, long expireMs) {
        this.length = length;
        this.width = width;
        this.height = height;
        this.expireMs = expireMs;
    }

    /**
     * 生成验证码，返回验证码图片的字节数组和验证码ID
     *
     * @return CaptchaResult 包含 captchaId 和图片字节
     */
    public CaptchaResult generate() {
        String captchaId = generateId();
        String code = generateCode();
        captchaStore.put(captchaId, new CaptchaEntry(code, System.currentTimeMillis() + expireMs));
        byte[] image = generateImage(code);
        return new CaptchaResult(captchaId, image);
    }

    /**
     * 验证验证码，验证成功后立即失效（一次性）
     *
     * @param captchaId 验证码ID
     * @param code      用户输入的验证码
     * @return 是否验证成功
     */
    public boolean verify(String captchaId, String code) {
        if (captchaId == null || code == null) {
            return false;
        }
        CaptchaEntry entry = captchaStore.remove(captchaId);
        if (entry == null) {
            return false;
        }
        if (System.currentTimeMillis() > entry.expireAt) {
            return false;
        }
        return code.equalsIgnoreCase(entry.code);
    }

    /**
     * 实现 CaptchaVerifyService 接口，兼容旧的验证码登录流程
     * <p>
     * 注意：此方法不使用 captchaId，仅用于兼容旧接口。
     * 新的图形验证码应使用 generate() + verify(captchaId, code) 方式。
     */
    @Override
    public boolean verifyCaptcha(String account, String captcha) {
        // 对于图形验证码模式，此方法不适用
        // 验证码通过 verify(captchaId, code) 验证
        return false;
    }

    private String generateId() {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    private byte[] generateImage(String code) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // 背景
        g.setColor(new Color(240, 240, 240));
        g.fillRect(0, 0, width, height);

        // 干扰线
        g.setColor(new Color(200, 200, 200));
        for (int i = 0; i < 6; i++) {
            int x1 = random.nextInt(width);
            int y1 = random.nextInt(height);
            int x2 = random.nextInt(width);
            int y2 = random.nextInt(height);
            g.drawLine(x1, y1, x2, y2);
        }

        // 验证码字符
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        int x = 10;
        for (int i = 0; i < code.length(); i++) {
            g.setColor(new Color(
                    random.nextInt(100),
                    random.nextInt(100),
                    random.nextInt(100)
            ));
            int angle = random.nextInt(30) - 15;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.rotate(Math.toRadians(angle), x + 14, height / 2.0);
            g2.drawString(String.valueOf(code.charAt(i)), x, height - 10);
            g2.dispose();
            x += 26;
        }

        // 噪点
        g.setColor(new Color(180, 180, 180));
        for (int i = 0; i < 30; i++) {
            int px = random.nextInt(width);
            int py = random.nextInt(height);
            g.fillOval(px, py, 2, 2);
        }

        g.dispose();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "PNG", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("生成验证码图片失败", e);
        }
    }

    /**
     * 清理过期的验证码
     */
    public void cleanup() {
        long now = System.currentTimeMillis();
        captchaStore.entrySet().removeIf(entry -> now > entry.getValue().expireAt);
    }

    /**
     * 验证码结果
     */
    public record CaptchaResult(String captchaId, byte[] image) {
    }

    private record CaptchaEntry(String code, long expireAt) {
    }
}
