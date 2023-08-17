package com.armorauth.federat.converter;

import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.core.convert.converter.Converter;

import java.util.Map;

public class DelegateMapOAuth2AccessTokenResponseConverter implements Converter<Map<String, Object>, OAuth2AccessTokenResponse> {

    @Override
    public OAuth2AccessTokenResponse convert(Map<String, Object> source) {
        return null;
    }
}
