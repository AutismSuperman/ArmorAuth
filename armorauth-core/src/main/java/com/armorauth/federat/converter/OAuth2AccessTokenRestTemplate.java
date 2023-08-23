package com.armorauth.federat.converter;

import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.web.client.RestTemplate;

public interface OAuth2AccessTokenRestTemplate {


    RestTemplate getRestTemplate(OAuth2AuthorizationCodeGrantRequest authorizationGrantRequest);


    /**
     * Returns <code>true</code> if this <Code>OAuth2AccessTokenRestTemplate</code> supports the
     * indicated <Code>ClientRegistration</code>.
     *
     * @param registrationId the registration identifier
     * @return <code>true</code> if the implementation can more closely evaluate the
     * <code>ClientRegistration</code>
     */
    boolean supports(String registrationId);

}
