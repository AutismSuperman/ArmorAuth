package com.armorauth.federation.authentication;

import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.SpringSecurityCoreVersion;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Collections;

public class FederatedLoginAuthenticationToken  extends AbstractAuthenticationToken {


    private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

    private OAuth2User principal;

    private ClientRegistration clientRegistration;

    private OAuth2AuthorizationExchange authorizationExchange;

    private OAuth2AccessToken accessToken;

    private OAuth2RefreshToken refreshToken;

    /**
     * This constructor should be used when the Authorization Request/Response is
     * complete.
     * @param clientRegistration the client registration
     * @param authorizationExchange the authorization exchange
     */
    public FederatedLoginAuthenticationToken(ClientRegistration clientRegistration,
                                          OAuth2AuthorizationExchange authorizationExchange) {
        super(Collections.emptyList());
        Assert.notNull(clientRegistration, "clientRegistration cannot be null");
        Assert.notNull(authorizationExchange, "authorizationExchange cannot be null");
        this.clientRegistration = clientRegistration;
        this.authorizationExchange = authorizationExchange;
        this.setAuthenticated(false);
    }

    /**
     * This constructor should be used when the Access Token Request/Response is complete,
     * which indicates that the Authorization Code Grant flow has fully completed and
     * OAuth 2.0 Login has been achieved.
     * @param clientRegistration the client registration
     * @param authorizationExchange the authorization exchange
     * @param principal the user {@code Principal} registered with the OAuth 2.0 Provider
     * @param authorities the authorities granted to the user
     * @param accessToken the access token credential
     */
    public FederatedLoginAuthenticationToken(ClientRegistration clientRegistration,
                                          OAuth2AuthorizationExchange authorizationExchange, OAuth2User principal,
                                          Collection<? extends GrantedAuthority> authorities, OAuth2AccessToken accessToken) {
        this(clientRegistration, authorizationExchange, principal, authorities, accessToken, null);
    }

    /**
     * This constructor should be used when the Access Token Request/Response is complete,
     * which indicates that the Authorization Code Grant flow has fully completed and
     * OAuth 2.0 Login has been achieved.
     * @param clientRegistration the client registration
     * @param authorizationExchange the authorization exchange
     * @param principal the user {@code Principal} registered with the OAuth 2.0 Provider
     * @param authorities the authorities granted to the user
     * @param accessToken the access token credential
     * @param refreshToken the refresh token credential
     */
    public FederatedLoginAuthenticationToken(ClientRegistration clientRegistration,
                                          OAuth2AuthorizationExchange authorizationExchange, OAuth2User principal,
                                          Collection<? extends GrantedAuthority> authorities, OAuth2AccessToken accessToken,
                                          @Nullable OAuth2RefreshToken refreshToken) {
        super(authorities);
        Assert.notNull(clientRegistration, "clientRegistration cannot be null");
        Assert.notNull(authorizationExchange, "authorizationExchange cannot be null");
        Assert.notNull(principal, "principal cannot be null");
        Assert.notNull(accessToken, "accessToken cannot be null");
        this.clientRegistration = clientRegistration;
        this.authorizationExchange = authorizationExchange;
        this.principal = principal;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.setAuthenticated(true);
    }

    @Override
    public OAuth2User getPrincipal() {
        return this.principal;
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    /**
     * Returns the {@link ClientRegistration client registration}.
     * @return the {@link ClientRegistration}
     */
    public ClientRegistration getClientRegistration() {
        return this.clientRegistration;
    }

    /**
     * Returns the {@link OAuth2AuthorizationExchange authorization exchange}.
     * @return the {@link OAuth2AuthorizationExchange}
     */
    public OAuth2AuthorizationExchange getAuthorizationExchange() {
        return this.authorizationExchange;
    }

    /**
     * Returns the {@link OAuth2AccessToken access token}.
     * @return the {@link OAuth2AccessToken}
     */
    public OAuth2AccessToken getAccessToken() {
        return this.accessToken;
    }

    /**
     * Returns the {@link OAuth2RefreshToken refresh token}.
     * @return the {@link OAuth2RefreshToken}
     * @since 5.1
     */
    public @Nullable OAuth2RefreshToken getRefreshToken() {
        return this.refreshToken;
    }
}
