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
package com.armorauth.security;

import com.armorauth.authentication.CaptchaAuthenticationToken;
import com.armorauth.web.endpoint.LoginSuccessResponse;
import com.armorauth.web.http.converter.LoginSuccessResponseHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.util.Assert;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * An {@link AuthenticationSuccessHandler} for capturing the {@link OidcUser} or
 * {@link OAuth2User} for Federated Account Linking or JIT Account Provisioning.
 *
 * @author Steve Riesenberg
 * @since 0.2.3
 */
public final class FederatedAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private RequestCache requestCache;
    private String redirect;
    private static final String defaultTargetUrl = "/";

    private final HttpMessageConverter<LoginSuccessResponse> loginSuccessHttpResponseConverter =
            new LoginSuccessResponseHttpMessageConverter();

    private AuthenticationSuccessHandler authenticationSuccessHandler = this::sendLoginSuccessResponse;

    private final AuthenticationSuccessHandler delegateAuthenticationSuccessHandler = new SavedRequestAwareAuthenticationSuccessHandler();

    private Consumer<OAuth2User> oauth2UserHandler = (user) -> this.oauth2UserHandler.accept(user);

    private Consumer<OidcUser> oidcUserHandler = (user) -> this.oidcUserHandler.accept(user);

    public FederatedAuthenticationSuccessHandler() {
        this(defaultTargetUrl, new HttpSessionRequestCache());
    }

    public FederatedAuthenticationSuccessHandler(String redirect, RequestCache requestCache) {
        Assert.notNull(requestCache, "requestCache must not be null");
        this.redirect = redirect;
        this.requestCache = requestCache;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        if (authentication instanceof OAuth2AuthenticationToken) {
            if (authentication.getPrincipal() instanceof OAuth2User) {
                //this.oidcUserHandler.accept((OidcUser) authentication.getPrincipal());
                this.delegateAuthenticationSuccessHandler.onAuthenticationSuccess(request, response, authentication);
            } else if (authentication.getPrincipal() instanceof OidcUser) {
                //this.oauth2UserHandler.accept((OAuth2User) authentication.getPrincipal());
                this.delegateAuthenticationSuccessHandler.onAuthenticationSuccess(request, response, authentication);
            } else {
                this.delegateAuthenticationSuccessHandler.onAuthenticationSuccess(request, response, authentication);
            }
        } else if (authentication instanceof UsernamePasswordAuthenticationToken) {
            this.authenticationSuccessHandler.onAuthenticationSuccess(request, response, authentication);
        } else if (authentication instanceof CaptchaAuthenticationToken) {
            this.authenticationSuccessHandler.onAuthenticationSuccess(request, response, authentication);
        } else {
            this.delegateAuthenticationSuccessHandler.onAuthenticationSuccess(request, response, authentication);
        }
    }

    private void sendLoginSuccessResponse(HttpServletRequest request, HttpServletResponse response,
                                          Authentication authentication) throws IOException {
        LoginSuccessResponse loginSuccessResponse = new LoginSuccessResponse();
        SavedRequest savedRequest = this.requestCache.getRequest(request, response);
        String redirectUrl = savedRequest == null ? this.redirect : savedRequest.getRedirectUrl();
        clearAuthenticationAttributes(request);
        loginSuccessResponse.setRedirectUri(redirectUrl);
        loginSuccessResponse.setDescription("登录成功");
        ServletServerHttpResponse httpResponse = new ServletServerHttpResponse(response);
        this.loginSuccessHttpResponseConverter.write(loginSuccessResponse, null, httpResponse);
    }

    private void clearAuthenticationAttributes(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        }
    }

    public void setOAuth2UserHandler(Consumer<OAuth2User> oauth2UserHandler) {
        this.oauth2UserHandler = oauth2UserHandler;
    }

    public void setOidcUserHandler(Consumer<OidcUser> oidcUserHandler) {
        this.oidcUserHandler = oidcUserHandler;
    }


    public void setAuthenticationSuccessHandler(AuthenticationSuccessHandler authenticationSuccessHandler) {
        Assert.notNull(authenticationSuccessHandler, "authenticationSuccessHandler cannot be null");
        this.authenticationSuccessHandler = authenticationSuccessHandler;
    }


    public void setRequestCache(RequestCache requestCache) {
        this.requestCache = requestCache;
    }


}
