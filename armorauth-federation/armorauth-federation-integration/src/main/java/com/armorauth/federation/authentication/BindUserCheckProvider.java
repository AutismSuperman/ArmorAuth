package com.armorauth.federation.authentication;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

public class BindUserCheckProvider implements AuthenticationProvider {


    private final BindUserCheckService bindUserCheckService;

    public BindUserCheckProvider(BindUserCheckService bindUserCheckService) {
        this.bindUserCheckService = bindUserCheckService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        //check if the user is bind
        //1.需要绑定的话，就返回一个FederatedBindUserAuthenticationToken认证为false
        //1.不需要绑定的话，就返回一个FederatedBindUserAuthenticationToken认证为true
        BindUserCheckToken bindUserCheckToken = (BindUserCheckToken) authentication;
        String userNameAttributeName = bindUserCheckToken.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
        Boolean requireBindUser = bindUserCheckService.requireBindUser(
                bindUserCheckToken.getPrincipal().getAttributes().get(userNameAttributeName).toString(),
                bindUserCheckToken.getClientRegistration().getRegistrationId()
        );
        bindUserCheckToken.setAuthenticated(requireBindUser);
        return bindUserCheckToken;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return BindUserCheckToken.class.isAssignableFrom(authentication);
    }

}
