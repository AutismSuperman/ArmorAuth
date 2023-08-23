package com.armorauth.federat.converter;

import com.armorauth.federat.wechat.WechatAuthorizationRequestConverter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

@Slf4j
public class WechatAuthorizationRequestConverterTest {

    @Test
    public void testConvert() {
        WechatAuthorizationRequestConverter wechatAuthorizationRequestService = new WechatAuthorizationRequestConverter();
        OAuth2AuthorizationRequest.Builder builder = OAuth2AuthorizationRequest.authorizationCode();
        builder.clientId("APP_ID");
        builder.authorizationUri("https://open.weixin.qq.com/connect/qrconnect");
        builder.redirectUri("REDIRECT_URI");
        builder.scope("snsapi_userinfo");
        builder.state("STATE");
        wechatAuthorizationRequestService.convert(builder);
        OAuth2AuthorizationRequest request = builder.build();
        log.info(request.getAuthorizationRequestUri());
    }
}
