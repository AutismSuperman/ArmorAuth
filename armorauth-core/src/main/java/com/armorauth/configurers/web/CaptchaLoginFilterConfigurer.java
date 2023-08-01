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


import com.armorauth.authentication.CaptchaAuthenticationFilter;
import com.armorauth.authentication.CaptchaAuthenticationProvider;
import com.armorauth.authentication.CaptchaVerifyService;
import com.armorauth.detail.CaptchaUserDetailsService;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;

public class CaptchaLoginFilterConfigurer<H extends HttpSecurityBuilder<H>>
        extends CaptchaAbstractLoginFilterConfigurer
            <
            H,
            CaptchaLoginFilterConfigurer<H>, CaptchaAuthenticationFilter,
            Oauth2UserLoginFilterSecurityConfigurer<H>
            > {

    private CaptchaUserDetailsService captchaUserDetailsService;

    private CaptchaVerifyService captchaVerifyService;

    public CaptchaLoginFilterConfigurer(Oauth2UserLoginFilterSecurityConfigurer<H> securityConfigurer) {
        super(securityConfigurer, new CaptchaAuthenticationFilter(), "/login/captcha");
    }

    public CaptchaLoginFilterConfigurer<H> captchaUserDetailsService(CaptchaUserDetailsService captchaUserDetailsService) {
        this.captchaUserDetailsService = captchaUserDetailsService;
        return this;
    }

    public CaptchaLoginFilterConfigurer<H> captchaVerifyService(CaptchaVerifyService captchaVerifyService) {
        this.captchaVerifyService = captchaVerifyService;
        return this;
    }


    @Override
    protected RequestMatcher createLoginProcessingUrlMatcher(String loginProcessingUrl) {
        return new AntPathRequestMatcher(loginProcessingUrl, "POST");
    }


    @Override
    protected AuthenticationProvider authenticationProvider(H http) {
        ApplicationContext applicationContext = http.getSharedObject(ApplicationContext.class);
        CaptchaUserDetailsService captchaUserDetailsService = this.captchaUserDetailsService != null ? this.captchaUserDetailsService : getBeanOrNull(applicationContext, CaptchaUserDetailsService.class);
        Assert.notNull(captchaUserDetailsService, "captchaUserDetailsService is required");
        CaptchaVerifyService captchaService = this.captchaVerifyService != null ? this.captchaVerifyService : getBeanOrNull(applicationContext, CaptchaVerifyService.class);
        Assert.notNull(captchaService, "captchaService is required");
        return new CaptchaAuthenticationProvider(captchaUserDetailsService, captchaService);
    }

}
