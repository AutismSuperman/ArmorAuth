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
package com.armorauth.samples.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Controller
public class OAuth2AuthorizeRequestController {

    private final static String PATTERN_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private final static DateTimeFormatter formatter = DateTimeFormatter.ofPattern(PATTERN_FORMAT).withZone(ZoneId.systemDefault());

    private final OAuth2AuthorizedClientManager authorizedClientManager;

    public OAuth2AuthorizeRequestController(OAuth2AuthorizedClientManager authorizedClientManager) {
        this.authorizedClientManager = authorizedClientManager;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }


    @GetMapping("/client_secret_basic")
    public String clientSecretBasic(Model model,
                                     Authentication authentication,
                                     HttpServletRequest servletRequest,
                                     HttpServletResponse servletResponse) {
        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId("autism-client-client-credentials")
                .principal(authentication)
                .attributes(attrs -> {
                    attrs.put(HttpServletRequest.class.getName(), servletRequest);
                    attrs.put(HttpServletResponse.class.getName(), servletResponse);
                })
                .build();
        OAuth2AuthorizedClient authorizedClient = this.authorizedClientManager.authorize(authorizeRequest);
        Assert.notNull(authorizedClient, "authorizedClient is null");
        OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
        model.addAttribute(OAuth2ParameterNames.ACCESS_TOKEN, accessToken.getTokenValue());
        model.addAttribute(OAuth2ParameterNames.SCOPE,StringUtils.collectionToCommaDelimitedString(authorizedClient.getClientRegistration().getScopes()));
        Assert.notNull(accessToken.getIssuedAt(), "accessToken.getIssuedAt() is null");
        model.addAttribute("issuedAt", formatter.format(accessToken.getIssuedAt()));
        Assert.notNull(accessToken.getExpiresAt(), "accessToken.getExpiresAt() is null");
        model.addAttribute("expiresAt", formatter.format(accessToken.getExpiresAt()));
        return "index::ul";
    }


    @GetMapping("/client_secret_jwt")
    public String clientSecretJwt(Model model,
                      Authentication authentication,
                      HttpServletRequest servletRequest,
                      HttpServletResponse servletResponse) {
        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId("silent-client-client-credentials-jwt")
                .principal(authentication)
                .attributes(attrs -> {
                    attrs.put(HttpServletRequest.class.getName(), servletRequest);
                    attrs.put(HttpServletResponse.class.getName(), servletResponse);
                })
                .build();
        OAuth2AuthorizedClient authorizedClient = this.authorizedClientManager.authorize(authorizeRequest);
        Assert.notNull(authorizedClient, "authorizedClient is null");
        OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
        model.addAttribute(OAuth2ParameterNames.ACCESS_TOKEN, accessToken.getTokenValue());
        model.addAttribute(OAuth2ParameterNames.SCOPE,StringUtils.collectionToCommaDelimitedString(authorizedClient.getClientRegistration().getScopes()));
        Assert.notNull(accessToken.getIssuedAt(), "accessToken.getIssuedAt() is null");
        model.addAttribute("issuedAt", formatter.format(accessToken.getIssuedAt()));
        Assert.notNull(accessToken.getExpiresAt(), "accessToken.getExpiresAt() is null");
        model.addAttribute("expiresAt", formatter.format(accessToken.getExpiresAt()));
        return "index::ul";
    }


    @GetMapping("/private_key_jwt")
    public String privateKeyJwt(Model model,
                      Authentication authentication,
                      HttpServletRequest servletRequest,
                      HttpServletResponse servletResponse) {
        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId("quietly-client-client-credentials-jwt-private-key")
                .principal(authentication)
                .attributes(attrs -> {
                    attrs.put(HttpServletRequest.class.getName(), servletRequest);
                    attrs.put(HttpServletResponse.class.getName(), servletResponse);
                })
                .build();
        OAuth2AuthorizedClient authorizedClient = this.authorizedClientManager.authorize(authorizeRequest);
        Assert.notNull(authorizedClient, "authorizedClient is null");
        OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
        model.addAttribute(OAuth2ParameterNames.ACCESS_TOKEN, accessToken.getTokenValue());
        model.addAttribute(OAuth2ParameterNames.SCOPE,StringUtils.collectionToCommaDelimitedString(authorizedClient.getClientRegistration().getScopes()));
        Assert.notNull(accessToken.getIssuedAt(), "accessToken.getIssuedAt() is null");
        model.addAttribute("issuedAt", formatter.format(accessToken.getIssuedAt()));
        Assert.notNull(accessToken.getExpiresAt(), "accessToken.getExpiresAt() is null");
        model.addAttribute("expiresAt", formatter.format(accessToken.getExpiresAt()));
        return "index::ul";
    }


}
