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

import com.armorauth.admin.dto.ApplicationDTO;
import com.armorauth.common.audit.AuditContext;
import com.armorauth.common.exception.ResourceNotFoundException;
import com.armorauth.common.validation.RedirectUriValidator;
import com.armorauth.data.entity.OAuth2Client;
import com.armorauth.data.entity.OAuth2ClientSettings;
import com.armorauth.data.entity.OAuth2Scope;
import com.armorauth.data.entity.OAuth2TokenSettings;
import com.armorauth.data.repository.OAuth2ClientRepository;
import com.armorauth.data.repository.OAuth2ClientSettingsRepository;
import com.armorauth.data.repository.OAuth2ScopeRepository;
import com.armorauth.data.repository.OAuth2TokenSettingsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 应用管理服务
 *
 * @author fulin
 * @since 2026-05-23
 */
@Service
public class ApplicationService {

    private final OAuth2ClientRepository clientRepository;
    private final OAuth2ClientSettingsRepository clientSettingsRepository;
    private final OAuth2TokenSettingsRepository tokenSettingsRepository;
    private final OAuth2ScopeRepository scopeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditEventService auditEventService;
    private final boolean allowHttpLocal;

    public ApplicationService(OAuth2ClientRepository clientRepository,
                              OAuth2ClientSettingsRepository clientSettingsRepository,
                              OAuth2TokenSettingsRepository tokenSettingsRepository,
                              OAuth2ScopeRepository scopeRepository,
                              PasswordEncoder passwordEncoder,
                              AuditEventService auditEventService,
                              @Value("${armorauth.admin.redirect-uri-allow-http-local:true}") boolean allowHttpLocal) {
        this.clientRepository = clientRepository;
        this.clientSettingsRepository = clientSettingsRepository;
        this.tokenSettingsRepository = tokenSettingsRepository;
        this.scopeRepository = scopeRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditEventService = auditEventService;
        this.allowHttpLocal = allowHttpLocal;
    }

    /**
     * 分页查询应用列表
     */
    @Transactional(readOnly = true)
    public Page<ApplicationDTO.Response> listApplications(Pageable pageable) {
        return clientRepository.findAll(pageable).map(this::toResponse);
    }

    /**
     * 根据ID获取应用详情
     */
    @Transactional(readOnly = true)
    public ApplicationDTO.Response getApplication(String id) {
        OAuth2Client client = clientRepository.findOAuth2ClientById(id)
                .orElseThrow(() -> new ResourceNotFoundException("应用", id));
        return toResponse(client);
    }

