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
package com.armorauth.config;

import com.armorauth.authentication.CaptchaVerifyService;
import com.armorauth.captcha.GraphicCaptchaService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class DefaultCaptchaConfiguration {

    /**
     * 默认图形验证码服务
     * <p>
     * 可通过设置 armorauth.captcha.type=mock 切换回演示模式的固定验证码 "1234"
     */
    @Bean
    @ConditionalOnMissingBean(CaptchaVerifyService.class)
    @ConditionalOnProperty(name = "armorauth.captcha.type", havingValue = "graphic", matchIfMissing = true)
    public GraphicCaptchaService graphicCaptchaService() {
        return new GraphicCaptchaService();
    }

    /**
     * 演示模式验证码（固定 1234）
     * <p>
     * 设置 armorauth.captcha.type=mock 启用
     */
    @Bean
    @ConditionalOnMissingBean(CaptchaVerifyService.class)
    @ConditionalOnProperty(name = "armorauth.captcha.type", havingValue = "mock")
    public CaptchaVerifyService mockCaptchaVerifyService() {
        return (account, captcha) -> "1234".equals(captcha);
    }

}
