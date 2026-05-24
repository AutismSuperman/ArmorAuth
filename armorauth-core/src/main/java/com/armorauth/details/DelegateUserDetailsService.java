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
import com.armorauth.data.repository.UserRoleRepository;
import com.armorauth.security.LoginLockoutService;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.Assert;

import java.util.LinkedList;
import java.util.List;

public class DelegateUserDetailsService implements UserDetailsService {

    private List<UserDetailsService> userDetailsServices;
    private LoginLockoutService loginLockoutService;

    public DelegateUserDetailsService(UserInfoRepository userInfoRepository,
                                       UserRoleRepository userRoleRepository) {
        this.userDetailsServices = new LinkedList<>();
        userDetailsServices.add(new JdbcCaptchaUserDetailsManager(userInfoRepository, userRoleRepository)::loadUserByAccount);
        userDetailsServices.add(new JdbcOAuth2UserDetailsManager(userInfoRepository, userRoleRepository)::loadOAuth2UserByUsername);
    }

    public void setLoginLockoutService(LoginLockoutService loginLockoutService) {
        this.loginLockoutService = loginLockoutService;
    }


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Assert.notNull(username, "username cannot be null");
        if (loginLockoutService != null && loginLockoutService.isLocked(username)) {
            throw new LockedException("账号已被锁定，请30分钟后重试");
        }
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
