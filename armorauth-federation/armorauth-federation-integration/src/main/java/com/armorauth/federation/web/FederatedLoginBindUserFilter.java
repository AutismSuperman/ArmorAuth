package com.armorauth.federation.web;

import com.armorauth.federation.endpoint.FederatedBindUserRequest;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;

import java.io.IOException;

public class FederatedLoginBindUserFilter  extends AbstractAuthenticationProcessingFilter {

    public static final String DEFAULT_FILTER_PROCESSES_URI = "/federated/bind";

    private FederatedBindUserRequestRepository<FederatedBindUserRequest> federatedBindUserRequestRepository = new HttpSessionFederatedBindUserRequestRepository();

    protected FederatedLoginBindUserFilter(String defaultFilterProcessesUrl) {
        super(DEFAULT_FILTER_PROCESSES_URI);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, IOException, ServletException {
        FederatedBindUserRequest federatedBindUserRequest = federatedBindUserRequestRepository.loadBindUserRequest(request);

        return null;
    }
}