    /**
     * 创建应用
     */
    @Transactional
    public ApplicationDTO.Response createApplication(ApplicationDTO.CreateRequest request) {
        // 校验 redirect URI
        RedirectUriValidator.validateAll(request.redirectUris(), allowHttpLocal);
        RedirectUriValidator.validateAll(request.postLogoutRedirectUris(), allowHttpLocal);

        String clientId = UUID.randomUUID().toString();
        String rawSecret = UUID.randomUUID().toString();

        OAuth2Client client = new OAuth2Client();
        client.setClientId(clientId);
        client.setClientSecret(passwordEncoder.encode(rawSecret));
        client.setClientName(request.clientName());
        client.setClientAuthenticationMethods(request.clientAuthenticationMethods());
        client.setAuthorizationGrantTypes(request.authorizationGrantTypes());
        client.setRedirectUris(request.redirectUris());
        client.setPostLogoutRedirectUris(request.postLogoutRedirectUris());
        client.setClientIdIssuedAt(Instant.now());
        client.setEnabled(true);
        client.setMfaRequired(Boolean.TRUE.equals(request.mfaRequired()));
        client = clientRepository.save(client);

        // 保存客户端设置
        OAuth2ClientSettings clientSettings = new OAuth2ClientSettings();
        clientSettings.setClientId(clientId);
        if (request.clientSettings() != null) {
            clientSettings.setJwkSetUrl(request.clientSettings().jwkSetUrl());
            clientSettings.setRequireAuthorizationConsent(
                    request.clientSettings().requireAuthorizationConsent() != null
                            ? request.clientSettings().requireAuthorizationConsent() : false);
            clientSettings.setRequireProofKey(
                    request.clientSettings().requireProofKey() != null
                            ? request.clientSettings().requireProofKey() : false);
            clientSettings.setSigningAlgorithm(request.clientSettings().signingAlgorithm());
        } else {
            clientSettings.setRequireAuthorizationConsent(false);
            clientSettings.setRequireProofKey(false);
        }
        clientSettingsRepository.save(clientSettings);

        // 保存Token设置
        OAuth2TokenSettings tokenSettings = new OAuth2TokenSettings();
        tokenSettings.setClientId(clientId);
        if (request.tokenSettings() != null) {
            tokenSettings.setAccessTokenTimeToLive(
                    Duration.ofSeconds(request.tokenSettings().accessTokenTimeToLiveSeconds() != null
                            ? request.tokenSettings().accessTokenTimeToLiveSeconds() : 300));
            tokenSettings.setRefreshTokenTimeToLive(
                    Duration.ofSeconds(request.tokenSettings().refreshTokenTimeToLiveSeconds() != null
                            ? request.tokenSettings().refreshTokenTimeToLiveSeconds() : 3600));
            tokenSettings.setDeviceCodeTimeToLive(
                    Duration.ofSeconds(request.tokenSettings().deviceCodeTimeToLiveSeconds() != null
                            ? request.tokenSettings().deviceCodeTimeToLiveSeconds() : 300));
            tokenSettings.setAuthorizationCodeTimeToLive(
                    Duration.ofSeconds(request.tokenSettings().authorizationCodeTimeToLiveSeconds() != null
                            ? request.tokenSettings().authorizationCodeTimeToLiveSeconds() : 300));
            tokenSettings.setIdTokenSignatureAlgorithm(request.tokenSettings().idTokenSignatureAlgorithm());
            tokenSettings.setReuseRefreshTokens(
                    request.tokenSettings().reuseRefreshTokens() != null
                            ? request.tokenSettings().reuseRefreshTokens() : false);
            tokenSettings.setTokenFormat(request.tokenSettings().tokenFormat());
        } else {
            tokenSettings.setAccessTokenTimeToLive(Duration.ofSeconds(300));
            tokenSettings.setRefreshTokenTimeToLive(Duration.ofSeconds(3600));
            tokenSettings.setDeviceCodeTimeToLive(Duration.ofSeconds(300));
            tokenSettings.setAuthorizationCodeTimeToLive(Duration.ofSeconds(300));
            tokenSettings.setReuseRefreshTokens(false);
            tokenSettings.setTokenFormat("self-contained");
        }
        tokenSettingsRepository.save(tokenSettings);

        // 保存Scope
        if (request.scopes() != null && !request.scopes().isEmpty()) {
            for (String scope : request.scopes()) {
                OAuth2Scope oAuth2Scope = new OAuth2Scope();
                oAuth2Scope.setClientId(clientId);
                oAuth2Scope.setScope(scope);
                scopeRepository.save(oAuth2Scope);
            }
        }

        // 审计日志
        auditEventService.record("APPLICATION_CREATED",
                AuditContext.getCurrentPrincipal(), "application", clientId,
                "创建应用: " + request.clientName(), AuditContext.getClientIp());

        // 返回响应（只在创建时返回明文secret）
        ApplicationDTO.Response response = toResponse(clientRepository.findOAuth2ClientById(clientId).orElse(client));
        return new ApplicationDTO.Response(
                response.id(), response.clientId(), response.clientName(),
                response.clientAuthenticationMethods(), response.authorizationGrantTypes(),
                response.redirectUris(), response.postLogoutRedirectUris(),
                response.scopes(), response.clientSettings(), response.tokenSettings(),
                response.clientIdIssuedAt(), response.clientSecretExpiresAt(),
                response.enabled(), response.mfaRequired(), rawSecret
        );
    }

