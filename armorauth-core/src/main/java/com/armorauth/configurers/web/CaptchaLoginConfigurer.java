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
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;

public class CaptchaLoginConfigurer<H extends HttpSecurityBuilder<H>>
        extends CustomizeAbstractAuthenticationFilterConfigurer<H, CaptchaLoginConfigurer<H>, CaptchaAuthenticationFilter> {

    private UserDetailsService userDetailsService;

    private CaptchaVerifyService captchaVerifyService;

    public CaptchaLoginConfigurer() {
        super(new CaptchaAuthenticationFilter(), "/login/captcha");
        accountParameter("account");
        captchaParameter("captcha");
    }


    @Override
    public CaptchaLoginConfigurer<H> loginPage(String loginPage) {
        return super.loginPage(loginPage);
    }

    public CaptchaLoginConfigurer<H> userDetailsService(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
        return this;
    }

    public CaptchaLoginConfigurer<H> captchaVerifyService(CaptchaVerifyService captchaVerifyService) {
        this.captchaVerifyService = captchaVerifyService;
        return this;
    }

    public CaptchaLoginConfigurer<H> accountParameter(String usernameParameter) {
        getAuthenticationFilter().setAccountParameter(usernameParameter);
        return this;
    }

    public CaptchaLoginConfigurer<H> captchaParameter(String passwordParameter) {
        getAuthenticationFilter().setCaptchaParameter(passwordParameter);
        return this;
    }


    @Override
    protected RequestMatcher createLoginProcessingUrlMatcher(String loginProcessingUrl) {
        return new AntPathRequestMatcher(loginProcessingUrl, "POST");
    }


    @Override
    public void init(H http) throws Exception {
        super.init(http);
        AuthenticationProvider authenticationProvider = authenticationProvider(http);
        http.authenticationProvider(postProcess(authenticationProvider));
    }

    @Override
    public void configure(H http) throws Exception {
        super.configure(http);
    }

    @Override
    protected void orderFilter(H http, CaptchaAuthenticationFilter filter) {
        http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);
    }

    protected AuthenticationProvider authenticationProvider(H http) {
        ApplicationContext applicationContext = http.getSharedObject(ApplicationContext.class);
        UserDetailsService userDetailsService = this.userDetailsService != null ? this.userDetailsService : getBeanOrNull(applicationContext, UserDetailsService.class);
        Assert.notNull(userDetailsService, "userDetailsService is required");
        CaptchaVerifyService captchaService = this.captchaVerifyService != null ? this.captchaVerifyService : getBeanOrNull(applicationContext, CaptchaVerifyService.class);
        Assert.notNull(captchaService, "captchaService is required");
        return new CaptchaAuthenticationProvider(userDetailsService, captchaService);
    }

    public final <T> T getBeanOrNull(ApplicationContext applicationContext, Class<T> beanType) {
        String[] beanNames = applicationContext.getBeanNamesForType(beanType);
        if (beanNames.length == 1) {
            return applicationContext.getBean(beanNames[0], beanType);
        }
        return null;
    }

}
