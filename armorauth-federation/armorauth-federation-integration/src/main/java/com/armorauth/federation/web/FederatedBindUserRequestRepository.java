package com.armorauth.federation.web;

import com.armorauth.federation.endpoint.FederatedBindUserRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface FederatedBindUserRequestRepository<T extends FederatedBindUserRequest> {

    T loadBindUserRequest(HttpServletRequest request);

    void saveBindUserRequest(T authorizationRequest, HttpServletRequest request, HttpServletResponse response);

    T removeBindUserRequest(HttpServletRequest request, HttpServletResponse response);


}
