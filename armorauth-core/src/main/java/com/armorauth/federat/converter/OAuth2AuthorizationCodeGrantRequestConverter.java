package com.armorauth.federat.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.http.RequestEntity;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;

public interface OAuth2AuthorizationCodeGrantRequestConverter extends Converter<OAuth2AuthorizationCodeGrantRequest, RequestEntity<?>> {


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
