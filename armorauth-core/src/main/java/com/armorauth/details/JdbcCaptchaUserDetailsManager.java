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
package com.armorauth.details;

import com.armorauth.data.entity.UserInfo;
import com.armorauth.data.entity.UserRole;
import com.armorauth.data.repository.UserInfoRepository;
import com.armorauth.data.repository.UserRoleRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.ObjectUtils;

import java.util.List;


public class JdbcCaptchaUserDetailsManager implements CaptchaUserDetailsService {

    private final UserInfoRepository userInfoRepository;
    private final UserRoleRepository userRoleRepository;

    public JdbcCaptchaUserDetailsManager(UserInfoRepository userInfoRepository,
                                          UserRoleRepository userRoleRepository) {
        this.userInfoRepository = userInfoRepository;
        this.userRoleRepository = userRoleRepository;
    }


    @Override
    public UserDetails loadUserByAccount(String phone) throws UsernameNotFoundException {
        UserInfo userByPhone = userInfoRepository.findOAuth2UserByPhone(phone);
        if (ObjectUtils.isEmpty(userByPhone)) {
            return null;
        }
        List<String> roles = loadRoles(userByPhone.getId());
        return User.builder()
                .username(userByPhone.getUsername())
                .password(userByPhone.getPassword())
                .roles(roles.toArray(new String[0]))
                .disabled(userByPhone.getStatus() != null && userByPhone.getStatus() == 2)
                .build();
    }

    private List<String> loadRoles(String userId) {
        return userRoleRepository.findByUserId(userId).stream()
                .map(UserRole::getRole)
                .filter(role -> role != null)
                .map(role -> role.getRoleCode())
                .toList();
    }
}
