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
package com.armorauth.configurers.web;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.web.DefaultSecurityFilterChain;

/**
 * Oauth 用户登录验证器
 */
public class Oauth2UserLoginFilterSecurityConfigurer<H extends HttpSecurityBuilder<H>> extends SecurityConfigurerAdapter<DefaultSecurityFilterChain, H> {


    //验证码验证
    private CaptchaLoginFilterConfigurer<H> captchaLoginFilterConfigurer;

    @Deprecated
    public CaptchaLoginFilterConfigurer<H> captchaLogin() {
        return lazyInitCaptchaLoginFilterConfigurer();
    }

    public Oauth2UserLoginFilterSecurityConfigurer<H> captchaLogin(Customizer<CaptchaLoginFilterConfigurer<H>> captchaLoginFilterConfigurerCustomizer) {
        captchaLoginFilterConfigurerCustomizer.customize(lazyInitCaptchaLoginFilterConfigurer());
        return this;
    }


    @Override
    public void init(H builder) throws Exception {
        if (captchaLoginFilterConfigurer != null) {
            captchaLoginFilterConfigurer.init(builder);
        }
    }

    @Override
    public void configure(H builder) throws Exception {
        if (captchaLoginFilterConfigurer != null) {
            captchaLoginFilterConfigurer.configure(builder);
        }
    }

    private CaptchaLoginFilterConfigurer<H> lazyInitCaptchaLoginFilterConfigurer() {
        if (captchaLoginFilterConfigurer == null) {
            this.captchaLoginFilterConfigurer = new CaptchaLoginFilterConfigurer<>(this);
        }
        return captchaLoginFilterConfigurer;
    }

}
