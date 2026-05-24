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
package com.armorauth.common.audit;

import org.springframework.context.ApplicationEvent;

/**
 * 用户注册事件，由联合登录自动注册或管理员创建用户时发布
 *
 * @author fulin
 * @since 2026-05-23
 */
public class UserRegistrationEvent extends ApplicationEvent {

    private final String userId;
    private final String username;
    private final String source; // "self", "federated", "admin"

    public UserRegistrationEvent(Object source, String userId, String username, String registrationSource) {
        super(source);
        this.userId = userId;
        this.username = username;
        this.source = registrationSource;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getRegistrationSource() {
        return source;
    }
}
