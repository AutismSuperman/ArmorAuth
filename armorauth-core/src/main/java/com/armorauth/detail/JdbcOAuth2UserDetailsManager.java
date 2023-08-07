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
package com.armorauth.detail;

import com.armorauth.data.entity.UserInfo;
import com.armorauth.data.repository.UserInfoRepository;
import com.armorauth.detail.repository.InMemoryUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;


public class JdbcOAuth2UserDetailsManager implements OAuth2UserDetailsService {

    private final UserInfoRepository userInfoRepository;

    public JdbcOAuth2UserDetailsManager(UserInfoRepository userInfoRepository) {
        this.userInfoRepository = userInfoRepository;
    }

    @Override
    public UserDetails loadOAuth2UserByUsername(String username) throws UsernameNotFoundException {
        UserInfo userByUsername = userInfoRepository.findOAuth2UserByUsername(username);
        if (ObjectUtils.isEmpty(userByUsername)) {
            return null;
        }
        return User.builder()
                .username(userByUsername.getUsername())
                .password(userByUsername.getPassword())
                .roles("USER")
                .build();
    }
}
