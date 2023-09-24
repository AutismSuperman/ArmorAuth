package com.armorauth.federation.endpoint;

import org.springframework.core.convert.converter.Converter;
import org.springframework.http.RequestEntity;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;

/**
 * 针对 OAuth2AccessTokenResponseClient 请求AccessToken的参数进行适配
 *
 * @author AutismSuperman
 * @see OAuth2AccessTokenResponseClient
 * @see DefaultAuthorizationCodeTokenResponseClient#setRequestEntityConverter(Converter)
 */
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
