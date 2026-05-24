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
import com.armorauth.data.entity.UserInfo;
import com.armorauth.data.repository.IdentityProviderRepository;
import com.armorauth.data.repository.UserInfoRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.StartTlsRequest;
import javax.naming.ldap.StartTlsResponse;
import java.net.URI;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * LDAP/AD bind, search, and user import support.
 */
@Service
public class LdapDirectorySyncService {

    private static final int DEFAULT_MAX_RESULTS = 200;
    private static final int HARD_MAX_RESULTS = 1000;

    private final IdentityProviderRepository identityProviderRepository;
    private final UserInfoRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicyService;
    private final SecretCryptoService secretCryptoService;
    private final AuditEventService auditEventService;
    private final SecureRandom secureRandom = new SecureRandom();

    public LdapDirectorySyncService(IdentityProviderRepository identityProviderRepository,
                                    UserInfoRepository userRepository,
                                    PasswordEncoder passwordEncoder,
                                    PasswordPolicyService passwordPolicyService,
                                    SecretCryptoService secretCryptoService,
                                    AuditEventService auditEventService) {
        this.identityProviderRepository = identityProviderRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyService = passwordPolicyService;
        this.secretCryptoService = secretCryptoService;
        this.auditEventService = auditEventService;
    }

    public boolean validateConfiguration(Map<String, Object> checks, IdentityProvider provider) {
        boolean valid = true;
        valid &= checkRequired(checks, "ldapUrl", provider.getLdapUrl());
        valid &= checkLdapUrl(checks, "ldapUrlScheme", provider.getLdapUrl());
        valid &= checkRequired(checks, "ldapBaseDn", provider.getLdapBaseDn());
        checks.put("ldapBindDn", hasText(provider.getLdapBindDn()) ? "ok" : "anonymous");
        if (hasText(provider.getLdapBindDn()) && !hasText(provider.getLdapBindPassword())) {
            checks.put("ldapBindPassword", "missing");
            valid = false;
        } else {
            checks.put("ldapBindPassword", hasText(provider.getLdapBindPassword()) ? "configured" : "anonymous");
        }
        checks.put("ldapUserSearchBase", hasText(provider.getLdapUserSearchBase()) ? "ok" : "base_dn");
        checks.put("ldapUserSearchFilter", hasText(provider.getLdapUserSearchFilter()) ? "ok" : "default_person");
        checks.put("ldapUsernameAttribute", hasText(provider.getLdapUsernameAttribute()) ? "ok" : "default_uid");
        checks.put("ldapEmailAttribute", hasText(provider.getLdapEmailAttribute()) ? "ok" : "default_mail");
        checks.put("ldapDisplayNameAttribute", hasText(provider.getLdapDisplayNameAttribute()) ? "ok" : "default_displayName");
        checks.put("ldapUseSsl", Boolean.TRUE.equals(provider.getLdapUseSsl()));
        checks.put("ldapStartTls", Boolean.TRUE.equals(provider.getLdapStartTls()));
        return valid;
    }

    public boolean probe(Map<String, Object> checks, IdentityProvider provider, int maxResults) {
        InitialLdapContext context = null;
        StartTlsResponse tls = null;
        try {
            context = openContext(provider);
            if (Boolean.TRUE.equals(provider.getLdapStartTls())) {
                tls = (StartTlsResponse) context.extendedOperation(new StartTlsRequest());
                tls.negotiate();
                checks.put("ldapStartTlsHandshake", "ok");
            }
            checks.put("ldapBind", "ok");

            LdapSearchConfig config = searchConfig(provider, maxResults);
            SearchControls controls = searchControls(config);
            int count = 0;
            NamingEnumeration<SearchResult> results =
                    context.search(config.searchBase(), config.searchFilter(), controls);
            while (results.hasMore() && count < config.maxResults()) {
                results.next();
                count++;
            }
            results.close();
            checks.put("ldapSearchBaseEffective", config.searchBase());
            checks.put("ldapSearchFilterEffective", config.searchFilter());
            checks.put("ldapSearchSampleCount", count);
            return true;
        } catch (Exception e) {
            checks.put("ldapProbeError", e.getClass().getSimpleName() + ": " + e.getMessage());
            return false;
        } finally {
            closeQuietly(tls);
            closeQuietly(context);
        }
    }