    /**
     * 更新应用
     */
    @Transactional
    public ApplicationDTO.Response updateApplication(String id, ApplicationDTO.UpdateRequest request) {
        OAuth2Client client = clientRepository.findOAuth2ClientById(id)
                .orElseThrow(() -> new ResourceNotFoundException("应用", id));

        // 校验 redirect URI
        if (request.redirectUris() != null) {
            RedirectUriValidator.validateAll(request.redirectUris(), allowHttpLocal);
        }
        if (request.postLogoutRedirectUris() != null) {
            RedirectUriValidator.validateAll(request.postLogoutRedirectUris(), allowHttpLocal);
        }

        if (request.clientName() != null) {
            client.setClientName(request.clientName());
        }
        if (request.clientAuthenticationMethods() != null) {
            client.setClientAuthenticationMethods(request.clientAuthenticationMethods());
        }
        if (request.authorizationGrantTypes() != null) {
            client.setAuthorizationGrantTypes(request.authorizationGrantTypes());
        }
        if (request.redirectUris() != null) {
            client.setRedirectUris(request.redirectUris());
        }
        if (request.postLogoutRedirectUris() != null) {
            client.setPostLogoutRedirectUris(request.postLogoutRedirectUris());
        }
        if (request.mfaRequired() != null) {
            client.setMfaRequired(request.mfaRequired());
        }
        clientRepository.save(client);

        // 更新客户端设置
        if (request.clientSettings() != null) {
            OAuth2ClientSettings clientSettings = clientSettingsRepository.findById(id).orElse(new OAuth2ClientSettings());
            clientSettings.setClientId(client.getClientId());
            if (request.clientSettings().jwkSetUrl() != null) {
                clientSettings.setJwkSetUrl(request.clientSettings().jwkSetUrl());
            }
            if (request.clientSettings().requireAuthorizationConsent() != null) {
                clientSettings.setRequireAuthorizationConsent(request.clientSettings().requireAuthorizationConsent());
            }
            if (request.clientSettings().requireProofKey() != null) {
                clientSettings.setRequireProofKey(request.clientSettings().requireProofKey());
            }
            if (request.clientSettings().signingAlgorithm() != null) {
                clientSettings.setSigningAlgorithm(request.clientSettings().signingAlgorithm());
            }
            clientSettingsRepository.save(clientSettings);
        }

        // 更新Token设置
        if (request.tokenSettings() != null) {
            OAuth2TokenSettings tokenSettings = tokenSettingsRepository.findById(id).orElse(new OAuth2TokenSettings());
            tokenSettings.setClientId(client.getClientId());
            if (request.tokenSettings().accessTokenTimeToLiveSeconds() != null) {
                tokenSettings.setAccessTokenTimeToLive(Duration.ofSeconds(request.tokenSettings().accessTokenTimeToLiveSeconds()));
            }
            if (request.tokenSettings().refreshTokenTimeToLiveSeconds() != null) {
                tokenSettings.setRefreshTokenTimeToLive(Duration.ofSeconds(request.tokenSettings().refreshTokenTimeToLiveSeconds()));
            }
            if (request.tokenSettings().reuseRefreshTokens() != null) {
                tokenSettings.setReuseRefreshTokens(request.tokenSettings().reuseRefreshTokens());
            }
            if (request.tokenSettings().tokenFormat() != null) {
                tokenSettings.setTokenFormat(request.tokenSettings().tokenFormat());
            }
            tokenSettingsRepository.save(tokenSettings);
        }

        // 更新Scope
        if (request.scopes() != null) {
            List<OAuth2Scope> existingScopes = scopeRepository.findAllByClientIdAndScopeIn(
                    client.getClientId(), request.scopes());
            Set<String> existingScopeNames = existingScopes.stream()
                    .map(OAuth2Scope::getScope)
                    .collect(Collectors.toSet());
            for (String scope : request.scopes()) {
                if (!existingScopeNames.contains(scope)) {
                    OAuth2Scope oAuth2Scope = new OAuth2Scope();
                    oAuth2Scope.setClientId(client.getClientId());
                    oAuth2Scope.setScope(scope);
                    scopeRepository.save(oAuth2Scope);
                }
            }
        }

        // 审计日志
        auditEventService.record("APPLICATION_UPDATED",
                AuditContext.getCurrentPrincipal(), "application", id,
                "更新应用: " + client.getClientName(), AuditContext.getClientIp());

        return toResponse(clientRepository.findOAuth2ClientById(id).orElse(client));
    }

