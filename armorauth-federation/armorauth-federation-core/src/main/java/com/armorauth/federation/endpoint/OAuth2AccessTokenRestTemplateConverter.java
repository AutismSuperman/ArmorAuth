package com.armorauth.federation.endpoint;

import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

/**
 * 针对 请求AccessToken进行RestTemplate的定制化
 *
 * @author AutismSuperman
 * @see OAuth2AccessTokenResponseClient
 * @see DefaultAuthorizationCodeTokenResponseClient#setRestOperations(RestOperations)
 */
public interface OAuth2AccessTokenRestTemplateConverter {


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
