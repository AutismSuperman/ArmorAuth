package com.armorauth.federat.wechat;

import com.armorauth.federat.converter.OAuth2AuthorizationRequestConverter;
import com.armorauth.federat.ExtendedOAuth2ClientProvider;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;

import java.util.LinkedHashMap;

public class Wechat2AuthorizationRequestConverter implements OAuth2AuthorizationRequestConverter {


    /**
     * 默认情况下Spring Security会生成授权链接：
     * {@code
     * https://open.weixin.qq.com/connect/qrconnect?
     * response_type=code&
     * client_id=CLIENT_ID&
     * scope=snsapi_userinfo&
     * state=STATE&
     * redirect_uri=REDIRECT_URI
     * }
     * 微信协议 {@code #wechat_redirect}，同时 {@code client_id}应该替换为{@code app_id}
     * {@code
     * https://open.weixin.qq.com/connect/qrconnect?
     * response_type=code&
     * appid=APPID&
     * scope=snsapi_userinfo&
     * state=STATE
     * redirect_uri=REDIRECT_URI&
     * #wechat_redirect
     * }
     *
     * @param builder the OAuth2AuthorizationRequest.builder
     */

    @Override
    public void convert(OAuth2AuthorizationRequest.Builder builder) {
        builder.attributes(attributes ->
                builder.parameters(parameters -> {
                    LinkedHashMap<String, Object> linkedParameters = new LinkedHashMap<>();
                    parameters.forEach((key, value) -> {
                        if (OAuth2ParameterNames.CLIENT_ID.equals(key)) {
                            linkedParameters.put(WechatParameterNames.APP_ID, value);
                        } else {
                            linkedParameters.put(key, value);
                        }
                    });
                    parameters.clear();
                    parameters.putAll(linkedParameters);
                    builder.authorizationRequestUri(uriBuilder ->
                            uriBuilder.fragment(WechatParameterNames.WECHAT_REDIRECT).build()
                    );
                })
        );
    }

    @Override
    public boolean supports(String registrationId) {
        return ExtendedOAuth2ClientProvider.matchNameLowerCase(ExtendedOAuth2ClientProvider.WECHAT, registrationId);
    }

}