    /**
     * 启用/禁用应用
     */
    @Transactional
    public void updateApplicationStatus(String id, Boolean enabled) {
        OAuth2Client client = clientRepository.findOAuth2ClientById(id)
                .orElseThrow(() -> new ResourceNotFoundException("应用", id));
        client.setEnabled(enabled);
        clientRepository.save(client);

        String action = Boolean.TRUE.equals(enabled) ? "启用" : "禁用";
        auditEventService.record("APPLICATION_STATUS_CHANGED",
                AuditContext.getCurrentPrincipal(), "application", id,
                action + "应用: " + client.getClientName(), AuditContext.getClientIp());
    }

    /**
     * 重置客户端密钥
     */
    @Transactional
    public ApplicationDTO.SecretResponse rotateSecret(String id) {
        OAuth2Client client = clientRepository.findOAuth2ClientById(id)
                .orElseThrow(() -> new ResourceNotFoundException("应用", id));

        String rawSecret = UUID.randomUUID().toString();
        client.setClientSecret(passwordEncoder.encode(rawSecret));
        client.setClientSecretExpiresAt(Instant.now().plus(Duration.ofDays(365)));
        clientRepository.save(client);

        auditEventService.record("APPLICATION_SECRET_ROTATED",
                AuditContext.getCurrentPrincipal(), "application", id,
                "重置密钥: " + client.getClientName(), AuditContext.getClientIp());

        return new ApplicationDTO.SecretResponse(client.getClientId(), rawSecret);
    }

    /**
     * 删除应用
     */
    @Transactional
    public void deleteApplication(String id) {
        OAuth2Client client = clientRepository.findOAuth2ClientById(id)
                .orElseThrow(() -> new ResourceNotFoundException("应用", id));

        String clientName = client.getClientName();

        // 删除关联的scope
        if (client.getScopes() != null) {
            scopeRepository.deleteAll(client.getScopes());
        }
        // 删除客户端设置
        clientSettingsRepository.deleteById(client.getClientId());
        // 删除token设置
        tokenSettingsRepository.deleteById(client.getClientId());
        // 删除客户端
        clientRepository.delete(client);

        auditEventService.record("APPLICATION_DELETED",
                AuditContext.getCurrentPrincipal(), "application", id,
                "删除应用: " + clientName, AuditContext.getClientIp());
    }

    private ApplicationDTO.Response toResponse(OAuth2Client client) {
        List<String> scopes = client.getScopes() != null
                ? client.getScopes().stream().map(OAuth2Scope::getScope).toList()
                : Collections.emptyList();

        ApplicationDTO.ClientSettingsDTO clientSettingsDTO = null;
        if (client.getClientSettings() != null) {
            OAuth2ClientSettings cs = client.getClientSettings();
            clientSettingsDTO = new ApplicationDTO.ClientSettingsDTO(
                    cs.getJwkSetUrl(),
                    cs.getRequireAuthorizationConsent(),
                    cs.getRequireProofKey(),
                    cs.getSigningAlgorithm()
            );
        }

        ApplicationDTO.TokenSettingsDTO tokenSettingsDTO = null;
        if (client.getTokenSettings() != null) {
            OAuth2TokenSettings ts = client.getTokenSettings();
            tokenSettingsDTO = new ApplicationDTO.TokenSettingsDTO(
                    ts.getAccessTokenTimeToLive() != null ? ts.getAccessTokenTimeToLive().getSeconds() : null,
                    ts.getRefreshTokenTimeToLive() != null ? ts.getRefreshTokenTimeToLive().getSeconds() : null,
                    ts.getDeviceCodeTimeToLive() != null ? ts.getDeviceCodeTimeToLive().getSeconds() : null,
                    ts.getAuthorizationCodeTimeToLive() != null ? ts.getAuthorizationCodeTimeToLive().getSeconds() : null,
                    ts.getIdTokenSignatureAlgorithm(),
                    ts.getReuseRefreshTokens(),
                    ts.getTokenFormat()
            );
        }

        return new ApplicationDTO.Response(
                client.getId(),
                client.getClientId(),
                client.getClientName(),
                client.getClientAuthenticationMethods(),
                client.getAuthorizationGrantTypes(),
                client.getRedirectUris(),
                client.getPostLogoutRedirectUris(),
                scopes,
                clientSettingsDTO,
                tokenSettingsDTO,
                client.getClientIdIssuedAt(),
                client.getClientSecretExpiresAt(),
                client.getEnabled(),
                client.getMfaRequired(),
                null // 不返回secret明文
        );
    }
}
