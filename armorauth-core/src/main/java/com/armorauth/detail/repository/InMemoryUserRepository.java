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
package com.armorauth.detail.repository;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

public class InMemoryUserRepository {

    public final static InMemoryUserDetailsManager userDetailsManager = new InMemoryUserDetailsManager();

    static {
        UserDetails userDetails = User.builder()
                .username("root")
                .password("root")
                .passwordEncoder(PasswordEncoderFactories.createDelegatingPasswordEncoder()::encode)
                .roles("user")
                .build();
        userDetailsManager.createUser(userDetails);
    }

    public static void addUser(UserDetails user) {
        userDetailsManager.createUser(user);
    }


    public static UserDetails findUser(String userName) {
        return userDetailsManager.loadUserByUsername(userName);
    }

    public static void main(String[] args) {
        UserDetails userDetails = User.builder()
                .username("root")
                .password("root")
                .passwordEncoder(PasswordEncoderFactories.createDelegatingPasswordEncoder()::encode)
                .roles("user")
                .build();
        InMemoryUserRepository.addUser(userDetails);
    }
}