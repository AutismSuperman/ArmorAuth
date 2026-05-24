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
package com.armorauth.authentication;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.SpringSecurityCoreVersion;

import java.io.Serial;
import java.util.Collection;
import java.util.Collections;

/**
 * MFA 认证令牌
 *
 * @author fulin
 * @since 2026-05-23
 */
public class MfaAuthenticationToken extends AbstractAuthenticationToken {

    @Serial
    private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

    private final Object principal;
    private String mfaCode;
    private final String factorId;

    public MfaAuthenticationToken(Object principal, String mfaCode, String factorId) {
        super(Collections.emptyList());
        this.principal = principal;
        this.mfaCode = mfaCode;
        this.factorId = factorId;
        setAuthenticated(false);
    }

    public MfaAuthenticationToken(Object principal, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.mfaCode = null;
        this.factorId = null;
        super.setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return this.mfaCode;
    }

    @Override
    public Object getPrincipal() {
        return this.principal;
    }

    public String getFactorId() {
        return this.factorId;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        if (isAuthenticated) {
            throw new IllegalArgumentException(
                    "Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead");
        }
        super.setAuthenticated(false);
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        mfaCode = null;
    }
}
