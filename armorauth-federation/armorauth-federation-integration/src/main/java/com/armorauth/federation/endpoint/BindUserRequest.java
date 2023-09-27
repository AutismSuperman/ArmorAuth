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
