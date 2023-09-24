package com.armorauth.federation.web.converter;

import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.function.Consumer;

/**
 * 针对 OAuth2AuthorizationRequestResolver 处理结果进行第三方适配转换
 * @author AutismSuperman
 * @see OAuth2AuthorizationRequestResolver
 * @see DefaultOAuth2AuthorizationRequestResolver#setAuthorizationRequestCustomizer(Consumer)
 */
public interface OAuth2AuthorizationRequestConverter {

    /**
     * OAuth2AuthorizationRequest.Builder convert
     *
     * @param builder
     */
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