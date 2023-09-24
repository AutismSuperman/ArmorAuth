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
package com.armorauth.federation.converter;

import com.armorauth.federation.wechat.WechatAuthorizationRequestConverter;
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
