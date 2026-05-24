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

import com.armorauth.common.audit.AuditContext;
import com.armorauth.data.entity.Role;
import com.armorauth.data.entity.UserInfo;
import com.armorauth.data.entity.UserRole;
import com.armorauth.data.repository.RoleRepository;
import com.armorauth.data.repository.UserInfoRepository;
import com.armorauth.data.repository.UserRoleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SCIM 2.0 user provisioning facade backed by the local user directory.
 */
@Service
public class ScimUserService {

    public static final String USER_SCHEMA = "urn:ietf:params:scim:schemas:core:2.0:User";
    public static final String GROUP_SCHEMA = "urn:ietf:params:scim:schemas:core:2.0:Group";
    public static final String LIST_SCHEMA = "urn:ietf:params:scim:api:messages:2.0:ListResponse";
    public static final String ERROR_SCHEMA = "urn:ietf:params:scim:api:messages:2.0:Error";

    private static final Pattern USERNAME_EQ_FILTER =
            Pattern.compile("(?i)^userName\\s+eq\\s+['\"]([^'\"]+)['\"]$");
    private static final Pattern USERNAME_CO_FILTER =
            Pattern.compile("(?i)^userName\\s+co\\s+['\"]([^'\"]+)['\"]$");
    private static final Pattern EMAIL_EQ_FILTER =
            Pattern.compile("(?i)^(?:emails\\.value|email)\\s+eq\\s+['\"]([^'\"]+)['\"]$");
    private static final Pattern DISPLAY_NAME_EQ_FILTER =
            Pattern.compile("(?i)^displayName\\s+eq\\s+['\"]([^'\"]+)['\"]$");
    private static final Pattern DISPLAY_NAME_CO_FILTER =
            Pattern.compile("(?i)^displayName\\s+co\\s+['\"]([^'\"]+)['\"]$");
    private static final Pattern MEMBER_VALUE_FILTER =
            Pattern.compile("(?i)^members\\[value\\s+eq\\s+['\"]([^'\"]+)['\"]\\]$");

    private final UserInfoRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicyService;
    private final AuditEventService auditEventService;
    private final SecureRandom secureRandom = new SecureRandom();

