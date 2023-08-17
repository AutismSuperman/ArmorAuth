package com.armorauth.federat.converter;

import org.springframework.http.RequestEntity;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.client.endpoint.OAuth2RefreshTokenGrantRequest;

public class DelegateOAuth2RefreshTokenRequestEntityConverter implements Converter<OAuth2RefreshTokenGrantRequest, RequestEntity<?>> {

    @Override
    public RequestEntity<?> convert(OAuth2RefreshTokenGrantRequest source) {
        return null;
    }
}
