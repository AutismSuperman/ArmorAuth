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
package com.armorauth.admin.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * API 延迟指标过滤器
 *
 * @author fulin
 * @since 2026-05-23
 */
@Component
public class ApiLatencyMetricsFilter extends OncePerRequestFilter {

    private final Timer apiTimer;

    public ApiLatencyMetricsFilter(MeterRegistry meterRegistry) {
        this.apiTimer = Timer.builder("armorauth.api.latency")
                .description("API 请求延迟")
                .register(meterRegistry);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long start = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.nanoTime() - start;
            apiTimer.record(duration, java.util.concurrent.TimeUnit.NANOSECONDS);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/assets/") || uri.startsWith("/static/")
                || uri.startsWith("/webjars/") || uri.equals("/favicon.ico");
    }
}
