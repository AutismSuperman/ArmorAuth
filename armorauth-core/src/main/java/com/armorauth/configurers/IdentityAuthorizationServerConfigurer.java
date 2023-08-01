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
package com.armorauth.configurers;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

import java.util.LinkedHashMap;
import java.util.Map;

public class IdentityAuthorizationServerConfigurer extends
        AbstractHttpConfigurer<IdentityAuthorizationServerConfigurer, HttpSecurity> {


    private final Map<Class<? extends AbstractIdentityConfigurer>, AbstractIdentityConfigurer>
            configurers = createConfigurers();


    public IdentityAuthorizationServerConfigurer federatedAuthorization(
            Customizer<FederatedAuthorizationConfigurer> federatedAuthorizationCustomizer) {
        federatedAuthorizationCustomizer.customize(getConfigurer(FederatedAuthorizationConfigurer.class));
        return this;
    }


    @Override
    public void init(HttpSecurity httpSecurity) throws Exception {
        for (AbstractIdentityConfigurer configurer : this.configurers.values()) {
            configurer.init(httpSecurity);
        }
    }

    @Override
    public void configure(HttpSecurity httpSecurity) throws Exception {
        for (AbstractIdentityConfigurer configurer : this.configurers.values()) {
            configurer.configure(httpSecurity);
        }
    }


    private Map<Class<? extends AbstractIdentityConfigurer>, AbstractIdentityConfigurer> createConfigurers() {
        Map<Class<? extends AbstractIdentityConfigurer>, AbstractIdentityConfigurer> configurers = new LinkedHashMap<>();
        configurers.put(FederatedAuthorizationConfigurer.class,
                new FederatedAuthorizationConfigurer(this::postProcess));
        return configurers;
    }


    @SuppressWarnings("unchecked")
    private <T> T getConfigurer(Class<T> type) {
        return (T) this.configurers.get(type);
    }

    private <T extends AbstractIdentityConfigurer> void addConfigurer(Class<T> configurerType, T configurer) {
        this.configurers.put(configurerType, configurer);
    }


}
