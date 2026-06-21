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
package com.armorauth.authorization.tenant;

import com.armorauth.data.entity.Tenant;
import com.armorauth.data.repository.TenantRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class TenantIssuerFilter extends OncePerRequestFilter {

    private static final String TENANT_ISSUER_PREFIX = "/t/";

    private final TenantRepository tenantRepository;
    private final boolean multipleIssuersEnabled;

    public TenantIssuerFilter(TenantRepository tenantRepository, boolean multipleIssuersEnabled) {
        this.tenantRepository = tenantRepository;
        this.multipleIssuersEnabled = multipleIssuersEnabled;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }

        if (!path.startsWith(TENANT_ISSUER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!multipleIssuersEnabled) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String tenantCode = resolveTenantCode(path);
        if (tenantCode == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Tenant tenant = tenantRepository.findByTenantCodeAndEnabled(tenantCode, true).orElse(null);
        if (tenant == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try {
            TenantIssuerContext.set(tenant.getId(), tenant.getTenantCode());
            filterChain.doFilter(request, response);
        } finally {
            TenantIssuerContext.clear();
        }
    }

    private String resolveTenantCode(String path) {
        String remainder = path.substring(TENANT_ISSUER_PREFIX.length());
        int separator = remainder.indexOf('/');
        String tenantCode = separator >= 0 ? remainder.substring(0, separator) : remainder;
        return tenantCode.isBlank() ? null : tenantCode;
    }
}
