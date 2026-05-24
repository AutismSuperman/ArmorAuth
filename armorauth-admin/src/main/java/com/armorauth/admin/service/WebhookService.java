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

import com.armorauth.admin.dto.WebhookDTO;
import com.armorauth.common.audit.AuditContext;
import com.armorauth.common.audit.SecurityAuditEvent;
import com.armorauth.common.exception.ResourceNotFoundException;
import com.armorauth.crypto.SecretCryptoService;
import com.armorauth.data.entity.WebhookDelivery;
import com.armorauth.data.entity.WebhookEndpoint;
import com.armorauth.data.repository.WebhookDeliveryRepository;
import com.armorauth.data.repository.WebhookEndpointRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;

/**
 * Webhook 管理和事件投递服务
 *
 * @author fulin
 * @since 2026-05-23
 */
@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final WebhookEndpointRepository endpointRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final AuditEventService auditEventService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final SecretCryptoService secretCryptoService;

    public WebhookService(WebhookEndpointRepository endpointRepository,
                          WebhookDeliveryRepository deliveryRepository,
                          AuditEventService auditEventService,
                          ObjectMapper objectMapper,
                          SecretCryptoService secretCryptoService) {
        this.endpointRepository = endpointRepository;
        this.deliveryRepository = deliveryRepository;
        this.auditEventService = auditEventService;
        this.objectMapper = objectMapper;
        this.secretCryptoService = secretCryptoService;
        this.restTemplate = new RestTemplate();
    }

    // ===== Admin CRUD =====

    @Transactional(readOnly = true)
    public Page<WebhookDTO.Response> listEndpoints(Pageable pageable) {
        return endpointRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public WebhookDTO.Response getEndpoint(String id) {
        WebhookEndpoint ep = endpointRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Webhook", id));
        return toResponse(ep);
    }

    @Transactional
    public WebhookDTO.Response createEndpoint(WebhookDTO.CreateRequest request) {
        WebhookEndpoint ep = new WebhookEndpoint();
        ep.setName(request.name() == null || request.name().isBlank() ? request.url() : request.name());
        ep.setUrl(request.url());
        ep.setSecret(secretCryptoService.protect(request.secret()));
        ep.setEventTypes(request.eventTypes());
        ep.setEnabled(true);
        ep.setCreatedAt(Instant.now());
        ep = endpointRepository.save(ep);

        auditEventService.record("WEBHOOK_CREATED",
                AuditContext.getCurrentPrincipal(), "webhook", ep.getId(),
                "创建 Webhook: " + ep.getName(), AuditContext.getClientIp());

        return toResponse(ep);
    }

    @Transactional
    public WebhookDTO.Response updateEndpoint(String id, WebhookDTO.UpdateRequest request) {
        WebhookEndpoint ep = endpointRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Webhook", id));

        if (request.name() != null) ep.setName(request.name());
        if (request.url() != null) ep.setUrl(request.url());
        if (request.secret() != null && !request.secret().isBlank()) {
            ep.setSecret(secretCryptoService.protect(request.secret()));
        }
        if (request.eventTypes() != null) ep.setEventTypes(request.eventTypes());
        ep.setUpdatedAt(Instant.now());
        ep = endpointRepository.save(ep);

        auditEventService.record("WEBHOOK_UPDATED",
                AuditContext.getCurrentPrincipal(), "webhook", id,
                "更新 Webhook: " + ep.getName(), AuditContext.getClientIp());

        return toResponse(ep);
    }

    @Transactional
    public void updateStatus(String id, Boolean enabled) {
        WebhookEndpoint ep = endpointRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Webhook", id));
        ep.setEnabled(enabled);
        ep.setUpdatedAt(Instant.now());
        endpointRepository.save(ep);

        String action = Boolean.TRUE.equals(enabled) ? "启用" : "禁用";
        auditEventService.record("WEBHOOK_STATUS_CHANGED",
                AuditContext.getCurrentPrincipal(), "webhook", id,
                action + " Webhook: " + ep.getName(), AuditContext.getClientIp());
    }

    @Transactional
    public void deleteEndpoint(String id) {
        WebhookEndpoint ep = endpointRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Webhook", id));
        String name = ep.getName();
        endpointRepository.delete(ep);

        auditEventService.record("WEBHOOK_DELETED",
                AuditContext.getCurrentPrincipal(), "webhook", id,
                "删除 Webhook: " + name, AuditContext.getClientIp());
    }

    @Transactional(readOnly = true)
    public Page<WebhookDTO.DeliveryResponse> listDeliveries(String endpointId, Pageable pageable) {
        return deliveryRepository.findByEndpointIdOrderByCreatedAtDesc(endpointId, pageable)
                .map(this::toDeliveryResponse);
    }

    // ===== Event Delivery =====

    @Async
    @EventListener
    public void onSecurityAuditEvent(SecurityAuditEvent event) {
        String eventType = event.getEventType();
        try {
            Map<String, Object> payload = Map.of(
                    "eventType", eventType,
                    "principalName", event.getPrincipalName() != null ? event.getPrincipalName() : "",
                    "resourceType", event.getResourceType() != null ? event.getResourceType() : "",
                    "resourceId", event.getResourceId() != null ? event.getResourceId() : "",
                    "detail", event.getDetail() != null ? event.getDetail() : "",
                    "ipAddress", event.getIpAddress() != null ? event.getIpAddress() : "",
                    "timestamp", Instant.now().toString()
            );
            String payloadJson = objectMapper.writeValueAsString(payload);

            for (WebhookEndpoint ep : endpointRepository.findByEnabledTrue()) {
                if (matchesEventType(ep.getEventTypes(), eventType)) {
                    deliverToEndpoint(ep, eventType, payloadJson);
                }
            }
        } catch (Exception e) {
            log.error("Webhook delivery failed for event {}", eventType, e);
        }
    }

    private void deliverToEndpoint(WebhookEndpoint ep, String eventType, String payload) {
        WebhookDelivery delivery = new WebhookDelivery();
        delivery.setEndpointId(ep.getId());
        delivery.setEventType(eventType);
        delivery.setPayload(payload);
        delivery.setCreatedAt(Instant.now());

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Webhook-Event", eventType);
            if (ep.getSecret() != null && !ep.getSecret().isBlank()) {
                String signature = computeHmacSha256(secretCryptoService.reveal(ep.getSecret()), payload);
                headers.set("X-Webhook-Signature", "sha256=" + signature);
            }

            HttpEntity<String> request = new HttpEntity<>(payload, headers);
            restTemplate.postForEntity(ep.getUrl(), request, String.class);

            delivery.setSuccess(true);
            delivery.setResponseStatus(200);
        } catch (Exception e) {
            delivery.setSuccess(false);
            delivery.setResponseBody(e.getMessage());
            delivery.setRetryCount(1);
            log.warn("Webhook delivery failed for endpoint {}: {}", ep.getName(), e.getMessage());
        }

        deliveryRepository.save(delivery);
    }

    private boolean matchesEventType(String eventTypes, String eventType) {
        if (eventTypes == null || eventTypes.isBlank()) {
            return true;
        }
        for (String pattern : eventTypes.split(",")) {
            String trimmed = pattern.trim();
            if (trimmed.equals("*") || trimmed.equals(eventType)) {
                return true;
            }
            if (trimmed.endsWith("*") && eventType.startsWith(trimmed.substring(0, trimmed.length() - 1))) {
                return true;
            }
        }
        return false;
    }

    private String computeHmacSha256(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("HMAC computation failed", e);
        }
    }

    private WebhookDTO.Response toResponse(WebhookEndpoint ep) {
        return new WebhookDTO.Response(
                ep.getId(), ep.getName(), ep.getUrl(), ep.getEventTypes(),
                ep.getEnabled(), ep.getCreatedAt(), ep.getUpdatedAt()
        );
    }

    private WebhookDTO.DeliveryResponse toDeliveryResponse(WebhookDelivery d) {
        return new WebhookDTO.DeliveryResponse(
                d.getId(), d.getEndpointId(), d.getEventType(), d.getPayload(),
                d.getResponseStatus(), d.getSuccess(), d.getRetryCount(), d.getCreatedAt()
        );
    }
}