    public ScimUserService(UserInfoRepository userRepository,
                           UserRoleRepository userRoleRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder,
                           PasswordPolicyService passwordPolicyService,
                           AuditEventService auditEventService) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyService = passwordPolicyService;
        this.auditEventService = auditEventService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listUsers(int startIndex, int count, String filter, String baseUrl) {
        int normalizedStart = Math.max(startIndex, 1);
        int normalizedCount = Math.max(Math.min(count, 100), 0);
        if (normalizedCount == 0) {
            return listResponse(List.of(), userRepository.count(), normalizedStart, 0);
        }

        Pageable pageable = PageRequest.of(
                Math.max((normalizedStart - 1) / normalizedCount, 0),
                normalizedCount,
                Sort.by(Sort.Direction.ASC, "username"));

        if (!hasText(filter)) {
            Page<UserInfo> page = userRepository.findAll(pageable);
            return listResponse(toResources(page.getContent(), baseUrl), page.getTotalElements(),
                    normalizedStart, page.getNumberOfElements());
        }

        Matcher usernameEq = USERNAME_EQ_FILTER.matcher(filter.trim());
        if (usernameEq.matches()) {
            List<UserInfo> users = userRepository.findByUsernameIgnoreCase(usernameEq.group(1))
                    .map(List::of)
                    .orElseGet(List::of);
            return listResponse(toResources(users, baseUrl), users.size(), normalizedStart, users.size());
        }

        Matcher usernameCo = USERNAME_CO_FILTER.matcher(filter.trim());
        if (usernameCo.matches()) {
            Page<UserInfo> page = userRepository.findByUsernameContainingIgnoreCase(usernameCo.group(1), pageable);
            return listResponse(toResources(page.getContent(), baseUrl), page.getTotalElements(),
                    normalizedStart, page.getNumberOfElements());
        }

        Matcher emailEq = EMAIL_EQ_FILTER.matcher(filter.trim());
        if (emailEq.matches()) {
            List<UserInfo> users = userRepository.findByEmailIgnoreCase(emailEq.group(1))
                    .map(List::of)
                    .orElseGet(List::of);
            return listResponse(toResources(users, baseUrl), users.size(), normalizedStart, users.size());
        }

        throw new ScimException(HttpStatus.BAD_REQUEST, "invalidFilter",
                "Supported filters: userName eq, userName co, emails.value eq.");
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUser(String id, String baseUrl) {
        UserInfo user = userRepository.findById(id)
                .orElseThrow(() -> notFound(id));
        return toResource(user, baseUrl);
    }

    @Transactional
    public Map<String, Object> createUser(Map<String, Object> request, String baseUrl) {
        String username = clean(asString(readField(request, "userName")));
        if (!hasText(username)) {
            throw new ScimException(HttpStatus.BAD_REQUEST, "invalidValue", "userName is required.");
        }
        ensureUsernameAvailable(username, null);

        String email = extractEmail(request);
        ensureEmailAvailable(email, null);

        String rawPassword = clean(asString(readField(request, "password")));
        if (!hasText(rawPassword)) {
            rawPassword = generatePassword();
        }
        validatePassword(rawPassword);

        Instant now = Instant.now();
        UserInfo user = new UserInfo();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setDisplayName(resolveDisplayName(request, username));
        user.setEmail(email);
        user.setPhone(extractPhone(request));
        user.setStatus(asActive(readField(request, "active")).orElse(true) ? 0 : 2);
        user.setEmailVerified(false);
        user.setPhoneVerified(false);
        user.setFailedLoginAttempts(0);
        user.setCreateTime(now);
        user.setPasswordChangedAt(now);
        user = userRepository.save(user);

        auditEventService.record("SCIM_USER_CREATED",
                AuditContext.getCurrentPrincipal(), "user", user.getId(),
                "SCIM created user: " + username, AuditContext.getClientIp());

        return toResource(user, baseUrl);
    }

    @Transactional
    public Map<String, Object> replaceUser(String id, Map<String, Object> request, String baseUrl) {
        UserInfo user = userRepository.findById(id)
                .orElseThrow(() -> notFound(id));
        applyReplace(user, request);
        user.setUpdateTime(Instant.now());
        user = userRepository.save(user);

        auditEventService.record("SCIM_USER_REPLACED",
                AuditContext.getCurrentPrincipal(), "user", user.getId(),
                "SCIM replaced user: " + user.getUsername(), AuditContext.getClientIp());

        return toResource(user, baseUrl);
    }

    @Transactional
    public Map<String, Object> patchUser(String id, Map<String, Object> request, String baseUrl) {
        UserInfo user = userRepository.findById(id)
                .orElseThrow(() -> notFound(id));

        Object operationsObject = readField(request, "Operations");
        if (!(operationsObject instanceof List<?> operations) || operations.isEmpty()) {
            throw new ScimException(HttpStatus.BAD_REQUEST, "invalidSyntax", "PATCH Operations are required.");
        }

        for (Object operationObject : operations) {
            if (!(operationObject instanceof Map<?, ?> operation)) {
                throw new ScimException(HttpStatus.BAD_REQUEST, "invalidSyntax", "PATCH operation must be an object.");
            }
            applyPatchOperation(user, operation);
        }

        user.setUpdateTime(Instant.now());
        user = userRepository.save(user);

        auditEventService.record("SCIM_USER_PATCHED",
                AuditContext.getCurrentPrincipal(), "user", user.getId(),
                "SCIM patched user: " + user.getUsername(), AuditContext.getClientIp());

        return toResource(user, baseUrl);
    }

    @Transactional
    public void deleteUser(String id) {
        UserInfo user = userRepository.findById(id)
                .orElseThrow(() -> notFound(id));
        String username = user.getUsername();
        userRoleRepository.deleteByUserId(id);
        userRepository.delete(user);

        auditEventService.record("SCIM_USER_DELETED",
                AuditContext.getCurrentPrincipal(), "user", id,
                "SCIM deleted user: " + username, AuditContext.getClientIp());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listGroups(int startIndex, int count, String filter, String baseUrl) {
        int normalizedStart = Math.max(startIndex, 1);
        int normalizedCount = Math.max(Math.min(count, 100), 0);
        if (normalizedCount == 0) {
            return listResponse(List.of(), roleRepository.count(), normalizedStart, 0);
        }

        Pageable pageable = PageRequest.of(
                Math.max((normalizedStart - 1) / normalizedCount, 0),
                normalizedCount,
                Sort.by(Sort.Direction.ASC, "roleCode"));

        if (!hasText(filter)) {
            Page<Role> page = roleRepository.findAll(pageable);
            return listResponse(toGroupResources(page.getContent(), baseUrl), page.getTotalElements(),
                    normalizedStart, page.getNumberOfElements());
        }

        Matcher displayNameEq = DISPLAY_NAME_EQ_FILTER.matcher(filter.trim());
        if (displayNameEq.matches()) {
            List<Role> roles = roleRepository.findByRoleCodeIgnoreCase(displayNameEq.group(1))
                    .map(List::of)
                    .orElseGet(List::of);
            return listResponse(toGroupResources(roles, baseUrl), roles.size(), normalizedStart, roles.size());
        }

        Matcher displayNameCo = DISPLAY_NAME_CO_FILTER.matcher(filter.trim());
        if (displayNameCo.matches()) {
            Page<Role> page = roleRepository.findByRoleCodeContainingIgnoreCase(displayNameCo.group(1), pageable);
            return listResponse(toGroupResources(page.getContent(), baseUrl), page.getTotalElements(),
                    normalizedStart, page.getNumberOfElements());
        }

        throw new ScimException(HttpStatus.BAD_REQUEST, "invalidFilter",
                "Supported group filters: displayName eq, displayName co.");
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getGroup(String id, String baseUrl) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> groupNotFound(id));
        return toGroupResource(role, baseUrl);
    }

    @Transactional
    public Map<String, Object> createGroup(Map<String, Object> request, String baseUrl) {
        String displayName = clean(asString(readField(request, "displayName")));
        validateGroupDisplayName(displayName);
        ensureGroupDisplayNameAvailable(displayName, null);

        Role role = new Role();
        role.setRoleCode(displayName);
        role.setRoleName(displayName);
        role.setDescription("SCIM managed group");
        role.setBuiltin(false);
        role = roleRepository.save(role);
        replaceGroupMembers(role.getId(), request);

        auditEventService.record("SCIM_GROUP_CREATED",
                AuditContext.getCurrentPrincipal(), "role", role.getId(),
                "SCIM created group: " + role.getRoleCode(), AuditContext.getClientIp());

        return toGroupResource(role, baseUrl);
    }

    @Transactional
    public Map<String, Object> replaceGroup(String id, Map<String, Object> request, String baseUrl) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> groupNotFound(id));
        ensureMutableGroup(role);

