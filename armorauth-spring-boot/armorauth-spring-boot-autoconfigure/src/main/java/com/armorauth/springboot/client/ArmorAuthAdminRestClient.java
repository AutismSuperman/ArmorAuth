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
package com.armorauth.springboot.client;

import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;

/**
 * Lightweight Map-based client for ArmorAuth Admin API.
 *
 * @author fulin
 * @since 2026-06-21
 */
public class ArmorAuthAdminRestClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_BODY =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;

    public ArmorAuthAdminRestClient(RestClient restClient) {
        Assert.notNull(restClient, "RestClient must not be null");
        this.restClient = restClient;
    }

    public Map<String, Object> listApplications(int page, int size) {
        return get("/api/admin/v1/applications?page={page}&size={size}", page, size);
    }

    public Map<String, Object> getApplication(String id) {
        return get("/api/admin/v1/applications/{id}", id);
    }

    public Map<String, Object> createApplication(Map<String, Object> request) {
        return post("/api/admin/v1/applications", request);
    }

    public Map<String, Object> updateApplication(String id, Map<String, Object> request) {
        return put("/api/admin/v1/applications/{id}", request, id);
    }

    public Map<String, Object> rotateSecret(String id) {
        return post("/api/admin/v1/applications/{id}/secret:rotate", null, id);
    }

    public Map<String, Object> listUsers(int page, int size) {
        return get("/api/admin/v1/users?page={page}&size={size}", page, size);
    }

    public Map<String, Object> getUser(String id) {
        return get("/api/admin/v1/users/{id}", id);
    }

    public Map<String, Object> createUser(Map<String, Object> request) {
        return post("/api/admin/v1/users", request);
    }

    public Map<String, Object> updateUser(String id, Map<String, Object> request) {
        return put("/api/admin/v1/users/{id}", request, id);
    }

    public Map<String, Object> listOrganizations(int page, int size) {
        return get("/api/admin/v1/organizations?page={page}&size={size}", page, size);
    }

    public Map<String, Object> listRoles(int page, int size) {
        return get("/api/admin/v1/roles?page={page}&size={size}", page, size);
    }

    public Map<String, Object> listIdentityProviders(int page, int size) {
        return get("/api/admin/v1/identity-providers?page={page}&size={size}", page, size);
    }

    public Map<String, Object> listAuditEvents(int page, int size) {
        return get("/api/admin/v1/audit-events?page={page}&size={size}", page, size);
    }

    public Map<String, Object> get(String path, Object... uriVariables) {
        return restClient.get()
                .uri(path, uriVariables)
                .retrieve()
                .body(MAP_BODY);
    }

    public Map<String, Object> post(String path, Object body, Object... uriVariables) {
        return restClient.post()
                .uri(path, uriVariables)
                .body(body == null ? Map.of() : body)
                .retrieve()
                .body(MAP_BODY);
    }

    public Map<String, Object> put(String path, Object body, Object... uriVariables) {
        return restClient.put()
                .uri(path, uriVariables)
                .body(body == null ? Map.of() : body)
                .retrieve()
                .body(MAP_BODY);
    }

    public void delete(String path, Object... uriVariables) {
        restClient.delete()
                .uri(path, uriVariables)
                .retrieve()
                .toBodilessEntity();
    }
}
