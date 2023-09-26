package com.armorauth.federation.web;

import com.armorauth.federation.endpoint.FederatedBindUserRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.util.Assert;

public final class HttpSessionFederatedBindUserRequestRepository
        implements FederatedBindUserRequestRepository<FederatedBindUserRequest> {

    private static final String DEFAULT_AUTHORIZATION_REQUEST_ATTR_NAME = HttpSessionFederatedBindUserRequestRepository.class
            .getName() + ".BIND_USER_REQUEST";

    private final String sessionAttributeName = DEFAULT_AUTHORIZATION_REQUEST_ATTR_NAME;

    @Override
    public FederatedBindUserRequest loadBindUserRequest(HttpServletRequest request) {
        Assert.notNull(request, "request cannot be null");
        return getAuthorizationRequest(request);
    }

    @Override
    public void saveBindUserRequest(FederatedBindUserRequest bindUserRequest, HttpServletRequest request,
                                         HttpServletResponse response) {
        Assert.notNull(request, "request cannot be null");
        Assert.notNull(response, "response cannot be null");
        if (bindUserRequest == null) {
            removeBindUserRequest(request, response);
            return;
        }
        request.getSession().setAttribute(this.sessionAttributeName, bindUserRequest);
    }

    @Override
    public FederatedBindUserRequest removeBindUserRequest(HttpServletRequest request,
                                                                 HttpServletResponse response) {
        Assert.notNull(response, "response cannot be null");
        FederatedBindUserRequest authorizationRequest = loadBindUserRequest(request);
        if (authorizationRequest != null) {
            request.getSession().removeAttribute(this.sessionAttributeName);
        }
        return authorizationRequest;
    }


    private FederatedBindUserRequest getAuthorizationRequest(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return (session != null) ? (FederatedBindUserRequest) session.getAttribute(this.sessionAttributeName) : null;
    }

}