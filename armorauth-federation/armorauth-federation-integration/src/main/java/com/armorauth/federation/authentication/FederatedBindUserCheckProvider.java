package com.armorauth.federation.authentication;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

public class FederatedBindUserCheckProvider implements AuthenticationProvider {


    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        //TODO: check if the user is binded
        //1.需要绑定的话，就返回一个FederatedBindUserAuthenticationToken认证为false
        //1.不需要绑定的话，就返回一个FederatedBindUserAuthenticationToken认证为true
        return null;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return FederatedLoginAuthenticationToken.class.isAssignableFrom(authentication);
    }

}
