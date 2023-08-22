package com.armorauth.federat.converter;

import com.armorauth.federat.ExtendedOAuth2ClientProvider;
import com.armorauth.federat.wechat.Wechat2AuthorizationRequestConverter;
import com.armorauth.federat.wechat.WechatAuthorizationCodeGrantRequestConverter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.http.RequestEntity;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;

@Slf4j
public class WechatAuthorizationCodeGrantRequestConverterTest {


    @Test
    public void testConvert() {
        Wechat2AuthorizationRequestConverter wechatAuthorizationRequestService = new Wechat2AuthorizationRequestConverter();
        OAuth2AuthorizationRequest.Builder builder = OAuth2AuthorizationRequest.authorizationCode();
        builder.clientId("APP_ID");
        builder.authorizationUri("https://open.weixin.qq.com/connect/qrconnect");
        builder.redirectUri("REDIRECT_URI");
        builder.scope("snsapi_userinfo");
        builder.state("STATE");
        wechatAuthorizationRequestService.convert(builder);
        OAuth2AuthorizationRequest request = builder.build();
        OAuth2AuthorizationResponse.Builder responseBuilder = OAuth2AuthorizationResponse.success("CODE");
        responseBuilder.state("STATE");
        responseBuilder.redirectUri("REDIRECT_URI");
        OAuth2AuthorizationResponse response = responseBuilder.build();
        OAuth2AuthorizationExchange oAuth2AuthorizationExchange = new OAuth2AuthorizationExchange(
                request, response
        );
        ClientRegistration.Builder wechat = ExtendedOAuth2ClientProvider.WECHAT.getBuilder("wechat");
        wechat.clientId("APP_ID");
        wechat.clientSecret("SECRET");
        wechat.authorizationGrantType(new AuthorizationGrantType("authorization_code"));
        ClientRegistration clientRegistration = wechat.build();

        OAuth2AuthorizationCodeGrantRequest codeGrantRequest = new OAuth2AuthorizationCodeGrantRequest(
                clientRegistration,
                oAuth2AuthorizationExchange
        );
        WechatAuthorizationCodeGrantRequestConverter codeGrantRequestConverter = new WechatAuthorizationCodeGrantRequestConverter();
        RequestEntity<?> convert = codeGrantRequestConverter.convert(codeGrantRequest);
        assert convert != null;
        log.info(convert.toString());
    }


}
