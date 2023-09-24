package com.armorauth.federation.qq.endpoint;

import com.armorauth.federation.ExtendedOAuth2ClientProvider;
import com.armorauth.federation.endpoint.OAuth2AuthorizationCodeGrantRequestConverter;
import com.armorauth.federation.qq.QqParameterNames;
import org.springframework.http.RequestEntity;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

public class QqAuthorizationCodeGrantRequestConverter implements OAuth2AuthorizationCodeGrantRequestConverter {

    @Override
    public RequestEntity<?> convert(OAuth2AuthorizationCodeGrantRequest request) {
        ClientRegistration clientRegistration = request.getClientRegistration();
        OAuth2AuthorizationExchange authorizationExchange = request.getAuthorizationExchange();
        MultiValueMap<String, String> queryParameters = new LinkedMultiValueMap<>();
        queryParameters.add(OAuth2ParameterNames.GRANT_TYPE, request.getGrantType().getValue());
        queryParameters.add(OAuth2ParameterNames.CLIENT_ID, clientRegistration.getClientId());
        queryParameters.add(OAuth2ParameterNames.CLIENT_SECRET, clientRegistration.getClientSecret());
        queryParameters.add(OAuth2ParameterNames.CODE, authorizationExchange.getAuthorizationResponse().getCode());
        queryParameters.add(OAuth2ParameterNames.REDIRECT_URI, authorizationExchange.getAuthorizationRequest().getRedirectUri());
        queryParameters.add(QqParameterNames.FMT, QqParameterNames.FMT_JOSN);
        // 1 is response openid
        queryParameters.add(QqParameterNames.NEED_OPENID, "1");
        String tokenUri = clientRegistration.getProviderDetails().getTokenUri();
        URI uri = UriComponentsBuilder.fromUriString(tokenUri).queryParams(queryParameters).build().toUri();
        return RequestEntity.get(uri).build();
    }


    @Override
    public boolean supports(String registrationId) {
        return ExtendedOAuth2ClientProvider.matchNameLowerCase(ExtendedOAuth2ClientProvider.QQ, registrationId);
    }
}
