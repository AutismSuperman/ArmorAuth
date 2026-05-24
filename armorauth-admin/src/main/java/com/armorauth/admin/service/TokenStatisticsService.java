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
package com.armorauth.admin.service;

import com.armorauth.common.audit.SecurityAuditEvent;
import com.armorauth.data.entity.TokenStatistics;
import com.armorauth.data.repository.TokenStatisticsRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Token 签发统计服务
 *
 * @author fulin
 * @since 2026-05-23
 */
@Service
public class TokenStatisticsService {

    private final TokenStatisticsRepository statsRepository;

    public TokenStatisticsService(TokenStatisticsRepository statsRepository) {
        this.statsRepository = statsRepository;
    }

    @EventListener
    @Transactional
    public void onTokenEvent(SecurityAuditEvent event) {
        String eventType = event.getEventType();
        if (!"TOKEN_ISSUED".equals(eventType) && !"TOKEN_REFRESHED".equals(eventType)) {
            return;
        }

        String clientId = event.getResourceId();
        if (clientId == null) {
            return;
        }

        String grantType = extractGrantType(event.getDetail());
        String tokenType = "TOKEN_REFRESHED".equals(eventType) ? "REFRESH_TOKEN" : "ACCESS_TOKEN";
        LocalDate today = LocalDate.now();

        TokenStatistics stats = statsRepository
                .findByClientIdAndGrantTypeAndTokenTypeAndDate(clientId, grantType, tokenType, today)
                .orElseGet(() -> {
                    TokenStatistics s = new TokenStatistics();
                    s.setClientId(clientId);
                    s.setGrantType(grantType);
                    s.setTokenType(tokenType);
                    s.setDate(today);
                    s.setCount(0L);
                    return s;
                });

        stats.setCount(stats.getCount() + 1);
        stats.setLastIssuedAt(Instant.now());
        statsRepository.save(stats);
    }

    @Transactional(readOnly = true)
    public List<TokenStatistics> getStatistics(String clientId, LocalDate from, LocalDate to) {
        return statsRepository.findByClientIdAndDateBetween(clientId, from, to);
    }

    @Transactional(readOnly = true)
    public List<TokenStatistics> getSummary(LocalDate from, LocalDate to) {
        return statsRepository.findAll().stream()
                .filter(s -> !s.getDate().isBefore(from) && !s.getDate().isAfter(to))
                .toList();
    }

    private String extractGrantType(String detail) {
        if (detail == null) return "unknown";
        if (detail.contains("authorization_code")) return "authorization_code";
        if (detail.contains("client_credentials")) return "client_credentials";
        if (detail.contains("refresh_token")) return "refresh_token";
        if (detail.contains("device_code")) return "urn:ietf:params:oauth:grant-type:device_code";
        return "unknown";
    }
}