    @Transactional
    public IdentityProviderDTO.LdapSyncResponse syncUsers(String providerId,
                                                          IdentityProviderDTO.LdapSyncRequest request) {
        IdentityProvider provider = identityProviderRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("身份源", providerId));
        if (provider.getProviderType() != IdentityProvider.ProviderType.LDAP) {
            throw new ValidationException("只有 LDAP/AD 身份源支持目录同步");
        }

        boolean dryRun = request == null || !Boolean.FALSE.equals(request.dryRun());
        int maxResults = normalizeMaxResults(request != null ? request.maxResults() : null);
        MutableSyncStats stats = new MutableSyncStats(provider.getId(), provider.getProviderName(), dryRun);

        InitialLdapContext context = null;
        StartTlsResponse tls = null;
        try {
            context = openContext(provider);
            if (Boolean.TRUE.equals(provider.getLdapStartTls())) {
                tls = (StartTlsResponse) context.extendedOperation(new StartTlsRequest());
                tls.negotiate();
            }

            LdapSearchConfig config = searchConfig(provider, maxResults);
            stats.samples.put("searchBase", config.searchBase());
            stats.samples.put("searchFilter", config.searchFilter());
            stats.samples.put("usernameAttribute", config.usernameAttribute());

            NamingEnumeration<SearchResult> results =
                    context.search(config.searchBase(), config.searchFilter(), searchControls(config));
            while (results.hasMore() && stats.scanned < config.maxResults()) {
                SearchResult result = results.next();
                syncEntry(provider, config, result.getAttributes(), stats, dryRun);
            }
            results.close();
        } catch (Exception e) {
            stats.failed++;
            stats.samples.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            closeQuietly(tls);
            closeQuietly(context);
        }

        auditEventService.record(dryRun ? "LDAP_USERS_SYNC_DRY_RUN" : "LDAP_USERS_SYNCED",
                AuditContext.getCurrentPrincipal(), "identity_provider", provider.getId(),
                "LDAP sync " + (dryRun ? "dry run" : "executed")
                        + ": scanned=" + stats.scanned
                        + ", created=" + stats.created
                        + ", updated=" + stats.updated
                        + ", skipped=" + stats.skipped
                        + ", failed=" + stats.failed,
                AuditContext.getClientIp());

