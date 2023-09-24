/*
 * Copyright (c) 2023-present ArmorAuth. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.armorauth.federation.wechat;

import com.armorauth.federation.converter.OAuth2AuthorizationRequestConverter;
import com.armorauth.federation.ExtendedOAuth2ClientProvider;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;

import java.util.LinkedHashMap;

public class WechatAuthorizationRequestConverter implements OAuth2AuthorizationRequestConverter {


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
