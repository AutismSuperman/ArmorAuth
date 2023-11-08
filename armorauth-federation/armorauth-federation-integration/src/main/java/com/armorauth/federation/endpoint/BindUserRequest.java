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
package com.armorauth.federation.endpoint;

import org.springframework.security.core.SpringSecurityCoreVersion;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.Serial;
import java.io.Serializable;

public class BindUserRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

    private final OAuth2User principal;

    private final String registrationId;

    // save session space
    private final String userNameAttributeName;

    public BindUserRequest(OAuth2User principal, String registrationId, String userNameAttributeName) {
        this.principal = principal;
        this.registrationId = registrationId;
        this.userNameAttributeName = userNameAttributeName;
    }

    public OAuth2User getPrincipal() {
        return principal;
    }

    public String getRegistrationId() {
        return registrationId;
    }

    public String getUserNameAttributeName() {
        return userNameAttributeName;
    }
}
