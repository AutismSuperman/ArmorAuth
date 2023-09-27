package com.armorauth.federation.authentication;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.util.Assert;

import java.util.Collection;

public class FederatedBindUserCheckToken extends AbstractAuthenticationToken {

    private final OAuth2User principal;

    private ClientRegistration clientRegistration;


    public FederatedBindUserCheckToken(OAuth2User principal, ClientRegistration clientRegistration) {
        super(null);
        Assert.notNull(principal, "principal cannot be null");
        Assert.notNull(clientRegistration, "clientRegistration cannot be null");
        this.principal = principal;
        this.clientRegistration = clientRegistration;
        this.setAuthenticated(false);
    }


    @Override
    public OAuth2User getPrincipal() {
        return principal;
    }

    @Override
    public Object getCredentials() {
        return principal;
    }

    public ClientRegistration getClientRegistration() {
        return clientRegistration;
    }
}
