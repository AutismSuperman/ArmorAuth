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