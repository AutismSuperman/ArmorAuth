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
package com.armorauth.federation.web;

import com.armorauth.captcha.GraphicCaptchaService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

/**
 * 图形验证码控制器
 * <p>
 * 提供验证码图片生成和获取接口
 *
 * @author fulin
 * @since 2026-05-23
 */
@Controller
public class CaptchaController {

    private final ObjectProvider<GraphicCaptchaService> captchaServiceProvider;

    public CaptchaController(ObjectProvider<GraphicCaptchaService> captchaServiceProvider) {
        this.captchaServiceProvider = captchaServiceProvider;
    }

    /**
     * 获取验证码图片
     * <p>
     * 返回验证码图片和 captchaId（通过响应头 X-Captcha-Id 传递）
     */
    @GetMapping(path = "/login/captcha/image", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> captchaImage() {
        GraphicCaptchaService captchaService = captchaServiceProvider.getIfAvailable();
        if (captchaService == null) {
            return ResponseEntity.notFound().build();
        }
        GraphicCaptchaService.CaptchaResult result = captchaService.generate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.set("X-Captcha-Id", result.captchaId());
        headers.setCacheControl("no-cache, no-store, must-revalidate");
        return new ResponseEntity<>(result.image(), headers, HttpStatus.OK);
    }

    /**
     * 获取验证码（JSON 格式，兼容前端 AJAX 调用）
     * <p>
     * 返回 captchaId，前端需要使用此 ID 和图片一起提交
     */
    @GetMapping(path = "/login/captcha/info", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> captchaInfo() {
        GraphicCaptchaService captchaService = captchaServiceProvider.getIfAvailable();
        if (captchaService == null) {
            return ResponseEntity.notFound().build();
        }
        GraphicCaptchaService.CaptchaResult result = captchaService.generate();
        return ResponseEntity.ok()
                .header("X-Captcha-Id", result.captchaId())
                .body(Map.of(
                        "captchaId", result.captchaId(),
                        "message", "请查看验证码图片"
                ));
    }
}
