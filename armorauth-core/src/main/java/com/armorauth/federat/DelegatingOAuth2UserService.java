package com.armorauth.federat;

import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;

public class DelegatingOAuth2UserService<R extends OAuth2UserRequest, U extends OAuth2User> implements OAuth2UserService<R, U> {

    @Override
    public U loadUser(R userRequest) throws OAuth2AuthenticationException {
        return null;
    }


}