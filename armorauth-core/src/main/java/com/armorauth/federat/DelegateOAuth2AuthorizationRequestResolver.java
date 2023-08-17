package com.armorauth.federat;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

public class DelegateOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {
    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return null;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        return null;
    }
}