        return stats.toResponse();
    }

    private void syncEntry(IdentityProvider provider,
                           LdapSearchConfig config,
                           Attributes attributes,
                           MutableSyncStats stats,
                           boolean dryRun) throws NamingException {
        stats.scanned++;
        String username = clean(attributeValue(attributes, config.usernameAttribute()));
        if (!hasText(username)) {
            stats.skipped++;
            addSample(stats, "skipped", "missing username attribute " + config.usernameAttribute());
            return;
        }

        String email = clean(attributeValue(attributes, config.emailAttribute()));
        String phone = clean(attributeValue(attributes, config.phoneAttribute()));
        String displayName = clean(attributeValue(attributes, config.displayNameAttribute()));
        if (!hasText(displayName)) {
            displayName = clean(attributeValue(attributes, "cn"));
        }
        if (!hasText(displayName)) {
            displayName = username;
        }

        UserInfo existing = userRepository.findByUsernameIgnoreCase(username).orElse(null);
        if (existing == null) {
            stats.wouldCreate++;
            addSample(stats, "create", username);
            if (!dryRun) {
                UserInfo user = new UserInfo();
                user.setUsername(username);
                user.setDisplayName(displayName);
                user.setEmail(email);
                user.setPhone(phone);
                user.setPassword(passwordEncoder.encode(generatePassword()));
                user.setStatus(0);
                user.setEmailVerified(false);
                user.setPhoneVerified(false);
                user.setFailedLoginAttempts(0);
                user.setCreateTime(Instant.now());
                user.setPasswordChangedAt(Instant.now());
                user.setProfile("{\"source\":\"ldap\",\"identityProviderId\":\"" + provider.getId() + "\"}");
                userRepository.save(user);
                stats.created++;
            }
            return;
        }

        boolean changed = updateIfChanged(existing, displayName, email, phone);
        if (changed) {
            stats.wouldUpdate++;
            addSample(stats, "update", username);
            if (!dryRun) {
                existing.setUpdateTime(Instant.now());
                userRepository.save(existing);
                stats.updated++;
            }
        } else {
            stats.skipped++;
        }
    }

    private boolean updateIfChanged(UserInfo user, String displayName, String email, String phone) {
        boolean changed = false;
        if (hasText(displayName) && !displayName.equals(user.getDisplayName())) {
            user.setDisplayName(displayName);
            changed = true;
        }
        if (email != null && !email.equals(user.getEmail())) {
            user.setEmail(email);
            changed = true;
        }
        if (phone != null && !phone.equals(user.getPhone())) {
            user.setPhone(phone);
            changed = true;
        }
        return changed;
    }

    private InitialLdapContext openContext(IdentityProvider provider) throws NamingException {
        Hashtable<String, Object> environment = new Hashtable<>();
        environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        environment.put(Context.PROVIDER_URL, provider.getLdapUrl());
        environment.put("com.sun.jndi.ldap.connect.timeout", "5000");
        environment.put("com.sun.jndi.ldap.read.timeout", "10000");
        if (hasText(provider.getLdapBindDn())) {
            environment.put(Context.SECURITY_AUTHENTICATION, "simple");
            environment.put(Context.SECURITY_PRINCIPAL, provider.getLdapBindDn());
            environment.put(Context.SECURITY_CREDENTIALS, secretCryptoService.reveal(provider.getLdapBindPassword()));
        } else {
            environment.put(Context.SECURITY_AUTHENTICATION, "none");
        }
        return new InitialLdapContext(environment, null);
    }

    private SearchControls searchControls(LdapSearchConfig config) {
        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        controls.setCountLimit(config.maxResults());
        controls.setReturningAttributes(new String[] {
                config.usernameAttribute(),
                config.emailAttribute(),
                config.phoneAttribute(),
                config.displayNameAttribute(),
                config.groupAttribute(),
                "cn"
        });
        return controls;
    }

    private LdapSearchConfig searchConfig(IdentityProvider provider, int maxResults) {
        String searchBase = effectiveSearchBase(provider.getLdapBaseDn(), provider.getLdapUserSearchBase());
        return new LdapSearchConfig(
                searchBase,
                defaultValue(provider.getLdapUserSearchFilter(), "(objectClass=person)"),
                defaultValue(provider.getLdapUsernameAttribute(), "uid"),
                defaultValue(provider.getLdapEmailAttribute(), "mail"),
                defaultValue(provider.getLdapPhoneAttribute(), "telephoneNumber"),
                defaultValue(provider.getLdapDisplayNameAttribute(), "displayName"),
                defaultValue(provider.getLdapGroupAttribute(), "memberOf"),
                normalizeMaxResults(maxResults));
    }

    private String effectiveSearchBase(String baseDn, String userSearchBase) {
        if (!hasText(userSearchBase)) {
            return baseDn;
        }
        if (!hasText(baseDn)) {
            return userSearchBase;
        }
        String normalizedSearch = userSearchBase.toLowerCase();
        String normalizedBase = baseDn.toLowerCase();
        if (normalizedSearch.endsWith(normalizedBase)) {
            return userSearchBase;
        }
        return userSearchBase + "," + baseDn;
    }

    private int normalizeMaxResults(Integer maxResults) {
        if (maxResults == null || maxResults <= 0) {
            return DEFAULT_MAX_RESULTS;
        }
        return Math.min(maxResults, HARD_MAX_RESULTS);
    }

    private String attributeValue(Attributes attributes, String name) throws NamingException {
        if (!hasText(name)) {
            return null;
        }
        Attribute attribute = attributes.get(name);
        if (attribute == null || attribute.size() == 0) {
            return null;
        }
        Object value = attribute.get(0);
        return value == null ? null : String.valueOf(value);
    }

    private boolean checkRequired(Map<String, Object> checks, String name, String value) {
        boolean ok = hasText(value);
        checks.put(name, ok ? "ok" : "missing");
        return ok;
    }

    private boolean checkLdapUrl(Map<String, Object> checks, String name, String value) {
        if (!hasText(value)) {
            checks.put(name, "missing");
            return false;
        }
        try {
            URI uri = URI.create(value);
            boolean ok = "ldap".equalsIgnoreCase(uri.getScheme()) || "ldaps".equalsIgnoreCase(uri.getScheme());
            checks.put(name, ok ? "ok" : "invalid_scheme");
            return ok;
        } catch (IllegalArgumentException e) {
            checks.put(name, "invalid_url");
            return false;
        }
    }

    private void addSample(MutableSyncStats stats, String key, String value) {
        @SuppressWarnings("unchecked")
        java.util.ArrayList<String> values =
                (java.util.ArrayList<String>) stats.samples.computeIfAbsent(key, ignored -> new java.util.ArrayList<String>());
        if (values.size() < 5) {
            values.add(value);
        }
    }

    private void closeQuietly(StartTlsResponse tls) {
        if (tls == null) {
            return;
        }
        try {
            tls.close();
        } catch (Exception ignored) {
            // best effort cleanup
        }
    }

    private void closeQuietly(InitialLdapContext context) {
        if (context == null) {
            return;
        }
        try {
            context.close();
        } catch (Exception ignored) {
            // best effort cleanup
        }
    }

    private String generatePassword() {
        for (int i = 0; i < 5; i++) {
            byte[] bytes = new byte[18];
            secureRandom.nextBytes(bytes);
            String candidate = "Ldap#9" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            if (passwordPolicyService.isValid(candidate)) {
                return candidate;
            }
        }
        return "Ldap#9" + Long.toHexString(secureRandom.nextLong()) + "Aa";
    }

    private String defaultValue(String value, String fallback) {
        return hasText(value) ? value : fallback;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private record LdapSearchConfig(
            String searchBase,
            String searchFilter,
            String usernameAttribute,
            String emailAttribute,
            String phoneAttribute,
            String displayNameAttribute,
            String groupAttribute,
            int maxResults
    ) {}

    private static class MutableSyncStats {
        private final String providerId;
        private final String providerName;
        private final boolean dryRun;
        private int scanned;
        private int wouldCreate;
        private int wouldUpdate;
        private int created;
        private int updated;
        private int skipped;
        private int failed;
        private final Map<String, Object> samples = new LinkedHashMap<>();

        private MutableSyncStats(String providerId, String providerName, boolean dryRun) {
            this.providerId = providerId;
            this.providerName = providerName;
            this.dryRun = dryRun;
        }

        private IdentityProviderDTO.LdapSyncResponse toResponse() {
            String message = dryRun
                    ? "LDAP/AD 用户同步预演完成"
                    : "LDAP/AD 用户同步完成";
            return new IdentityProviderDTO.LdapSyncResponse(
                    providerId,
                    providerName,
                    dryRun,
                    scanned,
                    wouldCreate,
                    wouldUpdate,
                    created,
                    updated,
                    skipped,
                    failed,
                    message,
                    samples
            );
        }
    }
}
