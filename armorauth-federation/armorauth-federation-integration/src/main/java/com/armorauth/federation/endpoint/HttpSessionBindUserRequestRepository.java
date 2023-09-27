package com.armorauth.federation.endpoint;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.util.Assert;

public final class HttpSessionBindUserRequestRepository
        implements BindUserRequestRepository<BindUserRequest> {

    private static final String DEFAULT_AUTHORIZATION_REQUEST_ATTR_NAME = HttpSessionBindUserRequestRepository.class
            .getName() + ".BIND_USER_REQUEST";

    private final String sessionAttributeName = DEFAULT_AUTHORIZATION_REQUEST_ATTR_NAME;

    @Override
    public BindUserRequest loadBindUserRequest(HttpServletRequest request) {
        Assert.notNull(request, "request cannot be null");
        return getAuthorizationRequest(request);
    }

    @Override
    public void saveBindUserRequest(BindUserRequest bindUserRequest, HttpServletRequest request,
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
    public BindUserRequest removeBindUserRequest(HttpServletRequest request,
                                                 HttpServletResponse response) {
        Assert.notNull(response, "response cannot be null");
        BindUserRequest authorizationRequest = loadBindUserRequest(request);
        if (authorizationRequest != null) {
            request.getSession().removeAttribute(this.sessionAttributeName);
        }
        return authorizationRequest;
    }


    private BindUserRequest getAuthorizationRequest(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return (session != null) ? (BindUserRequest) session.getAttribute(this.sessionAttributeName) : null;
    }

}