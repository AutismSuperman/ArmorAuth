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
package com.armorauth.configurers.web;

import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.security.web.util.matcher.RequestMatcher;

import jakarta.servlet.http.HttpServletRequest;

final class PermitAllSupport {

    private PermitAllSupport() {
    }

    static void permitAll(HttpSecurityBuilder<? extends HttpSecurityBuilder<?>> http, String... urls) {
        for (String url : urls) {
            if (url != null) {
                permitAll(http, new ExactUrlRequestMatcher(url));
            }
        }
    }

    @SuppressWarnings("unchecked")
    static void permitAll(HttpSecurityBuilder<? extends HttpSecurityBuilder<?>> http,
                          RequestMatcher... requestMatchers) {
        ExpressionUrlAuthorizationConfigurer<?> configurer = http
                .getConfigurer(ExpressionUrlAuthorizationConfigurer.class);
        configurer.getRegistry().requestMatchers(requestMatchers).permitAll();
    }

    private static final class ExactUrlRequestMatcher implements RequestMatcher {

        private String processUrl;

        private ExactUrlRequestMatcher(String processUrl) {
            this.processUrl = processUrl;
        }

        @Override
        public boolean matches(HttpServletRequest request) {
            String uri = request.getRequestURI();
            String query = request.getQueryString();
            if (query != null) {
                uri += "?" + query;
            }
            if ("".equals(request.getContextPath())) {
                return uri.equals(this.processUrl);
            }
            return uri.equals(request.getContextPath() + this.processUrl);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("ExactUrl [processUrl='").append(this.processUrl).append("']");
            return sb.toString();
        }

    }

}
