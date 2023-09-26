package com.armorauth.federation.authentication;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

public class FederatedBindUserCheckProvider implements AuthenticationProvider {


    private final FederatedBindUserCheckService bindUserCheckService;

    public FederatedBindUserCheckProvider(FederatedBindUserCheckService bindUserCheckService) {
        this.bindUserCheckService = bindUserCheckService;
    }


    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        //check if the user is binded
        //1.需要绑定的话，就返回一个FederatedBindUserAuthenticationToken认证为false
        //1.不需要绑定的话，就返回一个FederatedBindUserAuthenticationToken认证为true
        FederatedLoginAuthenticationToken loginAuthenticationToken = (FederatedLoginAuthenticationToken) authentication;
        String userNameAttributeName = loginAuthenticationToken.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
        Boolean requireBindUser = bindUserCheckService.requireBindUser(
                loginAuthenticationToken.getPrincipal().getAttributes().get(userNameAttributeName).toString(),
                loginAuthenticationToken.getClientRegistration().getRegistrationId()
        );
        loginAuthenticationToken.setAuthenticated(requireBindUser);
        return loginAuthenticationToken;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return FederatedLoginAuthenticationToken.class.isAssignableFrom(authentication);
    }

}
