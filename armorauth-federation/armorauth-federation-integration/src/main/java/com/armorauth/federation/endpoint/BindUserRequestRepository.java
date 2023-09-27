package com.armorauth.federation.endpoint;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface BindUserRequestRepository<T extends BindUserRequest> {

    T loadBindUserRequest(HttpServletRequest request);

    void saveBindUserRequest(T authorizationRequest, HttpServletRequest request, HttpServletResponse response);

    T removeBindUserRequest(HttpServletRequest request, HttpServletResponse response);


}
