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
package com.armorauth.client;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * ArmorAuth Admin API Java SDK
 * <p>
 * 提供对 ArmorAuth 管理 API 的 Java 客户端封装，支持：
 * - 应用管理（CRUD、密钥轮换）
 * - 用户管理（CRUD、状态变更、密码重置）
 * - 组织管理（CRUD、成员管理）
 * - Token 统计查询
 *
 * @author fulin
 * @since 2026-05-23
 */
public class ArmorAuthAdminClient {

    private final String baseUrl;
    private final RestTemplate restTemplate;
    private final HttpHeaders authHeaders;

    /**
     * 创建 Admin 客户端
     *
     * @param baseUrl  ArmorAuth 服务地址，例如 http://localhost:9000
     * @param username 管理员用户名
     * @param password 管理员密码
     */
    public ArmorAuthAdminClient(String baseUrl, String username, String password) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.restTemplate = new RestTemplate();
        this.authHeaders = new HttpHeaders();
        String credentials = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
        this.authHeaders.set("Authorization", "Basic " + credentials);
        this.authHeaders.setContentType(MediaType.APPLICATION_JSON);
    }

    // ========== 应用管理 ==========

    /**
     * 获取应用列表
     */
    public Map<String, Object> listApplications(int page, int size) {
        return get("/api/admin/v1/applications?page=" + page + "&size=" + size);
    }

    /**
     * 获取应用详情
     */
    public Map<String, Object> getApplication(String id) {
        return get("/api/admin/v1/applications/" + id);
    }

    /**
     * 创建应用
     */
    public Map<String, Object> createApplication(Map<String, Object> request) {
        return post("/api/admin/v1/applications", request);
    }

    /**
     * 更新应用
     */
    public Map<String, Object> updateApplication(String id, Map<String, Object> request) {
        return put("/api/admin/v1/applications/" + id, request);
    }

    /**
     * 轮换应用密钥
     */
    public Map<String, Object> rotateSecret(String id) {
        return post("/api/admin/v1/applications/" + id + "/secret:rotate", null);
    }

    /**
     * 更新应用状态
     */
    public void updateApplicationStatus(String id, boolean enabled) {
        patch("/api/admin/v1/applications/" + id + "/status", Map.of("enabled", enabled));
    }

    // ========== 用户管理 ==========

    /**
     * 获取用户列表
     */
    public Map<String, Object> listUsers(int page, int size) {
        return get("/api/admin/v1/users?page=" + page + "&size=" + size);
    }

    /**
     * 获取用户详情
     */
    public Map<String, Object> getUser(String id) {
        return get("/api/admin/v1/users/" + id);
    }

    /**
     * 创建用户
     */
    public Map<String, Object> createUser(Map<String, Object> request) {
        return post("/api/admin/v1/users", request);
    }

    /**
     * 更新用户
     */
    public Map<String, Object> updateUser(String id, Map<String, Object> request) {
        return put("/api/admin/v1/users/" + id, request);
    }

    /**
     * 重置用户密码
     */
    public void resetPassword(String id, String newPassword) {
        post("/api/admin/v1/users/" + id + "/password:reset", Map.of("newPassword", newPassword));
    }

    /**
     * 锁定用户
     */
    public void lockUser(String id) {
        post("/api/admin/v1/users/" + id + "/lock", Map.of("durationMinutes", 30));
    }

    /**
     * 解锁用户
     */
    public void unlockUser(String id) {
        post("/api/admin/v1/users/" + id + "/unlock", null);
    }

    // ========== 组织管理 ==========

    /**
     * 获取组织列表
     */
    public Map<String, Object> listOrganizations(int page, int size) {
        return get("/api/admin/v1/organizations?page=" + page + "&size=" + size);
    }

    /**
     * 获取组织详情
     */
    public Map<String, Object> getOrganization(String id) {
        return get("/api/admin/v1/organizations/" + id);
    }

    /**
     * 创建组织
     */
    public Map<String, Object> createOrganization(Map<String, Object> request) {
        return post("/api/admin/v1/organizations", request);
    }

    /**
     * 获取组织成员
     */
    public Map<String, Object> listOrganizationMembers(String orgId, int page, int size) {
        return get("/api/admin/v1/organizations/" + orgId + "/members?page=" + page + "&size=" + size);
    }

    // ========== 角色管理 ==========

    /**
     * 获取角色列表
     */
    public Map<String, Object> listRoles(int page, int size) {
        return get("/api/admin/v1/roles?page=" + page + "&size=" + size);
    }

    /**
     * 创建角色
     */
    public Map<String, Object> createRole(Map<String, Object> request) {
        return post("/api/admin/v1/roles", request);
    }

    // ========== 权限管理 ==========

    /**
     * 获取权限列表
     */
    public Map<String, Object> listPermissions(int page, int size) {
        return get("/api/admin/v1/permissions?page=" + page + "&size=" + size);
    }

    /**
     * 创建权限
     */
    public Map<String, Object> createPermission(Map<String, Object> request) {
        return post("/api/admin/v1/permissions", request);
    }

    // ========== 审计日志 ==========

    /**
     * 获取审计日志
     */
    public Map<String, Object> listAuditEvents(int page, int size, String eventType, String principalName) {
        StringBuilder url = new StringBuilder("/api/admin/v1/audit-events?page=" + page + "&size=" + size);
        if (eventType != null) url.append("&eventType=").append(eventType);
        if (principalName != null) url.append("&principalName=").append(principalName);
        return get(url.toString());
    }

    // ========== 身份源管理 ==========

    /**
     * 获取身份源列表
     */
    public Map<String, Object> listIdentityProviders(int page, int size) {
        return get("/api/admin/v1/identity-providers?page=" + page + "&size=" + size);
    }

    /**
     * 创建身份源
     */
    public Map<String, Object> createIdentityProvider(Map<String, Object> request) {
        return post("/api/admin/v1/identity-providers", request);
    }

    // ========== Token 统计 ==========

    /**
     * 获取 Token 统计
     */
    public Map<String, Object> getTokenStatistics(String clientId, String from, String to) {
        return get("/api/admin/v1/token-statistics?clientId=" + clientId + "&from=" + from + "&to=" + to);
    }

    // ========== 内部方法 ==========

    private Map<String, Object> get(String path) {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + path, HttpMethod.GET, entity,
                new ParameterizedTypeReference<>() {});
        return response.getBody();
    }

    private Map<String, Object> post(String path, Object body) {
        HttpEntity<Object> entity = new HttpEntity<>(body, authHeaders);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + path, HttpMethod.POST, entity,
                new ParameterizedTypeReference<>() {});
        return response.getBody();
    }

    private Map<String, Object> put(String path, Object body) {
        HttpEntity<Object> entity = new HttpEntity<>(body, authHeaders);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + path, HttpMethod.PUT, entity,
                new ParameterizedTypeReference<>() {});
        return response.getBody();
    }

    private void patch(String path, Object body) {
        HttpEntity<Object> entity = new HttpEntity<>(body, authHeaders);
        restTemplate.exchange(baseUrl + path, HttpMethod.PATCH, entity,
                new ParameterizedTypeReference<Void>() {});
    }
}
