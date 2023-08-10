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

import com.armorauth.data.repository.UserInfoRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.Assert;

import java.util.LinkedList;
import java.util.List;

public class DelegateUserDetailsService implements UserDetailsService {

    private List<UserDetailsService> userDetailsServices;

    public DelegateUserDetailsService(UserInfoRepository userInfoRepository) {
        this.userDetailsServices = new LinkedList<>();
        userDetailsServices.add(new JdbcCaptchaUserDetailsManager(userInfoRepository)::loadUserByAccount);
        userDetailsServices.add(new JdbcOAuth2UserDetailsManager(userInfoRepository)::loadOAuth2UserByUsername);
    }


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Assert.notNull(username, "username cannot be null");
        for (UserDetailsService delegate : this.userDetailsServices) {
            UserDetails userDetails = delegate.loadUserByUsername(username);
            if (userDetails != null) {
                return userDetails;
            }
        }
        throw new UsernameNotFoundException("User not found");
    }

    public void setUserDetailsServices(List<UserDetailsService> userDetailsServices) {
        Assert.notEmpty(userDetailsServices, "userDetailsServices cannot be empty");
        this.userDetailsServices = userDetailsServices;
    }


}
