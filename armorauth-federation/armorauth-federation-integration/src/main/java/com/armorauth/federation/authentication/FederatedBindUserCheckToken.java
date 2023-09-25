package com.armorauth.federation.authentication;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.util.Assert;

import java.util.Collection;

public class FederatedBindUserCheckToken extends AbstractAuthenticationToken {

    private final OAuth2User principal;

    private final String authorizedClientRegistrationId;


    public FederatedBindUserCheckToken(OAuth2User principal, Collection<? extends GrantedAuthority> authorities,
                                       String authorizedClientRegistrationId) {
        super(authorities);
        Assert.notNull(principal, "principal cannot be null");
        Assert.hasText(authorizedClientRegistrationId, "authorizedClientRegistrationId cannot be empty");
        this.principal = principal;
        this.authorizedClientRegistrationId = authorizedClientRegistrationId;
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

    /**
     * Returns the registration identifier of the {@link OAuth2AuthorizedClient Authorized
     * Client}.
     * @return the registration identifier of the Authorized Client.
     */
    public String getAuthorizedClientRegistrationId() {
        return this.authorizedClientRegistrationId;
    }



}
