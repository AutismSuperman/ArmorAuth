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
import com.armorauth.data.entity.Authorization;
import com.armorauth.data.entity.OAuth2Client;
import com.armorauth.data.entity.TokenStatistics;
import com.armorauth.data.repository.AuthorizationRepository;
import com.armorauth.data.repository.OAuth2ClientRepository;
import com.armorauth.data.repository.TokenStatisticsRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Token 签发统计服务
 *
 * @author fulin
 * @since 2026-05-23
 */
@Service
public class TokenStatisticsService {

    private final TokenStatisticsRepository statsRepository;
    private final AuthorizationRepository authorizationRepository;
    private final OAuth2ClientRepository clientRepository;

    public TokenStatisticsService(TokenStatisticsRepository statsRepository,
                                  AuthorizationRepository authorizationRepository,
                                  OAuth2ClientRepository clientRepository) {
        this.statsRepository = statsRepository;
        this.authorizationRepository = authorizationRepository;
        this.clientRepository = clientRepository;
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
        List<TokenStatistics> persisted = sortStatistics(
                statsRepository.findByClientIdAndDateBetween(clientId, from, to));
        return persisted.isEmpty() ? buildAuthorizationSummary(clientId, from, to) : persisted;
    }

    @Transactional(readOnly = true)
    public List<TokenStatistics> getSummary(LocalDate from, LocalDate to) {
        List<TokenStatistics> persisted = statsRepository.findAll().stream()
                .filter(s -> !s.getDate().isBefore(from) && !s.getDate().isAfter(to))
                .toList();
        return persisted.isEmpty() ? buildAuthorizationSummary(null, from, to) : sortStatistics(persisted);
    }

    private String extractGrantType(String detail) {
        if (detail == null) return "unknown";
        if (detail.contains("authorization_code")) return "authorization_code";
        if (detail.contains("client_credentials")) return "client_credentials";
        if (detail.contains("refresh_token")) return "refresh_token";
        if (detail.contains("device_code")) return "urn:ietf:params:oauth:grant-type:device_code";
        return "unknown";
    }

    private List<TokenStatistics> buildAuthorizationSummary(String clientIdFilter, LocalDate from, LocalDate to) {
        ZoneId zoneId = ZoneId.systemDefault();
        Instant fromInstant = from.atStartOfDay(zoneId).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(zoneId).toInstant();
        Map<String, String> clientIdCache = new HashMap<>();
        Map<String, TokenStatistics> summaries = new HashMap<>();

        for (Authorization authorization : authorizationRepository.findIssuedAccessTokens(fromInstant, toInstant)) {
            String clientId = clientIdCache.computeIfAbsent(authorization.getRegisteredClientId(), this::resolveClientId);
            if (StringUtils.hasText(clientIdFilter) && !clientIdFilter.equals(clientId)) {
                continue;
            }

            LocalDate date = authorization.getAccessTokenIssuedAt().atZone(zoneId).toLocalDate();
            String grantType = StringUtils.hasText(authorization.getAuthorizationGrantType())
                    ? authorization.getAuthorizationGrantType()
                    : "unknown";
            String tokenType = "ACCESS_TOKEN";
            String key = clientId + "|" + grantType + "|" + tokenType + "|" + date;
            TokenStatistics stats = summaries.computeIfAbsent(key, ignored -> {
                TokenStatistics s = new TokenStatistics();
                s.setId("live|" + key);
                s.setClientId(clientId);
                s.setGrantType(grantType);
                s.setTokenType(tokenType);
                s.setDate(date);
                s.setCount(0L);
                return s;
            });

            stats.setCount(stats.getCount() + 1);
            Instant issuedAt = authorization.getAccessTokenIssuedAt();
            if (stats.getLastIssuedAt() == null || issuedAt.isAfter(stats.getLastIssuedAt())) {
                stats.setLastIssuedAt(issuedAt);
            }
        }

        return sortStatistics(new ArrayList<>(summaries.values()));
    }

    private String resolveClientId(String registeredClientId) {
        if (!StringUtils.hasText(registeredClientId)) {
            return "unknown";
        }
        return clientRepository.findOAuth2ClientById(registeredClientId)
                .map(OAuth2Client::getClientId)
                .orElse(registeredClientId);
    }

    private List<TokenStatistics> sortStatistics(List<TokenStatistics> statistics) {
        return statistics.stream()
                .sorted(Comparator.comparing(TokenStatistics::getDate,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(TokenStatistics::getClientId,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(TokenStatistics::getGrantType,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }
}