        Object displayNameValue = readField(request, "displayName");
        if (displayNameValue != null) {
            String displayName = clean(asString(displayNameValue));
            validateGroupDisplayName(displayName);
            ensureGroupDisplayNameAvailable(displayName, role.getId());
            role.setRoleCode(displayName);
            role.setRoleName(displayName);
            role = roleRepository.save(role);
        }
        replaceGroupMembers(role.getId(), request);

        auditEventService.record("SCIM_GROUP_REPLACED",
                AuditContext.getCurrentPrincipal(), "role", role.getId(),
                "SCIM replaced group: " + role.getRoleCode(), AuditContext.getClientIp());

        return toGroupResource(role, baseUrl);
    }

    @Transactional
    public Map<String, Object> patchGroup(String id, Map<String, Object> request, String baseUrl) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> groupNotFound(id));
        ensureMutableGroup(role);

        Object operationsObject = readField(request, "Operations");
        if (!(operationsObject instanceof List<?> operations) || operations.isEmpty()) {
            throw new ScimException(HttpStatus.BAD_REQUEST, "invalidSyntax", "PATCH Operations are required.");
        }
        for (Object operationObject : operations) {
            if (!(operationObject instanceof Map<?, ?> operation)) {
                throw new ScimException(HttpStatus.BAD_REQUEST, "invalidSyntax", "PATCH operation must be an object.");
            }
            applyGroupPatchOperation(role, operation);
        }

        role = roleRepository.save(role);
        auditEventService.record("SCIM_GROUP_PATCHED",
                AuditContext.getCurrentPrincipal(), "role", role.getId(),
                "SCIM patched group: " + role.getRoleCode(), AuditContext.getClientIp());

        return toGroupResource(role, baseUrl);
    }

    @Transactional
    public void deleteGroup(String id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> groupNotFound(id));
        ensureMutableGroup(role);
        String displayName = role.getRoleCode();
        userRoleRepository.findByRoleId(id)
                .forEach(binding -> userRoleRepository.deleteById(binding.getId()));
        roleRepository.delete(role);

        auditEventService.record("SCIM_GROUP_DELETED",
                AuditContext.getCurrentPrincipal(), "role", id,
                "SCIM deleted group: " + displayName, AuditContext.getClientIp());
    }

    public Map<String, Object> serviceProviderConfig() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("schemas", List.of("urn:ietf:params:scim:schemas:core:2.0:ServiceProviderConfig"));
        config.put("documentationUri", "/docs/api-reference.md#scim-20");
        config.put("patch", Map.of("supported", true));
        config.put("bulk", Map.of("supported", false, "maxOperations", 0, "maxPayloadSize", 0));
        config.put("filter", Map.of("supported", true, "maxResults", 100));
        config.put("changePassword", Map.of("supported", true));
        config.put("sort", Map.of("supported", false));
        config.put("etag", Map.of("supported", false));
        config.put("authenticationSchemes", List.of(Map.of(
                "type", "httpbasic",
                "name", "HTTP Basic",
                "description", "ArmorAuth admin HTTP Basic authentication",
                "specUri", "https://www.rfc-editor.org/rfc/rfc7617",
                "primary", true)));
        return config;
    }

    public Map<String, Object> schemas() {
        return listResponse(List.of(Map.of(
                "id", USER_SCHEMA,
                "name", "User",
                "description", "ArmorAuth user directory SCIM schema",
                "attributes", List.of(
                        schemaAttribute("userName", "string", true, true),
                        schemaAttribute("name", "complex", false, false),
                        schemaAttribute("displayName", "string", false, false),
                        schemaAttribute("active", "boolean", false, false),
                        schemaAttribute("emails", "complex", false, false),
                        schemaAttribute("phoneNumbers", "complex", false, false),
                        schemaAttribute("password", "string", false, false))),
                Map.of(
                        "id", GROUP_SCHEMA,
                        "name", "Group",
                        "description", "ArmorAuth role directory SCIM group schema",
                        "attributes", List.of(
                                schemaAttribute("displayName", "string", true, true),
                                schemaAttribute("members", "complex", false, false)))), 2, 1, 2);
    }

    public Map<String, Object> resourceTypes(String baseUrl) {
        return listResponse(List.of(Map.of(
                "schemas", List.of("urn:ietf:params:scim:schemas:core:2.0:ResourceType"),
                "id", "User",
                "name", "User",
                "endpoint", "/Users",
                "description", "ArmorAuth user account",
                "schema", USER_SCHEMA,
                "schemaExtensions", List.of(),
                "meta", Map.of(
                        "resourceType", "ResourceType",
                        "location", baseUrl + "/ResourceTypes/User")),
                Map.of(
                        "schemas", List.of("urn:ietf:params:scim:schemas:core:2.0:ResourceType"),
                        "id", "Group",
                        "name", "Group",
                        "endpoint", "/Groups",
                        "description", "ArmorAuth role group",
                        "schema", GROUP_SCHEMA,
                        "schemaExtensions", List.of(),
                        "meta", Map.of(
                                "resourceType", "ResourceType",
                                "location", baseUrl + "/ResourceTypes/Group"))), 2, 1, 2);
    }

    private Map<String, Object> schemaAttribute(String name, String type, boolean required, boolean uniquenessServer) {
        Map<String, Object> attribute = new LinkedHashMap<>();
        attribute.put("name", name);
        attribute.put("type", type);
        attribute.put("multiValued", "emails".equals(name) || "phoneNumbers".equals(name) || "members".equals(name));
        attribute.put("required", required);
        attribute.put("caseExact", false);
        attribute.put("mutability", "password".equals(name) ? "writeOnly" : "readWrite");
        attribute.put("returned", "password".equals(name) ? "never" : "default");
        attribute.put("uniqueness", uniquenessServer ? "server" : "none");
        return attribute;
    }

    private void applyReplace(UserInfo user, Map<String, Object> request) {
        Object usernameValue = readField(request, "userName");
        if (usernameValue != null) {
            String username = clean(asString(usernameValue));
            if (!hasText(username)) {
                throw new ScimException(HttpStatus.BAD_REQUEST, "invalidValue", "userName cannot be blank.");
            }
            ensureUsernameAvailable(username, user.getId());
            user.setUsername(username);
        }

        String displayName = resolveDisplayName(request, user.getUsername());
        user.setDisplayName(displayName);

        String email = extractEmail(request);
        ensureEmailAvailable(email, user.getId());
        user.setEmail(email);
        user.setPhone(extractPhone(request));

        asActive(readField(request, "active")).ifPresent(active -> user.setStatus(active ? 0 : 2));

        String rawPassword = clean(asString(readField(request, "password")));
        if (hasText(rawPassword)) {
            setPassword(user, rawPassword);
        }
    }

    private void applyPatchOperation(UserInfo user, Map<?, ?> operation) {
        String op = clean(asString(readField(operation, "op")));
        if (!hasText(op)) {
            op = "replace";
        }
        String path = clean(asString(readField(operation, "path")));
        Object value = readField(operation, "value");

        switch (op.toLowerCase(Locale.ROOT)) {
            case "add", "replace" -> applyPatchReplace(user, path, value);
            case "remove" -> applyPatchRemove(user, path);
            default -> throw new ScimException(HttpStatus.BAD_REQUEST, "invalidSyntax",
                    "Unsupported PATCH op: " + op);
        }
    }

    private void applyPatchReplace(UserInfo user, String path, Object value) {
        if (!hasText(path)) {
            if (!(value instanceof Map<?, ?> mapValue)) {
                throw new ScimException(HttpStatus.BAD_REQUEST, "invalidValue",
                        "PATCH value must be an object when path is omitted.");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> request = (Map<String, Object>) mapValue;
            applyReplace(user, request);
            return;
        }

        String normalizedPath = path.toLowerCase(Locale.ROOT);
        switch (normalizedPath) {
            case "username" -> {
                String username = clean(asString(value));
                if (!hasText(username)) {
                    throw new ScimException(HttpStatus.BAD_REQUEST, "invalidValue", "userName cannot be blank.");
                }
                ensureUsernameAvailable(username, user.getId());
                user.setUsername(username);
            }
            case "displayname" -> user.setDisplayName(defaultDisplayName(clean(asString(value)), user.getUsername()));
            case "name" -> {
                if (value instanceof Map<?, ?> nameMap) {
                    user.setDisplayName(defaultDisplayName(resolveName(nameMap), user.getUsername()));
                }
            }
            case "active" -> user.setStatus(asActive(value)
                    .orElseThrow(() -> new ScimException(HttpStatus.BAD_REQUEST, "invalidValue",
                            "active must be a boolean.")) ? 0 : 2);
            case "emails", "emails.value" -> {
                String email = value instanceof List<?> ? extractEmail(Map.of("emails", value)) : clean(asString(value));
                ensureEmailAvailable(email, user.getId());
                user.setEmail(email);
            }
            case "phonenumbers", "phonenumbers.value" -> {
                String phone = value instanceof List<?> ? extractPhone(Map.of("phoneNumbers", value)) : clean(asString(value));
                user.setPhone(phone);
            }
            case "password" -> setPassword(user, clean(asString(value)));
            default -> throw new ScimException(HttpStatus.BAD_REQUEST, "noTarget",
                    "Unsupported PATCH path: " + path);
        }
    }

    private void applyPatchRemove(UserInfo user, String path) {
        if (!hasText(path)) {
            throw new ScimException(HttpStatus.BAD_REQUEST, "noTarget", "PATCH remove requires a path.");
        }
        String normalizedPath = path.toLowerCase(Locale.ROOT);
        switch (normalizedPath) {
            case "emails", "emails.value" -> user.setEmail(null);
            case "phonenumbers", "phonenumbers.value" -> user.setPhone(null);
            default -> throw new ScimException(HttpStatus.BAD_REQUEST, "mutability",
                    "Only emails and phoneNumbers can be removed.");
        }
    }

    private void applyGroupPatchOperation(Role role, Map<?, ?> operation) {
        String op = clean(asString(readField(operation, "op")));
        if (!hasText(op)) {
            op = "replace";
        }
        String path = clean(asString(readField(operation, "path")));
        Object value = readField(operation, "value");

        switch (op.toLowerCase(Locale.ROOT)) {
            case "add" -> applyGroupPatchAdd(role, path, value);
            case "replace" -> applyGroupPatchReplace(role, path, value);
            case "remove" -> applyGroupPatchRemove(role, path, value);
            default -> throw new ScimException(HttpStatus.BAD_REQUEST, "invalidSyntax",
                    "Unsupported PATCH op: " + op);
        }
    }

    private void applyGroupPatchAdd(Role role, String path, Object value) {
        if (!hasText(path)) {
            if (value instanceof Map<?, ?> mapValue) {
                Object members = readField(mapValue, "members");
                if (members != null) {
                    addGroupMembers(role.getId(), members);
                    return;
                }
            }
            throw new ScimException(HttpStatus.BAD_REQUEST, "noTarget", "Group add supports members only.");
        }
        if ("members".equals(path.toLowerCase(Locale.ROOT))) {
            addGroupMembers(role.getId(), value);
            return;
        }
        throw new ScimException(HttpStatus.BAD_REQUEST, "noTarget", "Unsupported group add path: " + path);
    }

    private void applyGroupPatchReplace(Role role, String path, Object value) {
        if (!hasText(path)) {
            if (!(value instanceof Map<?, ?> mapValue)) {
                throw new ScimException(HttpStatus.BAD_REQUEST, "invalidValue",
                        "PATCH value must be an object when path is omitted.");
            }
            Object displayNameValue = readField(mapValue, "displayName");
            if (displayNameValue != null) {
                updateGroupDisplayName(role, clean(asString(displayNameValue)));
            }
            Object members = readField(mapValue, "members");
            if (members != null) {
                replaceGroupMembers(role.getId(), Map.of("members", members));
            }
            return;
        }

        String normalizedPath = path.toLowerCase(Locale.ROOT);
        switch (normalizedPath) {
            case "displayname" -> updateGroupDisplayName(role, clean(asString(value)));
            case "members" -> {
                clearGroupMembers(role.getId());
                if (value != null) {
                    addGroupMembers(role.getId(), value);
                }
            }
            default -> throw new ScimException(HttpStatus.BAD_REQUEST, "noTarget",
                    "Unsupported group replace path: " + path);
        }
    }

    private void applyGroupPatchRemove(Role role, String path, Object value) {
        if (!hasText(path)) {
            throw new ScimException(HttpStatus.BAD_REQUEST, "noTarget", "Group remove requires a path.");
        }
        String normalizedPath = path.toLowerCase(Locale.ROOT);
        if ("members".equals(normalizedPath)) {
            if (value == null) {
                clearGroupMembers(role.getId());
            } else {
                removeGroupMembers(role.getId(), value);
            }
            return;
        }

        Matcher memberMatcher = MEMBER_VALUE_FILTER.matcher(path);
        if (memberMatcher.matches()) {
            removeSingleGroupMember(role.getId(), memberMatcher.group(1));
            return;
        }

        throw new ScimException(HttpStatus.BAD_REQUEST, "mutability", "Only group members can be removed.");
    }

    private void updateGroupDisplayName(Role role, String displayName) {
        validateGroupDisplayName(displayName);
        ensureGroupDisplayNameAvailable(displayName, role.getId());
        role.setRoleCode(displayName);
        role.setRoleName(displayName);
    }

    private void replaceGroupMembers(String roleId, Map<String, Object> request) {
        Object members = readField(request, "members");
        if (members == null) {
            return;
        }
        clearGroupMembers(roleId);
        addGroupMembers(roleId, members);
    }

    private void clearGroupMembers(String roleId) {
        userRoleRepository.findByRoleId(roleId)
                .forEach(binding -> userRoleRepository.deleteById(binding.getId()));
    }

    private void addGroupMembers(String roleId, Object members) {
        for (String userId : extractMemberIds(members)) {
            if (!userRepository.existsById(userId)) {
                throw new ScimException(HttpStatus.BAD_REQUEST, "invalidValue",
                        "Group member user does not exist: " + userId);
            }
            if (!userRoleRepository.existsByUserIdAndRoleId(userId, roleId)) {
                UserRole binding = new UserRole();
                binding.setUserId(userId);
                binding.setRoleId(roleId);
                userRoleRepository.save(binding);
            }
        }
    }

    private void removeGroupMembers(String roleId, Object members) {
        for (String userId : extractMemberIds(members)) {
            removeSingleGroupMember(roleId, userId);
        }
    }

    private void removeSingleGroupMember(String roleId, String userId) {
        userRoleRepository.findByRoleId(roleId).stream()
                .filter(binding -> userId.equals(binding.getUserId()))
                .forEach(binding -> userRoleRepository.deleteById(binding.getId()));
    }

    private List<String> extractMemberIds(Object members) {
        if (members == null) {
            return List.of();
        }
        List<?> memberList = members instanceof List<?> list ? list : List.of(members);
        List<String> userIds = new java.util.ArrayList<>();
        for (Object member : memberList) {
            String userId;
            if (member instanceof Map<?, ?> memberMap) {
                userId = clean(asString(readField(memberMap, "value")));
            } else {
                userId = clean(asString(member));
            }
            if (hasText(userId)) {
                userIds.add(userId);
            }
        }
        return userIds;
    }

    private List<Map<String, Object>> toGroupResources(List<Role> roles, String baseUrl) {
        return roles.stream()
                .map(role -> toGroupResource(role, baseUrl))
                .toList();
    }

    private Map<String, Object> toGroupResource(Role role, String baseUrl) {
        Map<String, Object> resource = new LinkedHashMap<>();
        resource.put("schemas", List.of(GROUP_SCHEMA));
        resource.put("id", role.getId());
        resource.put("displayName", role.getRoleCode());

        List<UserRole> bindings = userRoleRepository.findByRoleId(role.getId());
        if (!bindings.isEmpty()) {
            List<String> userIds = bindings.stream()
                    .map(UserRole::getUserId)
                    .toList();
            Map<String, UserInfo> users = new LinkedHashMap<>();
            userRepository.findAllById(userIds)
                    .forEach(user -> users.put(user.getId(), user));
            List<Map<String, Object>> members = new java.util.ArrayList<>();
            for (String userId : userIds) {
                UserInfo user = users.get(userId);
                Map<String, Object> member = new LinkedHashMap<>();
                member.put("value", userId);
                member.put("$ref", baseUrl + "/Users/" + userId);
                if (user != null) {
                    member.put("display", user.getUsername());
                }
                members.add(member);
            }
            resource.put("members", members);
        }

        resource.put("meta", meta("Group", baseUrl + "/Groups/" + role.getId(), null, null));
        return resource;
    }

    private void validateGroupDisplayName(String displayName) {
        if (!hasText(displayName)) {
            throw new ScimException(HttpStatus.BAD_REQUEST, "invalidValue", "displayName is required.");
        }
        if (displayName.length() > 100) {
            throw new ScimException(HttpStatus.BAD_REQUEST, "invalidValue",
                    "displayName cannot exceed 100 characters.");
        }
    }

    private void ensureGroupDisplayNameAvailable(String displayName, String currentRoleId) {
        roleRepository.findByRoleCodeIgnoreCase(displayName)
                .filter(existing -> !existing.getId().equals(currentRoleId))
                .ifPresent(existing -> {
                    throw new ScimException(HttpStatus.CONFLICT, "uniqueness",
                            "displayName already exists: " + displayName);
                });
    }

    private void ensureMutableGroup(Role role) {
        if (Boolean.TRUE.equals(role.getBuiltin())) {
            throw new ScimException(HttpStatus.FORBIDDEN, "mutability",
                    "Built-in groups are read-only through SCIM.");
        }
    }

    private ScimException groupNotFound(String id) {
        return new ScimException(HttpStatus.NOT_FOUND, null, "SCIM group not found: " + id);
    }

    private List<Map<String, Object>> toResources(List<UserInfo> users, String baseUrl) {
        return users.stream()
                .map(user -> toResource(user, baseUrl))
                .toList();
    }

    private Map<String, Object> toResource(UserInfo user, String baseUrl) {
        Map<String, Object> resource = new LinkedHashMap<>();
        resource.put("schemas", List.of(USER_SCHEMA));
        resource.put("id", user.getId());
        resource.put("userName", user.getUsername());
        resource.put("displayName", user.getDisplayName());
        resource.put("active", user.getStatus() != null && user.getStatus() == 0);

        Map<String, Object> name = new LinkedHashMap<>();
        name.put("formatted", user.getDisplayName());
        putNameParts(name, user.getDisplayName());
        resource.put("name", name);

        if (hasText(user.getEmail())) {
            resource.put("emails", List.of(Map.of(
                    "value", user.getEmail(),
                    "type", "work",
                    "primary", true)));
        }
        if (hasText(user.getPhone())) {
            resource.put("phoneNumbers", List.of(Map.of(
                    "value", user.getPhone(),
                    "type", "mobile",
                    "primary", true)));
        }

        resource.put("meta", meta("User", baseUrl + "/Users/" + user.getId(),
                user.getCreateTime(), user.getUpdateTime()));
        return resource;
    }

    private void putNameParts(Map<String, Object> name, String displayName) {
        if (!hasText(displayName)) {
            return;
        }
        String[] parts = displayName.trim().split("\\s+", 2);
        name.put("givenName", parts[0]);
        if (parts.length > 1) {
            name.put("familyName", parts[1]);
        }
    }

    private Map<String, Object> meta(String resourceType, String location, Instant created, Instant updated) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("resourceType", resourceType);
        if (created != null) {
            meta.put("created", created);
        }
        Instant lastModified = updated != null ? updated : created;
        if (lastModified != null) {
            meta.put("lastModified", lastModified);
        }
        meta.put("location", location);
        return meta;
    }

    private Map<String, Object> listResponse(List<?> resources, long totalResults, int startIndex, int itemsPerPage) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("schemas", List.of(LIST_SCHEMA));
        response.put("totalResults", totalResults);
        response.put("startIndex", startIndex);
        response.put("itemsPerPage", itemsPerPage);
        response.put("Resources", resources);
        return response;
    }

    private String resolveDisplayName(Map<String, Object> request, String username) {
        String displayName = clean(asString(readField(request, "displayName")));
        if (hasText(displayName)) {
            return displayName;
        }
        Object nameValue = readField(request, "name");
        if (nameValue instanceof Map<?, ?> nameMap) {
            displayName = resolveName(nameMap);
            if (hasText(displayName)) {
                return displayName;
            }
        }
        return username;
    }

    private String resolveName(Map<?, ?> nameMap) {
        String formatted = clean(asString(readField(nameMap, "formatted")));
        if (hasText(formatted)) {
            return formatted;
        }
        String givenName = clean(asString(readField(nameMap, "givenName")));
        String familyName = clean(asString(readField(nameMap, "familyName")));
        if (hasText(givenName) && hasText(familyName)) {
            return givenName + " " + familyName;
        }
        return hasText(givenName) ? givenName : familyName;
    }

    private String extractEmail(Map<String, Object> request) {
        return extractMultiValue(request, "emails");
    }

    private String extractPhone(Map<String, Object> request) {
        return extractMultiValue(request, "phoneNumbers");
    }

    private String extractMultiValue(Map<String, Object> request, String fieldName) {
        Object value = readField(request, fieldName);
        if (value == null) {
            return null;
        }
        if (value instanceof String stringValue) {
            return clean(stringValue);
        }
        if (!(value instanceof List<?> values)) {
            throw new ScimException(HttpStatus.BAD_REQUEST, "invalidValue",
                    fieldName + " must be an array.");
        }

        String first = null;
        for (Object item : values) {
            if (item instanceof Map<?, ?> itemMap) {
                String itemValue = clean(asString(readField(itemMap, "value")));
                if (!hasText(itemValue)) {
                    continue;
                }
                if (first == null) {
                    first = itemValue;
                }
                if (Boolean.TRUE.equals(asActive(readField(itemMap, "primary")).orElse(false))) {
                    return itemValue;
                }
            } else {
                String itemValue = clean(asString(item));
                if (hasText(itemValue) && first == null) {
                    first = itemValue;
                }
            }
        }
        return first;
    }

    private Optional<Boolean> asActive(Object value) {
        if (value == null) {
            return Optional.empty();
        }
        if (value instanceof Boolean booleanValue) {
            return Optional.of(booleanValue);
        }
        if (value instanceof String stringValue && hasText(stringValue)) {
            return Optional.of(Boolean.parseBoolean(stringValue));
        }
        return Optional.empty();
    }

    private Object readField(Map<?, ?> map, String name) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() instanceof String key && key.equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private void ensureUsernameAvailable(String username, String currentUserId) {
        userRepository.findByUsernameIgnoreCase(username)
                .filter(existing -> !existing.getId().equals(currentUserId))
                .ifPresent(existing -> {
                    throw new ScimException(HttpStatus.CONFLICT, "uniqueness",
                            "userName already exists: " + username);
                });
    }

    private void ensureEmailAvailable(String email, String currentUserId) {
        if (!hasText(email)) {
            return;
        }
        userRepository.findByEmailIgnoreCase(email)
                .filter(existing -> !existing.getId().equals(currentUserId))
                .ifPresent(existing -> {
                    throw new ScimException(HttpStatus.CONFLICT, "uniqueness",
                            "email already exists: " + email);
                });
    }

    private void setPassword(UserInfo user, String rawPassword) {
        if (!hasText(rawPassword)) {
            throw new ScimException(HttpStatus.BAD_REQUEST, "invalidValue", "password cannot be blank.");
        }
        validatePassword(rawPassword);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setPasswordChangedAt(Instant.now());
    }

    private void validatePassword(String password) {
        List<String> errors = passwordPolicyService.validate(password);
        if (!errors.isEmpty()) {
            StringJoiner joiner = new StringJoiner("; ");
            errors.forEach(joiner::add);
            throw new ScimException(HttpStatus.BAD_REQUEST, "invalidValue",
                    "Password does not match policy: " + joiner);
        }
    }

    private String generatePassword() {
        for (int i = 0; i < 5; i++) {
            byte[] bytes = new byte[18];
            secureRandom.nextBytes(bytes);
            String candidate = "Scim#9" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            if (passwordPolicyService.isValid(candidate)) {
                return candidate;
            }
        }
        return "Scim#9" + Long.toHexString(secureRandom.nextLong()) + "Aa";
    }

    private ScimException notFound(String id) {
        return new ScimException(HttpStatus.NOT_FOUND, null, "SCIM user not found: " + id);
    }

    private String defaultDisplayName(String displayName, String username) {
        return hasText(displayName) ? displayName : username;
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

    public static class ScimException extends RuntimeException {
        private final HttpStatus status;
        private final String scimType;

        public ScimException(HttpStatus status, String scimType, String message) {
            super(message);
            this.status = status;
            this.scimType = scimType;
        }

        public HttpStatus status() {
            return status;
        }

        public String scimType() {
            return scimType;
        }
    }
}
