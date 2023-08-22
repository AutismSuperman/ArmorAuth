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
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.FormLoginConfigurer;
import org.springframework.security.config.annotation.web.configurers.RememberMeConfigurer;
import org.springframework.security.web.DefaultSecurityFilterChain;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OAuth2 用户登录验证器
 */
public class OAuth2UserLoginFilterSecurityConfigurer extends AbstractHttpConfigurer<OAuth2UserLoginFilterSecurityConfigurer, HttpSecurity> {


    private final Map<Class<?>, SecurityConfigurerAdapter<DefaultSecurityFilterChain, HttpSecurity>>
            configurers = new LinkedHashMap<>();


    public OAuth2UserLoginFilterSecurityConfigurer captchaLogin(Customizer<CaptchaLoginConfigurer<HttpSecurity>> captchaLoginCustomizer) {
        captchaLoginCustomizer.customize(getConfigurer(new CaptchaLoginConfigurer<>()));
        return this;
    }

    public OAuth2UserLoginFilterSecurityConfigurer formLogin(Customizer<FormLoginConfigurer<HttpSecurity>> formLoginCustomizer) {
        formLoginCustomizer.customize(getConfigurer(new FormLoginConfigurer<>()));
        return this;
    }

    public OAuth2UserLoginFilterSecurityConfigurer rememberMe(Customizer<RememberMeConfigurer<HttpSecurity>> rememberMeCustomizer) {
        rememberMeCustomizer.customize(getConfigurer(new RememberMeConfigurer<>()));
        return this;
    }


    @Override
    public void init(HttpSecurity httpSecurity) throws Exception {
        for (SecurityConfigurerAdapter<DefaultSecurityFilterChain, HttpSecurity> configurer : this.configurers.values()) {
            configurer.init(httpSecurity);
        }
    }

    @Override
    public void configure(HttpSecurity httpSecurity) throws Exception {
        for (SecurityConfigurerAdapter<DefaultSecurityFilterChain, HttpSecurity> configurer : this.configurers.values()) {
            configurer.configure(httpSecurity);
        }
    }


    @SuppressWarnings("unchecked")
    private <T extends SecurityConfigurerAdapter<DefaultSecurityFilterChain, HttpSecurity>> T getConfigurer(T configurerAdapter) {
        T existingConfig = (T) this.configurers.get(configurerAdapter.getClass());
        if (existingConfig != null) {
            return existingConfig;
        }
        configurerAdapter.setBuilder(getBuilder());
        this.configurers.put(configurerAdapter.getClass(), configurerAdapter);
        return (T) this.configurers.get(configurerAdapter.getClass());
    }


}
