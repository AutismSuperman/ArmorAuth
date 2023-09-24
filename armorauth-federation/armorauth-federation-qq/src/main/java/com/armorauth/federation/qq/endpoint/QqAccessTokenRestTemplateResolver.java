package com.armorauth.federation.qq.endpoint;

import com.armorauth.federation.ExtendedOAuth2ClientProvider;
import com.armorauth.federation.endpoint.OAuth2AccessTokenRestTemplateResolver;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.DefaultMapOAuth2AccessTokenResponseConverter;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Map;

public class QqAccessTokenRestTemplateResolver implements OAuth2AccessTokenRestTemplateResolver {
    @Override
    public RestTemplate getRestTemplate(OAuth2AuthorizationCodeGrantRequest authorizationGrantRequest) {
        OAuth2AccessTokenResponseHttpMessageConverter tokenResponseHttpMessageConverter =
                new OAuth2AccessTokenResponseHttpMessageConverter();
        // QQ返回的 Content-type 是 text-html
        tokenResponseHttpMessageConverter.setSupportedMediaTypes(Arrays.asList(
                MediaType.APPLICATION_JSON,
                MediaType.TEXT_HTML,
                MediaType.TEXT_PLAIN,
                new MediaType("application", "*+json"))
        );
        tokenResponseHttpMessageConverter.setAccessTokenResponseConverter(responseParameters -> {
            // 解决QQ没有返回 token_type 导致的空校验异常
            Converter<Map<String, Object>, OAuth2AccessTokenResponse> delegate = new DefaultMapOAuth2AccessTokenResponseConverter();
            responseParameters.put(OAuth2ParameterNames.TOKEN_TYPE, OAuth2AccessToken.TokenType.BEARER.getValue());
            responseParameters.put(OAuth2ParameterNames.SCOPE, StringUtils.collectionToCommaDelimitedString(authorizationGrantRequest.getClientRegistration().getScopes()));
            return delegate.convert(responseParameters);
        });
        RestTemplate restTemplate = new RestTemplate(Arrays.asList(new FormHttpMessageConverter(), tokenResponseHttpMessageConverter));
        restTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());
        return restTemplate;
    }

    @Override
    public boolean supports(String registrationId) {
        return ExtendedOAuth2ClientProvider.matchNameLowerCase(ExtendedOAuth2ClientProvider.QQ, registrationId);
    }
}
