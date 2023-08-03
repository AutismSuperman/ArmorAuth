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
package com.armorauth.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class TestController {

    private final OAuth2AuthorizedClientManager authorizedClientManager;

    public TestController(OAuth2AuthorizedClientManager authorizedClientManager) {
        this.authorizedClientManager = authorizedClientManager;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }


    @GetMapping("/basic")
    public String basic(Model model,
                         Authentication authentication,
                         HttpServletRequest servletRequest,
                         HttpServletResponse servletResponse) {
        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId("autism-client-client-credentials")
                .principal(authentication)
                .attributes(attrs -> {
                    attrs.put(HttpServletRequest.class.getName(), servletRequest);
                    attrs.put(HttpServletResponse.class.getName(), servletResponse);
                })
                .build();
        OAuth2AuthorizedClient authorizedClient = this.authorizedClientManager.authorize(authorizeRequest);
        assert authorizedClient != null;
        OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
        model.addAttribute("accessToken", accessToken.getTokenValue());
        model.addAttribute("issuedAt", accessToken.getIssuedAt());
        model.addAttribute("expiresAt", accessToken.getExpiresAt());
        model.addAttribute("scopes",
                StringUtils.collectionToCommaDelimitedString(authorizedClient.getClientRegistration().getScopes()));
        return "index";
    }


    @GetMapping("/jwt")
    public String jwt(Model model,
                         Authentication authentication,
                         HttpServletRequest servletRequest,
                         HttpServletResponse servletResponse) {
        OAuth2AuthorizeRequest authorizeRequest =
                OAuth2AuthorizeRequest.withClientRegistrationId("silent-client-client-credentials-jwt")
                        .principal(authentication)
                        .attributes(attrs -> {
                            attrs.put(HttpServletRequest.class.getName(), servletRequest);
                            attrs.put(HttpServletResponse.class.getName(), servletResponse);
                        })
                        .build();
        OAuth2AuthorizedClient authorizedClient = this.authorizedClientManager.authorize(authorizeRequest);
        assert authorizedClient != null;
        OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
        model.addAttribute("accessToken", accessToken.getTokenValue());
        model.addAttribute("issuedAt", accessToken.getIssuedAt());
        model.addAttribute("expiresAt", accessToken.getExpiresAt());
        model.addAttribute("scopes",
                StringUtils.collectionToCommaDelimitedString(authorizedClient.getClientRegistration().getScopes()));
        return "index";
    }


}
