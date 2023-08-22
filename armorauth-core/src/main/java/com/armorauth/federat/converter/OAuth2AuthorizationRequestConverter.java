package com.armorauth.federat.converter;


import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;


public interface OAuth2AuthorizationRequestConverter {


    void convert(OAuth2AuthorizationRequest.Builder builder);


    /**
     * Returns <code>true</code> if this <Code>OAuth2AuthorizationCodeGrantRequestConverter</code> supports the
     * indicated <Code>ClientRegistration</code>.
     *
     * @param registrationId the registration identifier
     * @return <code>true</code> if the implementation can more closely evaluate the
     * <code>ClientRegistration</code>
     */
    boolean supports(String registrationId);


}
