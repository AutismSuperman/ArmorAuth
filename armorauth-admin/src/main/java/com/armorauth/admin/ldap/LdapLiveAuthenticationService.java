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
package com.armorauth.admin.ldap;

import com.armorauth.crypto.SecretCryptoService;
import com.armorauth.data.entity.IdentityProvider;
import com.armorauth.data.entity.Role;
import com.armorauth.data.entity.UserInfo;
import com.armorauth.data.entity.UserRole;
import com.armorauth.data.repository.IdentityProviderRepository;
import com.armorauth.data.repository.RoleRepository;
import com.armorauth.data.repository.UserInfoRepository;
import com.armorauth.data.repository.UserRoleRepository;
import com.armorauth.details.DelegateUserDetailsService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
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
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class LdapLiveAuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(LdapLiveAuthenticationService.class);

    private final IdentityProviderRepository identityProviderRepository;
    private final UserInfoRepository userInfoRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final DelegateUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final SecretCryptoService secretCryptoService;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public LdapLiveAuthenticationService(IdentityProviderRepository identityProviderRepository,
                                         UserInfoRepository userInfoRepository,
                                         RoleRepository roleRepository,
                                         UserRoleRepository userRoleRepository,
                                         DelegateUserDetailsService userDetailsService,
                                         PasswordEncoder passwordEncoder,
                                         SecretCryptoService secretCryptoService,
                                         ObjectMapper objectMapper) {
        this.identityProviderRepository = identityProviderRepository;
        this.userInfoRepository = userInfoRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.secretCryptoService = secretCryptoService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Optional<UserDetails> authenticate(String username, String password) {
        Optional<UserInfo> existingUser = userInfoRepository.findByUsernameIgnoreCase(username);
        if (existingUser.isPresent() && !isLdapManaged(existingUser.get())) {
            return Optional.empty();
        }

        List<IdentityProvider> providers = identityProviderRepository.findByEnabledTrueOrderByDisplayOrderAsc()
                .stream()
                .filter(provider -> provider.getProviderType() == IdentityProvider.ProviderType.LDAP)
                .toList();
        if (providers.isEmpty()) {
            return Optional.empty();
        }

        for (IdentityProvider provider : providers) {
            if (!isReadyForLogin(provider)) {
                continue;
            }
            try {
                Optional<LdapPrincipal> principal = authenticate(provider, username, password);
                if (principal.isEmpty()) {
                    continue;
                }
                UserInfo localUser = upsertLocalUser(provider, principal.get());
                if (localUser.getStatus() != null && localUser.getStatus() != 0) {
                    throw new DisabledException("LDAP user is disabled locally");
                }
                applyRoleMapping(localUser.getId(), provider, principal.get());
                return Optional.of(userDetailsService.loadUserByUsername(localUser.getUsername()));
            } catch (DisabledException e) {
                throw e;
            } catch (Exception e) {
                log.warn("LDAP live authentication failed for provider={} username={}: {}",
                        provider.getRegistrationId(), mask(username), e.getMessage());
            }
        }
        return Optional.empty();
    }

    private Optional<LdapPrincipal> authenticate(IdentityProvider provider,
                                                String username,
                                                String password) throws Exception {
        LdapSearchConfig config = searchConfig(provider);
        SearchResult result;
        try (LdapContextSession context = openServiceContext(provider)) {
            SearchControls controls = searchControls(provider, config);
            NamingEnumeration<SearchResult> results =
                    context.context().search(config.searchBase(), userSearchFilter(config, username), controls);
            if (!results.hasMore()) {
                results.close();
                return Optional.empty();
            }
            result = results.next();
            results.close();
        }

        String dn = resolveDn(result, config.searchBase());
        if (!hasText(dn) || !bindAsUser(provider, dn, password)) {
            return Optional.empty();
        }

        Attributes attributes = result.getAttributes();
        String effectiveUsername = clean(firstAttribute(attributes, config.usernameAttribute()));
        if (!hasText(effectiveUsername)) {
            effectiveUsername = username;
        }
        String displayName = clean(firstAttribute(attributes, config.displayNameAttribute()));
        if (!hasText(displayName)) {
            displayName = clean(firstAttribute(attributes, "cn"));
        }
        if (!hasText(displayName)) {
            displayName = effectiveUsername;
        }

        return Optional.of(new LdapPrincipal(
                dn,
                effectiveUsername,
                clean(firstAttribute(attributes, config.emailAttribute())),
                clean(firstAttribute(attributes, config.phoneAttribute())),
                displayName,
                attributeValues(attributes, config.groupAttribute()),
                attributesToMap(attributes)));
    }

    private UserInfo upsertLocalUser(IdentityProvider provider, LdapPrincipal principal) {
        UserInfo user = userInfoRepository.findByUsernameIgnoreCase(principal.username()).orElse(null);
        Instant now = Instant.now();
        if (user == null) {
            user = new UserInfo();
            user.setUsername(principal.username());
            user.setPassword(passwordEncoder.encode(generatePassword()));
            user.setStatus(0);
            user.setEmailVerified(false);
            user.setPhoneVerified(false);
            user.setFailedLoginAttempts(0);
            user.setCreateTime(now);
            user.setPasswordChangedAt(now);
        }

        if (hasText(principal.displayName())) {
            user.setDisplayName(principal.displayName());
        }
        if (principal.email() != null) {
            user.setEmail(principal.email());
        }
        if (principal.phone() != null) {
            user.setPhone(principal.phone());
        }
        user.setProfile(ldapProfile(provider, principal));
        user.setUpdateTime(now);
        return userInfoRepository.save(user);
    }

    private void applyRoleMapping(String userId, IdentityProvider provider, LdapPrincipal principal) {
        Set<String> roleCodes = resolveRoleCodes(provider, principal);
        if (roleCodes.isEmpty()) {
            return;
        }

        for (String roleCode : roleCodes) {
            roleRepository.findByRoleCodeIgnoreCase(roleCode)
                    .map(Role::getId)
                    .filter(roleId -> !userRoleRepository.existsByUserIdAndRoleId(userId, roleId))
                    .ifPresent(roleId -> {
                        UserRole userRole = new UserRole();
                        userRole.setUserId(userId);
                        userRole.setRoleId(roleId);
                        userRoleRepository.save(userRole);
                        log.info("Mapped LDAP user {} to role {}", userId, roleCode);
                    });
        }
    }

    private Set<String> resolveRoleCodes(IdentityProvider provider, LdapPrincipal principal) {
        if (!hasText(provider.getAttributeMapping())) {
            return Set.of();
        }
        try {
            Map<String, Object> mapping = objectMapper.readValue(provider.getAttributeMapping(), new TypeReference<>() {});
            Object roles = mapping.get("roles");
            if (roles == null) {
                return Set.of();
            }

            Set<String> result = new LinkedHashSet<>();
            if (roles instanceof String || roles instanceof Collection<?>) {
                addRoleCodes(result, roles);
                return result;
            }
            if (!(roles instanceof Map<?, ?> roleMapping)) {
                return Set.of();
            }

            addRoleCodes(result, roleMapping.get("value"));
            addRoleCodes(result, roleMapping.get("default"));
            addRoleCodes(result, roleMapping.get("defaultRoles"));

            Object fromAttribute = roleMapping.get("fromAttribute");
            List<String> sourceValues = fromAttribute instanceof String attributeName
                    ? principal.attributeValues(attributeName)
                    : principal.groups();

            Map<?, ?> mappedGroups = firstMap(roleMapping, "mappings", "mapping", "groupRoleMapping", "groups");
            if (mappedGroups != null) {
                for (String value : sourceValues) {
                    for (Map.Entry<?, ?> entry : mappedGroups.entrySet()) {
                        if (entry.getKey() != null && groupMatches(value, entry.getKey().toString())) {
                            addRoleCodes(result, entry.getValue());
                        }
                    }
                }
            }

            if (Boolean.TRUE.equals(roleMapping.get("direct"))) {
                addRoleCodes(result, sourceValues);
            }
            return result;
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse LDAP role mapping for provider={}: {}",
                    provider.getRegistrationId(), e.getMessage());
            return Set.of();
        }
    }

    private Map<?, ?> firstMap(Map<?, ?> source, String... keys) {
        for (String key : keys) {
            Object value = source.get(key);
            if (value instanceof Map<?, ?> map) {
                return map;
            }
        }
        return null;
    }

    private boolean groupMatches(String groupValue, String expected) {
        if (!hasText(groupValue) || !hasText(expected)) {
            return false;
        }
        if (groupValue.equalsIgnoreCase(expected)) {
            return true;
        }
        String groupCn = cn(groupValue);
        return hasText(groupCn) && groupCn.equalsIgnoreCase(expected);
    }

    private String cn(String dn) {
        for (String part : dn.split(",")) {
            String trimmed = part.trim();
            if (trimmed.regionMatches(true, 0, "CN=", 0, 3)) {
                return trimmed.substring(3);
            }
        }
        return null;
    }

    private void addRoleCodes(Set<String> target, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                addRoleCodes(target, item);
            }
            return;
        }
        String text = value.toString();
        for (String part : text.split("[,;]")) {
            String roleCode = part.trim();
            if (!roleCode.isEmpty()) {
                target.add(roleCode);
            }
        }
    }

    private LdapContextSession openServiceContext(IdentityProvider provider) throws Exception {
        String bindDn = provider.getLdapBindDn();
        String bindPassword = hasText(provider.getLdapBindPassword())
                ? secretCryptoService.reveal(provider.getLdapBindPassword())
                : null;
        return openContext(provider, bindDn, bindPassword);
    }

    private boolean bindAsUser(IdentityProvider provider, String dn, String password) {
        try (LdapContextSession ignored = openContext(provider, dn, password)) {
            return true;
        } catch (Exception e) {
            log.debug("LDAP user bind failed for dn={}: {}", dn, e.getMessage());
            return false;
        }
    }

    private LdapContextSession openContext(IdentityProvider provider,
                                           String principal,
                                           String credentials) throws NamingException, IOException {
        boolean authenticated = hasText(principal);
        Hashtable<String, Object> environment = new Hashtable<>();
        environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        environment.put(Context.PROVIDER_URL, provider.getLdapUrl());
        environment.put("com.sun.jndi.ldap.connect.timeout", "5000");
        environment.put("com.sun.jndi.ldap.read.timeout", "10000");
        if (authenticated && !Boolean.TRUE.equals(provider.getLdapStartTls())) {
            environment.put(Context.SECURITY_AUTHENTICATION, "simple");
            environment.put(Context.SECURITY_PRINCIPAL, principal);
            environment.put(Context.SECURITY_CREDENTIALS, credentials != null ? credentials : "");
        } else {
            environment.put(Context.SECURITY_AUTHENTICATION, "none");
        }

        InitialLdapContext context = new InitialLdapContext(environment, null);
        StartTlsResponse tls = null;
        if (Boolean.TRUE.equals(provider.getLdapStartTls())) {
            tls = (StartTlsResponse) context.extendedOperation(new StartTlsRequest());
            tls.negotiate();
            if (authenticated) {
                context.addToEnvironment(Context.SECURITY_AUTHENTICATION, "simple");
                context.addToEnvironment(Context.SECURITY_PRINCIPAL, principal);
                context.addToEnvironment(Context.SECURITY_CREDENTIALS, credentials != null ? credentials : "");
                context.reconnect(null);
            }
        }
        return new LdapContextSession(context, tls);
    }

    private SearchControls searchControls(IdentityProvider provider, LdapSearchConfig config) {
        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        controls.setCountLimit(2);
        Set<String> attributes = new LinkedHashSet<>();
        attributes.add(config.usernameAttribute());
        attributes.add(config.emailAttribute());
        attributes.add(config.phoneAttribute());
        attributes.add(config.displayNameAttribute());
        attributes.add(config.groupAttribute());
        attributes.add("cn");
        roleFromAttribute(provider).ifPresent(attributes::add);
        controls.setReturningAttributes(attributes.stream().filter(this::hasText).toArray(String[]::new));
        return controls;
    }

    private Optional<String> roleFromAttribute(IdentityProvider provider) {
        if (!hasText(provider.getAttributeMapping())) {
            return Optional.empty();
        }
        try {
            Map<String, Object> mapping = objectMapper.readValue(provider.getAttributeMapping(), new TypeReference<>() {});
            Object roles = mapping.get("roles");
            if (roles instanceof Map<?, ?> roleMapping) {
                Object fromAttribute = roleMapping.get("fromAttribute");
                if (fromAttribute instanceof String value && hasText(value)) {
                    return Optional.of(value);
                }
            }
        } catch (JsonProcessingException ignored) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    private LdapSearchConfig searchConfig(IdentityProvider provider) {
        String usernameAttribute = defaultValue(provider.getLdapUsernameAttribute(), "uid");
        return new LdapSearchConfig(
                effectiveSearchBase(provider.getLdapBaseDn(), provider.getLdapUserSearchBase()),
                defaultValue(provider.getLdapUserSearchFilter(), "(objectClass=person)"),
                usernameAttribute,
                defaultValue(provider.getLdapEmailAttribute(), "mail"),
                defaultValue(provider.getLdapPhoneAttribute(), "telephoneNumber"),
                defaultValue(provider.getLdapDisplayNameAttribute(), "displayName"),
                defaultValue(provider.getLdapGroupAttribute(), "memberOf"));
    }

    private String userSearchFilter(LdapSearchConfig config, String username) {
        String escaped = escapeLdapFilter(username);
        String filter = config.searchFilter();
        if (filter.contains("{0}")) {
            return filter.replace("{0}", escaped);
        }
        if (filter.contains("%s")) {
            return String.format(Locale.ROOT, filter, escaped);
        }
        if (!filter.startsWith("(")) {
            filter = "(" + filter + ")";
        }
        return "(&" + filter + "(" + config.usernameAttribute() + "=" + escaped + "))";
    }

    private String escapeLdapFilter(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            switch (c) {
                case '\\' -> escaped.append("\\5c");
                case '*' -> escaped.append("\\2a");
                case '(' -> escaped.append("\\28");
                case ')' -> escaped.append("\\29");
                case '\u0000' -> escaped.append("\\00");
                default -> escaped.append(c);
            }
        }
        return escaped.toString();
    }

    private String resolveDn(SearchResult result, String searchBase) throws NamingException {
        try {
            return result.getNameInNamespace();
        } catch (UnsupportedOperationException e) {
            String name = result.getName();
            if (!hasText(name)) {
                return searchBase;
            }
            return result.isRelative() ? name + "," + searchBase : name;
        }
    }

    private String firstAttribute(Attributes attributes, String name) throws NamingException {
        List<String> values = attributeValues(attributes, name);
        return values.isEmpty() ? null : values.get(0);
    }

    private List<String> attributeValues(Attributes attributes, String name) throws NamingException {
        if (!hasText(name)) {
            return List.of();
        }
        Attribute attribute = attributes.get(name);
        if (attribute == null) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        NamingEnumeration<?> all = attribute.getAll();
        while (all.hasMore()) {
            Object value = all.next();
            if (value != null && hasText(value.toString())) {
                values.add(value.toString());
            }
        }
        all.close();
        return values;
    }

    private Map<String, Object> attributesToMap(Attributes attributes) throws NamingException {
        Map<String, Object> result = new LinkedHashMap<>();
        NamingEnumeration<? extends Attribute> all = attributes.getAll();
        while (all.hasMore()) {
            Attribute attribute = all.next();
            List<String> values = attributeValues(attributes, attribute.getID());
            if (values.size() == 1) {
                result.put(attribute.getID(), values.get(0));
            } else if (!values.isEmpty()) {
                result.put(attribute.getID(), values);
            }
        }
        all.close();
        return result;
    }

    private String ldapProfile(IdentityProvider provider, LdapPrincipal principal) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("source", "ldap");
        profile.put("identityProviderId", provider.getId());
        profile.put("registrationId", provider.getRegistrationId());
        profile.put("ldapDn", principal.dn());
        profile.put("ldapGroups", principal.groups());
        try {
            return objectMapper.writeValueAsString(profile);
        } catch (JsonProcessingException e) {
            return "{\"source\":\"ldap\"}";
        }
    }

    private String effectiveSearchBase(String baseDn, String userSearchBase) {
        if (!hasText(userSearchBase)) {
            return baseDn;
        }
        if (!hasText(baseDn)) {
            return userSearchBase;
        }
        String normalizedSearch = userSearchBase.toLowerCase(Locale.ROOT);
        String normalizedBase = baseDn.toLowerCase(Locale.ROOT);
        if (normalizedSearch.endsWith(normalizedBase)) {
            return userSearchBase;
        }
        return userSearchBase + "," + baseDn;
    }

    private boolean isReadyForLogin(IdentityProvider provider) {
        return hasText(provider.getLdapUrl()) && hasText(provider.getLdapBaseDn());
    }

    private boolean isLdapManaged(UserInfo user) {
        String profile = user.getProfile();
        return profile != null && profile.contains("\"source\":\"ldap\"");
    }

    private String generatePassword() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return "Ldap#9" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
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

    private String mask(String value) {
        if (!hasText(value)) {
            return "unknown";
        }
        if (value.length() <= 4) {
            return value;
        }
        return value.substring(0, 2) + "***" + value.substring(value.length() - 2);
    }

    private record LdapSearchConfig(
            String searchBase,
            String searchFilter,
            String usernameAttribute,
            String emailAttribute,
            String phoneAttribute,
            String displayNameAttribute,
            String groupAttribute
    ) {}

    private record LdapPrincipal(
            String dn,
            String username,
            String email,
            String phone,
            String displayName,
            List<String> groups,
            Map<String, Object> attributes
    ) {
        private List<String> attributeValues(String name) {
            Object value = attributes.get(name);
            if (value instanceof Collection<?> collection) {
                return collection.stream().map(String::valueOf).toList();
            }
            if (value != null) {
                return List.of(String.valueOf(value));
            }
            return List.of();
        }
    }

    private record LdapContextSession(InitialLdapContext context, StartTlsResponse tls) implements AutoCloseable {
        @Override
        public void close() {
            if (tls != null) {
                try {
                    tls.close();
                } catch (Exception ignored) {
                    // best effort cleanup
                }
            }
            try {
                context.close();
            } catch (Exception ignored) {
                // best effort cleanup
            }
        }
    }
}
