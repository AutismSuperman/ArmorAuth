package com.armorauth.federat;

import com.armorauth.federat.converter.OAuth2AuthorizationCodeGrantRequestConverter;
import com.armorauth.federat.wechat.WechatAuthorizationCodeGrantRequestConverter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.RequestEntity;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequestEntityConverter;
import org.springframework.security.oauth2.client.registration.ClientRegistration;

import java.util.ArrayList;
import java.util.List;

public class DelegatingOAuth2AuthorizationCodeGrantRequestEntityConverter implements Converter<OAuth2AuthorizationCodeGrantRequest, RequestEntity<?>> {

    private final OAuth2AuthorizationCodeGrantRequestEntityConverter defaultConverter = new OAuth2AuthorizationCodeGrantRequestEntityConverter();

    private final List<OAuth2AuthorizationCodeGrantRequestConverter> authorizationCodeGrantRequestConverters;


    public DelegatingOAuth2AuthorizationCodeGrantRequestEntityConverter() {
        this.authorizationCodeGrantRequestConverters = new ArrayList<>();
        authorizationCodeGrantRequestConverters.add(new WechatAuthorizationCodeGrantRequestConverter());
    }


    @Override
    public RequestEntity<?> convert(OAuth2AuthorizationCodeGrantRequest request) {
        ClientRegistration clientRegistration = request.getClientRegistration();
        String registrationId = clientRegistration.getRegistrationId();
        for (OAuth2AuthorizationCodeGrantRequestConverter converter : authorizationCodeGrantRequestConverters) {
            if (converter.supports(registrationId)) {
                return converter.convert(request);
            }
        }
        return defaultConverter.convert(request);
    }

   public  void addAuthorizationCodeGrantRequestConverter(OAuth2AuthorizationCodeGrantRequestConverter auth2AuthorizationCodeGrantRequestConverter){
        authorizationCodeGrantRequestConverters.add(auth2AuthorizationCodeGrantRequestConverter);
   }



}
