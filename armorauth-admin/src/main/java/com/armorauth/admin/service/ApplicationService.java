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
import com.armorauth.data.entity.Tenant;
import com.armorauth.data.repository.OAuth2ClientRepository;
import com.armorauth.data.repository.OAuth2ClientSettingsRepository;
import com.armorauth.data.repository.OAuth2ScopeRepository;
import com.armorauth.data.repository.OAuth2TokenSettingsRepository;
import com.armorauth.data.repository.TenantRepository;
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

    private static final String DEFAULT_TENANT_ID = "tenant-default";
    private static final String REGISTRATION_SOURCE_ADMIN = "ADMIN";
    private static final String SCOPE_CLIENT_CREATE = "client.create";
    private static final String SCOPE_CLIENT_READ = "client.read";
    private static final Set<String> DPOP_ALLOWED_ALGORITHMS = Set.of(
            "ES256", "ES384", "ES512", "RS256", "RS384", "RS512", "PS256", "PS384", "PS512");

    private final OAuth2ClientRepository clientRepository;
    private final OAuth2ClientSettingsRepository clientSettingsRepository;
    private final OAuth2TokenSettingsRepository tokenSettingsRepository;
    private final OAuth2ScopeRepository scopeRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditEventService auditEventService;
    private final boolean allowHttpLocal;

    public ApplicationService(OAuth2ClientRepository clientRepository,
                              OAuth2ClientSettingsRepository clientSettingsRepository,
                              OAuth2TokenSettingsRepository tokenSettingsRepository,
                              OAuth2ScopeRepository scopeRepository,
                              TenantRepository tenantRepository,
                              PasswordEncoder passwordEncoder,
                              AuditEventService auditEventService,
                              @Value("${armorauth.admin.redirect-uri-allow-http-local:true}") boolean allowHttpLocal) {
        this.clientRepository = clientRepository;
        this.clientSettingsRepository = clientSettingsRepository;
        this.tokenSettingsRepository = tokenSettingsRepository;
        this.scopeRepository = scopeRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditEventService = auditEventService;
        this.allowHttpLocal = allowHttpLocal;
    }

    /**
     * 分页查询应用列表
     */
    @Transactional(readOnly = true)
    public Page<ApplicationDTO.Response> listApplications(String tenantId, Pageable pageable) {
        if (hasText(tenantId)) {
            return clientRepository.findByTenantId(tenantId, pageable).map(this::toResponse);
        }
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
        String tenantId = resolveTenantId(request.tenantId());
        validateTenantExists(tenantId);
        validateClientProtocolSettings(request.clientAuthenticationMethods(), request.authorizationGrantTypes(),
                request.clientSettings(), request.tokenSettings(), request.dynamicClientRegistrar());

        // 校验 redirect URI
        RedirectUriValidator.validateAll(request.redirectUris(), allowHttpLocal);
        RedirectUriValidator.validateAll(request.postLogoutRedirectUris(), allowHttpLocal);

        String clientId = UUID.randomUUID().toString();
        String rawSecret = UUID.randomUUID().toString();

        OAuth2Client client = new OAuth2Client();
        client.setId(UUID.randomUUID().toString());
        client.setTenantId(tenantId);
        client.setRegistrationSource(REGISTRATION_SOURCE_ADMIN);
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
            clientSettings.setX509CertificateSubjectDN(request.clientSettings().x509CertificateSubjectDN());
            clientSettings.setDpopEnabled(
                    request.clientSettings().dpopEnabled() != null
                            ? request.clientSettings().dpopEnabled() : false);
            clientSettings.setDpopRequired(
                    request.clientSettings().dpopRequired() != null
                            ? request.clientSettings().dpopRequired() : false);
            clientSettings.setDpopAllowedAlgorithms(request.clientSettings().dpopAllowedAlgorithms());
        } else {
            clientSettings.setRequireAuthorizationConsent(false);
            clientSettings.setRequireProofKey(false);
            clientSettings.setDpopEnabled(false);
            clientSettings.setDpopRequired(false);
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
            tokenSettings.setX509CertificateBoundAccessTokens(
                    request.tokenSettings().x509CertificateBoundAccessTokens() != null
                            ? request.tokenSettings().x509CertificateBoundAccessTokens() : false);
        } else {
            tokenSettings.setAccessTokenTimeToLive(Duration.ofSeconds(300));
            tokenSettings.setRefreshTokenTimeToLive(Duration.ofSeconds(3600));
            tokenSettings.setDeviceCodeTimeToLive(Duration.ofSeconds(300));
            tokenSettings.setAuthorizationCodeTimeToLive(Duration.ofSeconds(300));
            tokenSettings.setReuseRefreshTokens(false);
            tokenSettings.setTokenFormat("self-contained");
            tokenSettings.setX509CertificateBoundAccessTokens(false);
        }
        tokenSettingsRepository.save(tokenSettings);

        // 保存Scope
        Set<String> scopes = resolveRequestedScopes(request.scopes(), request.dynamicClientRegistrar());
        if (!scopes.isEmpty()) {
            for (String scope : scopes) {
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
        ApplicationDTO.Response response = toResponse(clientRepository.findOAuth2ClientById(client.getId()).orElse(client));
        return new ApplicationDTO.Response(
                response.id(), response.tenantId(), response.tenantCode(), response.issuerPath(),
                response.clientId(), response.clientName(),
                response.clientAuthenticationMethods(), response.authorizationGrantTypes(),
                response.redirectUris(), response.postLogoutRedirectUris(),
                response.scopes(), response.clientSettings(), response.tokenSettings(),
                response.clientIdIssuedAt(), response.clientSecretExpiresAt(),
                response.enabled(), response.mfaRequired(), response.registrationSource(),
                response.dynamicClientRegistrar(), rawSecret
        );
    }

    /**
     * 更新应用
     */
    @Transactional
    public ApplicationDTO.Response updateApplication(String id, ApplicationDTO.UpdateRequest request) {
        OAuth2Client client = clientRepository.findOAuth2ClientById(id)
                .orElseThrow(() -> new ResourceNotFoundException("应用", id));
        if (hasText(request.tenantId()) && !Objects.equals(client.getTenantId(), request.tenantId())) {
            throw new IllegalArgumentException("应用租户归属创建后不可修改");
        }
        String targetClientAuthenticationMethods = request.clientAuthenticationMethods() != null
                ? request.clientAuthenticationMethods()
                : client.getClientAuthenticationMethods();
        String targetGrantTypes = request.authorizationGrantTypes() != null
                ? request.authorizationGrantTypes()
                : client.getAuthorizationGrantTypes();
        validateClientProtocolSettings(targetClientAuthenticationMethods, targetGrantTypes,
                request.clientSettings(), request.tokenSettings(), request.dynamicClientRegistrar());

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
            OAuth2ClientSettings clientSettings = clientSettingsRepository.findById(client.getClientId()).orElse(new OAuth2ClientSettings());
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
            if (request.clientSettings().x509CertificateSubjectDN() != null) {
                clientSettings.setX509CertificateSubjectDN(request.clientSettings().x509CertificateSubjectDN());
            }
            if (request.clientSettings().dpopEnabled() != null) {
                clientSettings.setDpopEnabled(request.clientSettings().dpopEnabled());
            }
            if (request.clientSettings().dpopRequired() != null) {
                clientSettings.setDpopRequired(request.clientSettings().dpopRequired());
            }
            if (request.clientSettings().dpopAllowedAlgorithms() != null) {
                clientSettings.setDpopAllowedAlgorithms(request.clientSettings().dpopAllowedAlgorithms());
            }
            clientSettingsRepository.save(clientSettings);
        }

        // 更新Token设置
        if (request.tokenSettings() != null) {
            OAuth2TokenSettings tokenSettings = tokenSettingsRepository.findById(client.getClientId()).orElse(new OAuth2TokenSettings());
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
            if (request.tokenSettings().x509CertificateBoundAccessTokens() != null) {
                tokenSettings.setX509CertificateBoundAccessTokens(request.tokenSettings().x509CertificateBoundAccessTokens());
            }
            tokenSettingsRepository.save(tokenSettings);
        }

        // 更新Scope
        if (request.scopes() != null || request.dynamicClientRegistrar() != null) {
            Set<String> requestedScopes = request.scopes() != null
                    ? resolveRequestedScopes(request.scopes(), request.dynamicClientRegistrar())
                    : resolveRequestedScopes(currentScopeNames(client.getClientId()), request.dynamicClientRegistrar());
            replaceScopes(client.getClientId(), requestedScopes);
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
        Tenant tenant = resolveTenant(client.getTenantId());
        String tenantCode = tenant != null ? tenant.getTenantCode() : null;
        String issuerPath = tenantCode != null ? "/t/" + tenantCode : null;

        ApplicationDTO.ClientSettingsDTO clientSettingsDTO = null;
        if (client.getClientSettings() != null) {
            OAuth2ClientSettings cs = client.getClientSettings();
            clientSettingsDTO = new ApplicationDTO.ClientSettingsDTO(
                    cs.getJwkSetUrl(),
                    cs.getRequireAuthorizationConsent(),
                    cs.getRequireProofKey(),
                    cs.getSigningAlgorithm(),
                    cs.getX509CertificateSubjectDN(),
                    cs.getDpopEnabled(),
                    cs.getDpopRequired(),
                    cs.getDpopAllowedAlgorithms()
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
                    ts.getTokenFormat(),
                    ts.getX509CertificateBoundAccessTokens()
            );
        }

        return new ApplicationDTO.Response(
                client.getId(),
                client.getTenantId(),
                tenantCode,
                issuerPath,
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
                Optional.ofNullable(client.getRegistrationSource()).orElse(REGISTRATION_SOURCE_ADMIN),
                isDynamicClientRegistrar(scopes),
                null // 不返回secret明文
        );
    }

    private void validateClientProtocolSettings(String clientAuthenticationMethods,
                                                String authorizationGrantTypes,
                                                ApplicationDTO.ClientSettingsDTO clientSettings,
                                                ApplicationDTO.TokenSettingsDTO tokenSettings,
                                                Boolean dynamicClientRegistrar) {
        Set<String> methods = parseCsv(clientAuthenticationMethods);
        Set<String> grantTypes = parseCsv(authorizationGrantTypes);
        boolean usesTlsClientAuth = methods.contains("tls_client_auth");
        boolean usesSelfSignedTlsClientAuth = methods.contains("self_signed_tls_client_auth");
        boolean usesMtls = usesTlsClientAuth || usesSelfSignedTlsClientAuth;

        if (usesTlsClientAuth && (clientSettings == null || !hasText(clientSettings.x509CertificateSubjectDN()))) {
            throw new IllegalArgumentException("tls_client_auth 需要配置 x509CertificateSubjectDN");
        }
        if (usesSelfSignedTlsClientAuth && (clientSettings == null || !hasText(clientSettings.jwkSetUrl()))) {
            throw new IllegalArgumentException("self_signed_tls_client_auth 需要配置 jwkSetUrl");
        }
        if (tokenSettings != null
                && Boolean.TRUE.equals(tokenSettings.x509CertificateBoundAccessTokens())
                && !usesMtls) {
            throw new IllegalArgumentException("x509CertificateBoundAccessTokens 只能用于 mTLS 客户端认证方式");
        }
        if (clientSettings != null && Boolean.TRUE.equals(clientSettings.dpopRequired())
                && !Boolean.TRUE.equals(clientSettings.dpopEnabled())) {
            throw new IllegalArgumentException("dpopRequired 需要先启用 dpopEnabled");
        }
        if (clientSettings != null && hasText(clientSettings.dpopAllowedAlgorithms())) {
            Set<String> algorithms = parseCsv(clientSettings.dpopAllowedAlgorithms());
            if (!DPOP_ALLOWED_ALGORITHMS.containsAll(algorithms)) {
                throw new IllegalArgumentException("dpopAllowedAlgorithms 包含不支持的算法");
            }
        }
        if (Boolean.TRUE.equals(dynamicClientRegistrar)
                && !grantTypes.contains("client_credentials")) {
            throw new IllegalArgumentException("DCR registrar 客户端必须支持 client_credentials 授权类型");
        }
    }

    private String resolveTenantId(String tenantId) {
        return hasText(tenantId) ? tenantId : DEFAULT_TENANT_ID;
    }

    private void validateTenantExists(String tenantId) {
        if (DEFAULT_TENANT_ID.equals(tenantId)) {
            return;
        }
        if (!tenantRepository.existsById(tenantId)) {
            throw new ResourceNotFoundException("租户", tenantId);
        }
    }

    private Tenant resolveTenant(String tenantId) {
        if (!hasText(tenantId)) {
            return null;
        }
        return tenantRepository.findById(tenantId).orElse(null);
    }

    private Set<String> resolveRequestedScopes(Collection<String> requestedScopes, Boolean dynamicClientRegistrar) {
        Set<String> scopes = requestedScopes == null
                ? new LinkedHashSet<>()
                : requestedScopes.stream()
                .filter(this::hasText)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (Boolean.TRUE.equals(dynamicClientRegistrar)) {
            scopes.add(SCOPE_CLIENT_CREATE);
            scopes.add(SCOPE_CLIENT_READ);
        } else if (Boolean.FALSE.equals(dynamicClientRegistrar)) {
            scopes.remove(SCOPE_CLIENT_CREATE);
            scopes.remove(SCOPE_CLIENT_READ);
        }
        return scopes;
    }

    private List<String> currentScopeNames(String clientId) {
        return scopeRepository.findAllByClientId(clientId).stream()
                .map(OAuth2Scope::getScope)
                .toList();
    }

    private void replaceScopes(String clientId, Set<String> scopes) {
        List<OAuth2Scope> existingScopes = scopeRepository.findAllByClientId(clientId);
        if (!existingScopes.isEmpty()) {
            scopeRepository.deleteAll(existingScopes);
        }
        for (String scope : scopes) {
            OAuth2Scope oAuth2Scope = new OAuth2Scope();
            oAuth2Scope.setClientId(clientId);
            oAuth2Scope.setScope(scope);
            scopeRepository.save(oAuth2Scope);
        }
    }

    private boolean isDynamicClientRegistrar(Collection<String> scopes) {
        return scopes != null && scopes.contains(SCOPE_CLIENT_CREATE) && scopes.contains(SCOPE_CLIENT_READ);
    }

    private Set<String> parseCsv(String value) {
        if (value == null || value.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .collect(Collectors.toSet());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
