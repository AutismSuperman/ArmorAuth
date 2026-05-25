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
import com.armorauth.data.entity.IdentityProviderDisplayPreference;
import com.armorauth.data.entity.IdentityProvider;
import com.armorauth.data.repository.IdentityProviderDisplayPreferenceRepository;
import com.armorauth.data.repository.IdentityProviderRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class IdentityProviderService {

    private static final String CONFIG_ID_PREFIX = "config:";
    private static final String SOURCE_DATABASE = "DATABASE";
    private static final String SOURCE_CONFIG_FILE = "CONFIG_FILE";

    private final IdentityProviderRepository idpRepository;
    private final IdentityProviderDisplayPreferenceRepository displayPreferenceRepository;
    private final AuditEventService auditEventService;
    private final SecretCryptoService secretCryptoService;
    private final LdapDirectorySyncService ldapDirectorySyncService;
    private final Environment environment;
    private final ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider;

    public IdentityProviderService(IdentityProviderRepository idpRepository,
                                   IdentityProviderDisplayPreferenceRepository displayPreferenceRepository,
                                   AuditEventService auditEventService,
                                   SecretCryptoService secretCryptoService,
                                   LdapDirectorySyncService ldapDirectorySyncService,
                                   Environment environment,
                                   ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider) {
        this.idpRepository = idpRepository;
        this.displayPreferenceRepository = displayPreferenceRepository;
        this.auditEventService = auditEventService;
        this.secretCryptoService = secretCryptoService;
        this.ldapDirectorySyncService = ldapDirectorySyncService;
        this.environment = environment;
        this.clientRegistrationRepositoryProvider = clientRegistrationRepositoryProvider;
    }

    @Transactional(readOnly = true)
    public Page<IdentityProviderDTO.Response> listProviders(Pageable pageable) {
        return listProviders(pageable, null);
    }

    @Transactional(readOnly = true)
    public Page<IdentityProviderDTO.Response> listProviders(Pageable pageable, String source) {
        Map<String, ConfiguredProvider> configuredProviders = configuredProviders();
        Map<String, IdentityProviderDisplayPreference> preferences =
                displayPreferences(configuredProviders.keySet());

        List<IdentityProvider> databaseProviders =
                idpRepository.findAll(Sort.by(Sort.Direction.ASC, "displayOrder")
                        .and(Sort.by(Sort.Direction.ASC, "providerName")));
        Set<String> databaseRegistrationIds = new HashSet<>();
        for (IdentityProvider provider : databaseProviders) {
            databaseRegistrationIds.add(provider.getRegistrationId());
        }

        List<IdentityProviderDTO.Response> responses = new ArrayList<>();
        configuredProviders.values().stream()
                .filter(provider -> !databaseRegistrationIds.contains(provider.registrationId()))
                .map(provider -> toConfiguredResponse(provider, preferences.get(provider.registrationId())))
                .forEach(responses::add);
        databaseProviders.stream().map(this::toResponse).forEach(responses::add);

        responses.sort(Comparator
                .comparing((IdentityProviderDTO.Response response) ->
                        response.displayOrder() != null ? response.displayOrder() : 0)
                .thenComparing(response -> SOURCE_CONFIG_FILE.equals(response.source()) ? 0 : 1)
                .thenComparing(response -> response.providerName() != null ? response.providerName() : ""));

        responses = filterBySource(responses, source);

        if (pageable.isUnpaged()) {
            return new PageImpl<>(responses);
        }
        int start = (int) Math.min(pageable.getOffset(), responses.size());
        int end = Math.min(start + pageable.getPageSize(), responses.size());
        return new PageImpl<>(responses.subList(start, end), pageable, responses.size());
    }

    private List<IdentityProviderDTO.Response> filterBySource(List<IdentityProviderDTO.Response> responses,
                                                              String source) {
        if (source == null || source.isBlank() || "ALL".equalsIgnoreCase(source)) {
            return responses;
        }
        String normalized = source.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        if (!SOURCE_DATABASE.equals(normalized) && !SOURCE_CONFIG_FILE.equals(normalized)) {
            return responses;
        }
        return responses.stream()
                .filter(response -> normalized.equals(response.source()))
                .toList();
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
        idp.setIconKey(resolveIconKey(request.iconKey(), idp.getProviderType(), idp.getRegistrationId()));
        idp.setIconUrl(normalizeIconUrl(request.iconUrl()));
        idp.setDisplayOnLogin(request.displayOnLogin() == null || request.displayOnLogin());
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
        if (request.iconKey() != null) {
            idp.setIconKey(resolveIconKey(request.iconKey(), idp.getProviderType(), idp.getRegistrationId()));
        }
        if (request.iconUrl() != null) idp.setIconUrl(normalizeIconUrl(request.iconUrl()));
        if (request.displayOnLogin() != null) idp.setDisplayOnLogin(request.displayOnLogin());
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
    public IdentityProviderDTO.Response updateProviderLoginDisplay(String id, Boolean displayOnLogin) {
        if (displayOnLogin == null) {
            throw new ValidationException("登录页展示状态不能为空");
        }
        if (id != null && id.startsWith(CONFIG_ID_PREFIX)) {
            String registrationId = id.substring(CONFIG_ID_PREFIX.length());
            ConfiguredProvider configuredProvider = configuredProviders().get(registrationId);
            if (configuredProvider == null) {
                throw new ResourceNotFoundException("配置文件身份源", registrationId);
            }
            IdentityProviderDisplayPreference preference = displayPreferenceRepository
                    .findById(registrationId)
                    .orElseGet(() -> {
                        IdentityProviderDisplayPreference created = new IdentityProviderDisplayPreference();
                        created.setRegistrationId(registrationId);
                        return created;
                    });
            preference.setDisplayOnLogin(displayOnLogin);
            preference.setUpdatedAt(Instant.now());
            preference = displayPreferenceRepository.save(preference);

            String action = Boolean.TRUE.equals(displayOnLogin) ? "显示" : "隐藏";
            auditEventService.record("IDENTITY_PROVIDER_LOGIN_DISPLAY_CHANGED",
                    AuditContext.getCurrentPrincipal(), "identity_provider", registrationId,
                    action + "配置文件身份源: " + configuredProvider.providerName(), AuditContext.getClientIp());

            return toConfiguredResponse(configuredProvider, preference);
        }

        IdentityProvider idp = idpRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("身份源", id));
        idp.setDisplayOnLogin(displayOnLogin);
        idp.setUpdatedAt(Instant.now());
        idp = idpRepository.save(idp);

        String action = Boolean.TRUE.equals(displayOnLogin) ? "显示" : "隐藏";
        auditEventService.record("IDENTITY_PROVIDER_LOGIN_DISPLAY_CHANGED",
                AuditContext.getCurrentPrincipal(), "identity_provider", id,
                action + "身份源登录页入口: " + idp.getProviderName(), AuditContext.getClientIp());

        return toResponse(idp);
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
                resolvedRedirectUri(idp),
                resolvedAuthorizationGrantType(idp),
                resolvedClientAuthenticationMethod(idp),
                resolvedUserNameAttributeName(idp),
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
                idp.getScopes(), idp.getIconKey(), idp.getIconUrl(),
                idp.getDisplayOnLogin() == null || idp.getDisplayOnLogin(),
                idp.getAttributeMapping(),
                idp.getLinkingStrategy() != null ? idp.getLinkingStrategy().name() : null,
                idp.getDisplayOrder(), idp.getEnabled(),
                idp.getCreatedAt(), idp.getUpdatedAt(),
                SOURCE_DATABASE, false
        );
    }

    private IdentityProviderDTO.Response toConfiguredResponse(ConfiguredProvider provider,
                                                              IdentityProviderDisplayPreference preference) {
        boolean displayOnLogin = preference == null
                || preference.getDisplayOnLogin() == null
                || preference.getDisplayOnLogin();
        Instant updatedAt = preference != null ? preference.getUpdatedAt() : null;
        return new IdentityProviderDTO.Response(
                CONFIG_ID_PREFIX + provider.registrationId(),
                provider.providerName(),
                provider.providerType().name(),
                provider.registrationId(),
                provider.clientId(),
                provider.redirectUri(),
                provider.authorizationGrantType(),
                provider.clientAuthenticationMethod(),
                provider.userNameAttributeName(),
                provider.authorizationUri(),
                provider.tokenUri(),
                provider.userinfoUri(),
                provider.jwkSetUri(),
                null, null, null, null, null,
                null, null, null,
                null, null, null, false,
                null, null,
                null, null,
                null, null,
                null, null, null, null,
                provider.scopes(),
                provider.iconKey(),
                null,
                displayOnLogin,
                null,
                IdentityProvider.LinkingStrategy.AUTO_REGISTER.name(),
                0,
                true,
                null,
                updatedAt,
                SOURCE_CONFIG_FILE,
                true
        );
    }

    private Map<String, ConfiguredProvider> configuredProviders() {
        Binder binder = Binder.get(environment);
        Map<String, ConfiguredRegistrationProperties> registrations = binder
                .bind("spring.security.oauth2.client.registration",
                        Bindable.mapOf(String.class, ConfiguredRegistrationProperties.class))
                .orElse(Map.of());
        if (registrations.isEmpty()) {
            return Map.of();
        }
        Map<String, ConfiguredProviderProperties> providers = binder
                .bind("spring.security.oauth2.client.provider",
                        Bindable.mapOf(String.class, ConfiguredProviderProperties.class))
                .orElse(Map.of());
        Map<String, ClientRegistration> clientRegistrations = configuredClientRegistrations(registrations.keySet());

        Map<String, ConfiguredProvider> configuredProviders = new LinkedHashMap<>();
        registrations.forEach((registrationId, registration) -> {
            String providerId = firstText(
                    registration.getProvider(),
                    registrationId);
            ConfiguredProviderProperties provider = providers.getOrDefault(providerId, new ConfiguredProviderProperties());
            IdentityProvider.ProviderType providerType = inferProviderType(registrationId, providerId);
            String iconKey = normalizeIconKey(providerId);
            ClientRegistration clientRegistration = clientRegistrations.get(registrationId);
            String providerName = firstText(
                    registration.getClientName(),
                    configuredProviderName(registrationId, providerId, providerType),
                    clientRegistration != null ? clientRegistration.getClientName() : null);
            configuredProviders.put(registrationId, new ConfiguredProvider(
                    registrationId,
                    providerName,
                    providerType,
                    firstText(clientRegistration != null ? clientRegistration.getClientId() : null,
                            registration.getClientId()),
                    clientRegistration != null ? clientRegistration.getRedirectUri() : registration.getRedirectUri(),
                    clientRegistration != null && clientRegistration.getAuthorizationGrantType() != null
                            ? clientRegistration.getAuthorizationGrantType().getValue()
                            : registration.getAuthorizationGrantType(),
                    clientRegistration != null && clientRegistration.getClientAuthenticationMethod() != null
                            ? clientRegistration.getClientAuthenticationMethod().getValue()
                            : registration.getClientAuthenticationMethod(),
                    clientRegistration != null
                            ? clientRegistration.getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName()
                            : provider.getUserNameAttribute(),
                    firstText(
                            clientRegistration != null
                                    ? clientRegistration.getProviderDetails().getAuthorizationUri()
                                    : null,
                            registration.getAuthorizationUri(),
                            provider.getAuthorizationUri()),
                    firstText(
                            clientRegistration != null
                                    ? clientRegistration.getProviderDetails().getTokenUri()
                                    : null,
                            registration.getTokenUri(),
                            provider.getTokenUri()),
                    firstText(
                            clientRegistration != null
                                    ? clientRegistration.getProviderDetails().getUserInfoEndpoint().getUri()
                                    : null,
                            registration.getUserInfoUri(),
                            provider.getUserInfoUri()),
                    firstText(
                            clientRegistration != null ? clientRegistration.getProviderDetails().getJwkSetUri() : null,
                            registration.getJwkSetUri(),
                            provider.getJwkSetUri()),
                    clientRegistration != null ? String.join(",", clientRegistration.getScopes()) : scopeValue(registration.getScope()),
                    iconKey));
        });
        return configuredProviders;
    }

    private Map<String, ClientRegistration> configuredClientRegistrations(Collection<String> registrationIds) {
        ClientRegistrationRepository repository = this.clientRegistrationRepositoryProvider.getIfAvailable();
        if (!(repository instanceof Iterable<?> registrations) || registrationIds.isEmpty()) {
            return Map.of();
        }
        Map<String, ClientRegistration> clientRegistrations = new LinkedHashMap<>();
        for (Object registration : registrations) {
            if (registration instanceof ClientRegistration clientRegistration
                    && registrationIds.contains(clientRegistration.getRegistrationId())) {
                clientRegistrations.put(clientRegistration.getRegistrationId(), clientRegistration);
            }
        }
        return clientRegistrations;
    }

    private Map<String, IdentityProviderDisplayPreference> displayPreferences(Collection<String> registrationIds) {
        if (registrationIds.isEmpty()) {
            return Map.of();
        }
        Map<String, IdentityProviderDisplayPreference> preferences = new HashMap<>();
        displayPreferenceRepository.findAllById(registrationIds)
                .forEach(preference -> preferences.put(preference.getRegistrationId(), preference));
        return preferences;
    }

    private IdentityProvider.ProviderType inferProviderType(String registrationId, String providerId) {
        String candidate = firstText(providerId, registrationId);
        if (candidate != null) {
            String normalized = candidate.trim().toUpperCase(Locale.ROOT).replace('-', '_');
            try {
                return IdentityProvider.ProviderType.valueOf(normalized);
            } catch (IllegalArgumentException ignored) {
                // OAuth2ClientProperties commonly names generic OIDC/OAuth providers by brand.
            }
        }
        return IdentityProvider.ProviderType.OIDC;
    }

    private String configuredProviderName(String registrationId, String providerId,
                                          IdentityProvider.ProviderType providerType) {
        String normalized = normalizeIconKey(firstText(providerId, registrationId));
        return switch (normalized) {
            case "github" -> "GitHub";
            case "google" -> "Google";
            case "facebook" -> "Facebook";
            case "microsoft" -> "Microsoft";
            case "gitlab" -> "GitLab";
            case "discord" -> "Discord";
            case "slack" -> "Slack";
            case "linkedin" -> "LinkedIn";
            case "apple" -> "Apple";
            case "weibo" -> "微博";
            case "baidu" -> "百度";
            case "oschina" -> "OSChina";
            case "douyin" -> "抖音";
            case "wechat", "weixin" -> "微信";
            case "wecom" -> "企业微信";
            case "dingtalk" -> "钉钉";
            case "feishu" -> "飞书";
            case "alipay" -> "支付宝";
            case "qq" -> "QQ";
            case "gitee" -> "Gitee";
            default -> providerType != IdentityProvider.ProviderType.OIDC
                    ? providerType.name()
                    : registrationId;
        };
    }

    private String firstText(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String string) {
            return string;
        }
        return String.valueOf(value);
    }

    private String scopeValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Iterable<?> iterable) {
            List<String> scopes = new ArrayList<>();
            iterable.forEach(scope -> {
                if (scope != null && !String.valueOf(scope).isBlank()) {
                    scopes.add(String.valueOf(scope).trim());
                }
            });
            return String.join(",", scopes);
        }
        if (value.getClass().isArray()) {
            Object[] values = (Object[]) value;
            List<String> scopes = new ArrayList<>();
            for (Object scope : values) {
                if (scope != null && !String.valueOf(scope).isBlank()) {
                    scopes.add(String.valueOf(scope).trim());
                }
            }
            return String.join(",", scopes);
        }
        return stringValue(value);
    }

    private record ConfiguredProvider(
            String registrationId,
            String providerName,
            IdentityProvider.ProviderType providerType,
            String clientId,
            String redirectUri,
            String authorizationGrantType,
            String clientAuthenticationMethod,
            String userNameAttributeName,
            String authorizationUri,
            String tokenUri,
            String userinfoUri,
            String jwkSetUri,
            String scopes,
            String iconKey
    ) {}

    private static final class ConfiguredRegistrationProperties {

        private String provider;
        private String clientId;
        private String clientName;
        private String redirectUri;
        private String authorizationGrantType;
        private String clientAuthenticationMethod;
        private String authorizationUri;
        private String tokenUri;
        private String userInfoUri;
        private String jwkSetUri;
        private List<String> scope;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientName() {
            return clientName;
        }

        public void setClientName(String clientName) {
            this.clientName = clientName;
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }

        public String getAuthorizationGrantType() {
            return authorizationGrantType;
        }

        public void setAuthorizationGrantType(String authorizationGrantType) {
            this.authorizationGrantType = authorizationGrantType;
        }

        public String getClientAuthenticationMethod() {
            return clientAuthenticationMethod;
        }

        public void setClientAuthenticationMethod(String clientAuthenticationMethod) {
            this.clientAuthenticationMethod = clientAuthenticationMethod;
        }

        public String getAuthorizationUri() {
            return authorizationUri;
        }

        public void setAuthorizationUri(String authorizationUri) {
            this.authorizationUri = authorizationUri;
        }

        public String getTokenUri() {
            return tokenUri;
        }

        public void setTokenUri(String tokenUri) {
            this.tokenUri = tokenUri;
        }

        public String getUserInfoUri() {
            return userInfoUri;
        }

        public void setUserInfoUri(String userInfoUri) {
            this.userInfoUri = userInfoUri;
        }

        public String getJwkSetUri() {
            return jwkSetUri;
        }

        public void setJwkSetUri(String jwkSetUri) {
            this.jwkSetUri = jwkSetUri;
        }

        public List<String> getScope() {
            return scope;
        }

        public void setScope(List<String> scope) {
            this.scope = scope;
        }
    }

    private static final class ConfiguredProviderProperties {

        private String authorizationUri;
        private String tokenUri;
        private String userInfoUri;
        private String jwkSetUri;
        private String userNameAttribute;

        public String getAuthorizationUri() {
            return authorizationUri;
        }

        public void setAuthorizationUri(String authorizationUri) {
            this.authorizationUri = authorizationUri;
        }

        public String getTokenUri() {
            return tokenUri;
        }

        public void setTokenUri(String tokenUri) {
            this.tokenUri = tokenUri;
        }

        public String getUserInfoUri() {
            return userInfoUri;
        }

        public void setUserInfoUri(String userInfoUri) {
            this.userInfoUri = userInfoUri;
        }

        public String getJwkSetUri() {
            return jwkSetUri;
        }

        public void setJwkSetUri(String jwkSetUri) {
            this.jwkSetUri = jwkSetUri;
        }

        public String getUserNameAttribute() {
            return userNameAttribute;
        }

        public void setUserNameAttribute(String userNameAttribute) {
            this.userNameAttribute = userNameAttribute;
        }
    }

    private String resolvedRedirectUri(IdentityProvider idp) {
        if (idp.getProviderType() == IdentityProvider.ProviderType.SAML) {
            return idp.getSamlAcsUrl();
        }
        if (idp.getProviderType() == IdentityProvider.ProviderType.LDAP) {
            return null;
        }
        return "{baseUrl}/login/oauth2/code/{registrationId}";
    }

    private String resolvedAuthorizationGrantType(IdentityProvider idp) {
        return isOAuthProvider(idp) ? "authorization_code" : null;
    }

    private String resolvedClientAuthenticationMethod(IdentityProvider idp) {
        return isOAuthProvider(idp) ? "client_secret_basic" : null;
    }

    private String resolvedUserNameAttributeName(IdentityProvider idp) {
        if (!isOAuthProvider(idp)) {
            return null;
        }
        return switch (idp.getProviderType()) {
            case WECHAT, DINGTALK, QQ -> "openid";
            case WECOM -> "userid";
            case FEISHU -> "user_id";
            case GITEE -> "id";
            default -> "sub";
        };
    }

    private boolean isOAuthProvider(IdentityProvider idp) {
        return idp.getProviderType() != IdentityProvider.ProviderType.SAML
                && idp.getProviderType() != IdentityProvider.ProviderType.LDAP;
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

    private String resolveIconKey(String iconKey, IdentityProvider.ProviderType providerType, String registrationId) {
        if (iconKey != null && !iconKey.isBlank()) {
            return normalizeIconKey(iconKey);
        }
        if (providerType != null) {
            return normalizeIconKey(providerType.name());
        }
        if (registrationId != null && !registrationId.isBlank()) {
            return normalizeIconKey(registrationId);
        }
        return "custom";
    }

    private String normalizeIconKey(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private String normalizeIconUrl(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
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
