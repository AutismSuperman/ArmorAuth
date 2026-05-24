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

import com.armorauth.admin.dto.IdentityProviderDTO;
import com.armorauth.common.audit.AuditContext;
import com.armorauth.common.exception.ResourceNotFoundException;
import com.armorauth.common.exception.ValidationException;
import com.armorauth.crypto.SecretCryptoService;
import com.armorauth.data.entity.IdentityProvider;
import com.armorauth.data.repository.IdentityProviderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class IdentityProviderService {

    private final IdentityProviderRepository idpRepository;
    private final AuditEventService auditEventService;
    private final SecretCryptoService secretCryptoService;
    private final LdapDirectorySyncService ldapDirectorySyncService;

    public IdentityProviderService(IdentityProviderRepository idpRepository,
                                   AuditEventService auditEventService,
                                   SecretCryptoService secretCryptoService,
                                   LdapDirectorySyncService ldapDirectorySyncService) {
        this.idpRepository = idpRepository;
        this.auditEventService = auditEventService;
        this.secretCryptoService = secretCryptoService;
        this.ldapDirectorySyncService = ldapDirectorySyncService;
    }

    @Transactional(readOnly = true)
    public Page<IdentityProviderDTO.Response> listProviders(Pageable pageable) {
        return idpRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public IdentityProviderDTO.Response getProvider(String id) {
        IdentityProvider idp = idpRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("身份源", id));
        return toResponse(idp);
    }

    @Transactional
    public IdentityProviderDTO.Response createProvider(IdentityProviderDTO.CreateRequest request) {
        if (idpRepository.existsByRegistrationId(request.registrationId())) {
            throw new ValidationException("Registration ID 已存在: " + request.registrationId());
        }

        IdentityProvider idp = new IdentityProvider();
        idp.setProviderName(request.providerName());
        idp.setProviderType(parseProviderType(request.providerType()));
        idp.setRegistrationId(request.registrationId());
        idp.setClientId(request.clientId());
        idp.setClientSecret(protectSecret(request.clientSecret()));
        idp.setAuthorizationUri(request.authorizationUri());
        idp.setTokenUri(request.tokenUri());
        idp.setUserinfoUri(request.userinfoUri());
        idp.setJwkSetUri(request.jwkSetUri());
        idp.setSamlEntityId(request.samlEntityId());
        idp.setSamlSsoUrl(request.samlSsoUrl());
        idp.setSamlSloUrl(request.samlSloUrl());
        idp.setSamlX509Certificate(request.samlX509Certificate());
        idp.setSamlMetadataUrl(request.samlMetadataUrl());
        idp.setSamlSpEntityId(request.samlSpEntityId());
        idp.setSamlAcsUrl(request.samlAcsUrl());
        idp.setSamlNameIdFormat(request.samlNameIdFormat());
        idp.setLdapUrl(request.ldapUrl());
        idp.setLdapBaseDn(request.ldapBaseDn());
        idp.setLdapBindDn(request.ldapBindDn());
        idp.setLdapBindPassword(protectSecret(request.ldapBindPassword()));
        idp.setLdapUserSearchBase(request.ldapUserSearchBase());
        idp.setLdapUserSearchFilter(request.ldapUserSearchFilter());
        idp.setLdapUsernameAttribute(request.ldapUsernameAttribute());
        idp.setLdapEmailAttribute(request.ldapEmailAttribute());
        idp.setLdapPhoneAttribute(request.ldapPhoneAttribute());
        idp.setLdapDisplayNameAttribute(request.ldapDisplayNameAttribute());
        idp.setLdapGroupAttribute(request.ldapGroupAttribute());
        idp.setLdapUseSsl(Boolean.TRUE.equals(request.ldapUseSsl()));
        idp.setLdapStartTls(Boolean.TRUE.equals(request.ldapStartTls()));
        idp.setLdapPageSize(request.ldapPageSize() != null ? request.ldapPageSize() : 200);
        idp.setScopes(request.scopes());
        idp.setAttributeMapping(request.attributeMapping());
        idp.setLinkingStrategy(request.linkingStrategy() != null
                ? parseLinkingStrategy(request.linkingStrategy())
                : IdentityProvider.LinkingStrategy.AUTO_REGISTER);
        idp.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
        idp.setEnabled(true);
        idp.setCreatedAt(Instant.now());
        idp = idpRepository.save(idp);

        auditEventService.record("IDENTITY_PROVIDER_CREATED",
                AuditContext.getCurrentPrincipal(), "identity_provider", idp.getId(),
                "创建身份源: " + idp.getProviderName(), AuditContext.getClientIp());

        return toResponse(idp);
    }

    @Transactional
    public IdentityProviderDTO.Response updateProvider(String id, IdentityProviderDTO.UpdateRequest request) {
        IdentityProvider idp = idpRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("身份源", id));

        if (request.providerName() != null) idp.setProviderName(request.providerName());
        if (request.clientId() != null) idp.setClientId(request.clientId());
        if (request.clientSecret() != null && !request.clientSecret().isBlank()) {
            idp.setClientSecret(protectSecret(request.clientSecret()));
        }
        if (request.authorizationUri() != null) idp.setAuthorizationUri(request.authorizationUri());
        if (request.tokenUri() != null) idp.setTokenUri(request.tokenUri());
        if (request.userinfoUri() != null) idp.setUserinfoUri(request.userinfoUri());
        if (request.jwkSetUri() != null) idp.setJwkSetUri(request.jwkSetUri());
        if (request.samlEntityId() != null) idp.setSamlEntityId(request.samlEntityId());
        if (request.samlSsoUrl() != null) idp.setSamlSsoUrl(request.samlSsoUrl());
        if (request.samlSloUrl() != null) idp.setSamlSloUrl(request.samlSloUrl());
        if (request.samlX509Certificate() != null) idp.setSamlX509Certificate(request.samlX509Certificate());
        if (request.samlMetadataUrl() != null) idp.setSamlMetadataUrl(request.samlMetadataUrl());
        if (request.samlSpEntityId() != null) idp.setSamlSpEntityId(request.samlSpEntityId());
        if (request.samlAcsUrl() != null) idp.setSamlAcsUrl(request.samlAcsUrl());
        if (request.samlNameIdFormat() != null) idp.setSamlNameIdFormat(request.samlNameIdFormat());
        if (request.ldapUrl() != null) idp.setLdapUrl(request.ldapUrl());
        if (request.ldapBaseDn() != null) idp.setLdapBaseDn(request.ldapBaseDn());
        if (request.ldapBindDn() != null) idp.setLdapBindDn(request.ldapBindDn());
        if (request.ldapBindPassword() != null && !request.ldapBindPassword().isBlank()) {
            idp.setLdapBindPassword(protectSecret(request.ldapBindPassword()));
        }
        if (request.ldapUserSearchBase() != null) idp.setLdapUserSearchBase(request.ldapUserSearchBase());
        if (request.ldapUserSearchFilter() != null) idp.setLdapUserSearchFilter(request.ldapUserSearchFilter());
        if (request.ldapUsernameAttribute() != null) idp.setLdapUsernameAttribute(request.ldapUsernameAttribute());
        if (request.ldapEmailAttribute() != null) idp.setLdapEmailAttribute(request.ldapEmailAttribute());
        if (request.ldapPhoneAttribute() != null) idp.setLdapPhoneAttribute(request.ldapPhoneAttribute());
        if (request.ldapDisplayNameAttribute() != null) idp.setLdapDisplayNameAttribute(request.ldapDisplayNameAttribute());
        if (request.ldapGroupAttribute() != null) idp.setLdapGroupAttribute(request.ldapGroupAttribute());
        if (request.ldapUseSsl() != null) idp.setLdapUseSsl(request.ldapUseSsl());
        if (request.ldapStartTls() != null) idp.setLdapStartTls(request.ldapStartTls());
        if (request.ldapPageSize() != null) idp.setLdapPageSize(request.ldapPageSize());
        if (request.scopes() != null) idp.setScopes(request.scopes());
        if (request.attributeMapping() != null) idp.setAttributeMapping(request.attributeMapping());
        if (request.linkingStrategy() != null) idp.setLinkingStrategy(parseLinkingStrategy(request.linkingStrategy()));
        if (request.displayOrder() != null) idp.setDisplayOrder(request.displayOrder());
        idp.setUpdatedAt(Instant.now());
        idp = idpRepository.save(idp);

        auditEventService.record("IDENTITY_PROVIDER_UPDATED",
                AuditContext.getCurrentPrincipal(), "identity_provider", id,
                "更新身份源: " + idp.getProviderName(), AuditContext.getClientIp());

        return toResponse(idp);
    }

    @Transactional
    public void updateProviderStatus(String id, Boolean enabled) {
        IdentityProvider idp = idpRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("身份源", id));
        idp.setEnabled(enabled);
        idp.setUpdatedAt(Instant.now());
        idpRepository.save(idp);

        String action = Boolean.TRUE.equals(enabled) ? "启用" : "禁用";
        auditEventService.record("IDENTITY_PROVIDER_STATUS_CHANGED",
                AuditContext.getCurrentPrincipal(), "identity_provider", id,
                action + "身份源: " + idp.getProviderName(), AuditContext.getClientIp());
    }

    @Transactional
    public void deleteProvider(String id) {
        IdentityProvider idp = idpRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("身份源", id));
        String providerName = idp.getProviderName();
        idpRepository.delete(idp);

        auditEventService.record("IDENTITY_PROVIDER_DELETED",
                AuditContext.getCurrentPrincipal(), "identity_provider", id,
                "删除身份源: " + providerName, AuditContext.getClientIp());
    }

    @Transactional(readOnly = true)
    public IdentityProviderDTO.TestResponse testProvider(String id, boolean probeRemote) {
        IdentityProvider idp = idpRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("身份源", id));
        Map<String, Object> checks = new LinkedHashMap<>();

        boolean valid = checkRequired(checks, "providerName", idp.getProviderName())
                & checkRequired(checks, "registrationId", idp.getRegistrationId());

        IdentityProvider.ProviderType providerType = idp.getProviderType();
        checks.put("providerType", providerType != null ? providerType.name() : "missing");
        if (providerType == IdentityProvider.ProviderType.SAML) {
            boolean samlValid = valid & validateSamlProvider(checks, idp, probeRemote);
            checks.put("runtimeSupport", "sp_redirect_post_assertion");
            String message = samlValid
                    ? "SAML 配置字段检查通过，运行时登录接入已启用"
                    : "SAML 配置字段检查失败";
            return new IdentityProviderDTO.TestResponse(samlValid, message, checks);
        }
        if (providerType == IdentityProvider.ProviderType.LDAP) {
            boolean ldapValid = valid & ldapDirectorySyncService.validateConfiguration(checks, idp);
            checks.put("runtimeSupport", "bind_search_user_sync_login");
            if (ldapValid && probeRemote) {
                ldapValid &= ldapDirectorySyncService.probe(checks, idp,
                        idp.getLdapPageSize() != null ? idp.getLdapPageSize() : 20);
            } else {
                checks.put("remoteProbe", "skipped");
            }
            String message = ldapValid
                    ? "LDAP/AD 配置检查通过"
                    : "LDAP/AD 配置检查失败";
            return new IdentityProviderDTO.TestResponse(ldapValid, message, checks);
        }

        valid &= checkRequired(checks, "clientId", idp.getClientId());
        valid &= checkUrl(checks, "authorizationUri", idp.getAuthorizationUri());
        valid &= checkUrl(checks, "tokenUri", idp.getTokenUri());
        valid &= checkUrl(checks, "userinfoUri", idp.getUserinfoUri());
        if (idp.getJwkSetUri() != null && !idp.getJwkSetUri().isBlank()) {
            valid &= checkUrl(checks, "jwkSetUri", idp.getJwkSetUri());
        }

        if (valid && probeRemote) {
            valid &= probeUrl(checks, "authorizationUriReachable", idp.getAuthorizationUri());
            valid &= probeUrl(checks, "tokenUriReachable", idp.getTokenUri());
            valid &= probeUrl(checks, "userinfoUriReachable", idp.getUserinfoUri());
            if (idp.getJwkSetUri() != null && !idp.getJwkSetUri().isBlank()) {
                valid &= probeUrl(checks, "jwkSetUriReachable", idp.getJwkSetUri());
            }
        } else {
            checks.put("remoteProbe", "skipped");
        }

        String message = valid ? "身份源配置检查通过" : "身份源配置检查失败";
        return new IdentityProviderDTO.TestResponse(valid, message, checks);
    }

    @Transactional
    public IdentityProviderDTO.LdapSyncResponse syncLdapUsers(
            String id,
            IdentityProviderDTO.LdapSyncRequest request) {
        return ldapDirectorySyncService.syncUsers(id, request);
    }

    private IdentityProviderDTO.Response toResponse(IdentityProvider idp) {
        return new IdentityProviderDTO.Response(
                idp.getId(), idp.getProviderName(), idp.getProviderType().name(),
                idp.getRegistrationId(), idp.getClientId(),
                idp.getAuthorizationUri(), idp.getTokenUri(), idp.getUserinfoUri(),
                idp.getJwkSetUri(),
                idp.getSamlEntityId(), idp.getSamlSsoUrl(), idp.getSamlSloUrl(),
                idp.getSamlX509Certificate(), idp.getSamlMetadataUrl(),
                idp.getSamlSpEntityId(), idp.getSamlAcsUrl(), idp.getSamlNameIdFormat(),
                idp.getLdapUrl(), idp.getLdapBaseDn(), idp.getLdapBindDn(),
                idp.getLdapBindPassword() != null && !idp.getLdapBindPassword().isBlank(),
                idp.getLdapUserSearchBase(), idp.getLdapUserSearchFilter(),
                idp.getLdapUsernameAttribute(), idp.getLdapEmailAttribute(),
                idp.getLdapPhoneAttribute(), idp.getLdapDisplayNameAttribute(),
                idp.getLdapGroupAttribute(), idp.getLdapUseSsl(), idp.getLdapStartTls(),
                idp.getLdapPageSize(),
                idp.getScopes(), idp.getAttributeMapping(),
                idp.getLinkingStrategy() != null ? idp.getLinkingStrategy().name() : null,
                idp.getDisplayOrder(), idp.getEnabled(),
                idp.getCreatedAt(), idp.getUpdatedAt()
        );
    }

    private String protectSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            return secret;
        }
        return secretCryptoService.protect(secret);
    }

    private boolean checkRequired(Map<String, Object> checks, String name, String value) {
        boolean ok = value != null && !value.isBlank();
        checks.put(name, ok ? "ok" : "missing");
        return ok;
    }

    private IdentityProvider.ProviderType parseProviderType(String providerType) {
        if (providerType == null || providerType.isBlank()) {
            throw new ValidationException("Provider Type 不能为空");
        }
        String normalized = providerType.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        try {
            return IdentityProvider.ProviderType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("不支持的 Provider Type: " + providerType);
        }
    }

    private IdentityProvider.LinkingStrategy parseLinkingStrategy(String linkingStrategy) {
        if (linkingStrategy == null || linkingStrategy.isBlank()) {
            return IdentityProvider.LinkingStrategy.AUTO_REGISTER;
        }
        String normalized = linkingStrategy.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        if ("AUTO".equals(normalized)) {
            normalized = IdentityProvider.LinkingStrategy.AUTO_REGISTER.name();
        } else if ("EMAIL".equals(normalized)) {
            normalized = IdentityProvider.LinkingStrategy.EMAIL_MATCH.name();
        }
        try {
            return IdentityProvider.LinkingStrategy.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("不支持的链接策略: " + linkingStrategy);
        }
    }

    private boolean checkUrl(Map<String, Object> checks, String name, String value) {
        if (!checkRequired(checks, name, value)) {
            return false;
        }
        try {
            URI uri = URI.create(value);
            boolean ok = "http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme());
            checks.put(name, ok ? "ok" : "invalid_scheme");
            return ok;
        } catch (IllegalArgumentException e) {
            checks.put(name, "invalid_url");
            return false;
        }
    }

    private boolean checkOptionalUrl(Map<String, Object> checks, String name, String value) {
        if (value == null || value.isBlank()) {
            checks.put(name, "skipped");
            return true;
        }
        return checkUrl(checks, name, value);
    }

    private boolean validateSamlProvider(Map<String, Object> checks, IdentityProvider idp, boolean probeRemote) {
        boolean metadataConfigured = idp.getSamlMetadataUrl() != null && !idp.getSamlMetadataUrl().isBlank();
        boolean valid;
        if (metadataConfigured) {
            valid = checkUrl(checks, "samlMetadataUrl", idp.getSamlMetadataUrl());
            checks.put("samlMetadataMode", "metadata_url");
        } else {
            valid = checkRequired(checks, "samlEntityId", idp.getSamlEntityId())
                    & checkUrl(checks, "samlSsoUrl", idp.getSamlSsoUrl())
                    & checkRequired(checks, "samlX509Certificate", idp.getSamlX509Certificate());
            checks.put("samlMetadataMode", "manual");
        }
        valid &= checkOptionalUrl(checks, "samlSloUrl", idp.getSamlSloUrl());
        valid &= checkOptionalUrl(checks, "samlAcsUrl", idp.getSamlAcsUrl());
        if (idp.getSamlSpEntityId() == null || idp.getSamlSpEntityId().isBlank()) {
            checks.put("samlSpEntityId", "optional");
        } else {
            checks.put("samlSpEntityId", "ok");
        }
        if (idp.getSamlNameIdFormat() == null || idp.getSamlNameIdFormat().isBlank()) {
            checks.put("samlNameIdFormat", "optional");
        } else {
            checks.put("samlNameIdFormat", "ok");
        }
        if (valid && probeRemote) {
            if (metadataConfigured) {
                valid &= probeUrl(checks, "samlMetadataUrlReachable", idp.getSamlMetadataUrl());
            } else {
                valid &= probeUrl(checks, "samlSsoUrlReachable", idp.getSamlSsoUrl());
            }
        } else {
            checks.put("remoteProbe", "skipped");
        }
        return valid;
    }

    private boolean probeUrl(Map<String, Object> checks, String name, String value) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(value))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            boolean ok = response.statusCode() < 500;
            checks.put(name, response.statusCode());
            return ok;
        } catch (Exception e) {
            checks.put(name, e.getClass().getSimpleName() + ": " + e.getMessage());
            return false;
        }
    }
}
