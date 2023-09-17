package com.armorauth.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;

public class DeviceVerificationResponseHandler implements AuthenticationSuccessHandler {

    private final String activatedUri;

    public DeviceVerificationResponseHandler(String activatedUri) {
        this.activatedUri = activatedUri;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        response.sendRedirect(activatedUri);
    }
}
