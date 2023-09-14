package com.armorauth.security;

import com.armorauth.web.endpoint.ConsentSuccessResponse;
import com.armorauth.web.http.converter.ConsentSuccessResponseHttpMessageConverter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class OAuth2AuthorizationResponseHandler implements AuthenticationSuccessHandler {

    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();


    private ConsentSuccessResponseHttpMessageConverter consentSuccessConverter=new ConsentSuccessResponseHttpMessageConverter();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        if (request.getMethod().equals(HttpMethod.POST.name())) {
            // consent success  request is  post
            ConsentSuccessResponse consentSuccessResponse = new ConsentSuccessResponse();
            String redirectUri = getRedirectUri(authentication);
            consentSuccessResponse.setRedirectUri(redirectUri);
            ServletServerHttpResponse httpResponse = new ServletServerHttpResponse(response);
            consentSuccessConverter.write(consentSuccessResponse, null, httpResponse);
        } else {
            sendAuthorizationResponse(request, response, authentication);
        }
    }


    private void sendAuthorizationResponse(HttpServletRequest request, HttpServletResponse response,
                                           Authentication authentication) throws IOException {
        String redirectUri = getRedirectUri(authentication);
        this.redirectStrategy.sendRedirect(request, response, redirectUri);
    }

    private static String getRedirectUri(Authentication authentication) {
        OAuth2AuthorizationCodeRequestAuthenticationToken authorizationCodeRequestAuthentication =
                (OAuth2AuthorizationCodeRequestAuthenticationToken) authentication;
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromUriString(authorizationCodeRequestAuthentication.getRedirectUri())
                .queryParam(OAuth2ParameterNames.CODE, authorizationCodeRequestAuthentication.getAuthorizationCode().getTokenValue());
        if (StringUtils.hasText(authorizationCodeRequestAuthentication.getState())) {
            uriBuilder.queryParam(
                    OAuth2ParameterNames.STATE,
                    UriUtils.encode(authorizationCodeRequestAuthentication.getState(), StandardCharsets.UTF_8));
        }
        // build(true) -> Components are explicitly encoded
        return uriBuilder.build(true).toUriString();
    }

    public void setConsentSuccessConverter(ConsentSuccessResponseHttpMessageConverter consentSuccessConverter) {
        this.consentSuccessConverter = consentSuccessConverter;
    }
}
