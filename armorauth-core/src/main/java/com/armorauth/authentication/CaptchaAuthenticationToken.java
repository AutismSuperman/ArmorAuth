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
 * @author fulin
 */
public class CaptchaAuthenticationToken extends AbstractAuthenticationToken {

    @Serial
    private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

    private final Object principal;
    private String captcha;
    private String captchaId;

    /**
     * 此构造函数用来初始化未授信凭据.
     *
     * @param principal the principal
     * @param captcha   the captcha
     */
    public CaptchaAuthenticationToken(Object principal, String captcha) {
        this(principal, captcha, (String) null);
    }

    /**
     * 此构造函数用来初始化带验证码标识的未授信凭据.
     *
     * @param principal the principal
     * @param captcha   the captcha
     * @param captchaId the captcha id
     */
    public CaptchaAuthenticationToken(Object principal, String captcha, String captchaId) {
        super(Collections.emptyList());
        this.principal = principal;
        this.captcha = captcha;
        this.captchaId = captchaId;
        setAuthenticated(false);
    }

    /**
     * 此构造函数用来初始化授信凭据.
     *
     * @param principal   the principal
     * @param captcha     the captcha
     * @param authorities the authorities
     */
    public CaptchaAuthenticationToken(Object principal, String captcha,
                                      Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.captcha = captcha;
        // must use super, as we override
        super.setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return this.captcha;
    }

    public String getCaptchaId() {
        return this.captchaId;
    }

    @Override
    public Object getPrincipal() {
        return this.principal;
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
        captcha = null;
        captchaId = null;
    }
}
