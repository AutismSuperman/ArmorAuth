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
package com.armorauth.admin.controller;

import com.armorauth.admin.service.ScimUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SCIM 2.0 provisioning API.
 */
@RestController
@RequestMapping(value = "/scim/v2", produces = {ScimController.SCIM_MEDIA_TYPE, MediaType.APPLICATION_JSON_VALUE})
public class ScimController {

    static final String SCIM_MEDIA_TYPE = "application/scim+json";

    private final ScimUserService scimUserService;

    public ScimController(ScimUserService scimUserService) {
        this.scimUserService = scimUserService;
    }

    @GetMapping("/ServiceProviderConfig")
    public ResponseEntity<Map<String, Object>> serviceProviderConfig() {
        return scimOk(scimUserService.serviceProviderConfig());
    }

    @GetMapping("/Schemas")
    public ResponseEntity<Map<String, Object>> schemas() {
        return scimOk(scimUserService.schemas());
    }

    @GetMapping("/ResourceTypes")
    public ResponseEntity<Map<String, Object>> resourceTypes(HttpServletRequest request) {
        return scimOk(scimUserService.resourceTypes(baseUrl(request)));
    }

    @GetMapping("/Users")
    public ResponseEntity<Map<String, Object>> listUsers(
            @RequestParam(name = "startIndex", defaultValue = "1") int startIndex,
            @RequestParam(name = "count", defaultValue = "20") int count,
            @RequestParam(name = "filter", required = false) String filter,
            HttpServletRequest request) {
        return scimOk(scimUserService.listUsers(startIndex, count, filter, baseUrl(request)));
    }

    @PostMapping(value = "/Users", consumes = {SCIM_MEDIA_TYPE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody Map<String, Object> body,
                                                          HttpServletRequest request) {
        Map<String, Object> resource = scimUserService.createUser(body, baseUrl(request));
        return ResponseEntity.created(URI.create(metaLocation(resource)))
                .contentType(MediaType.valueOf(SCIM_MEDIA_TYPE))
                .body(resource);
    }

    @GetMapping("/Users/{id}")
    public ResponseEntity<Map<String, Object>> getUser(@PathVariable(name = "id") String id,
                                                       HttpServletRequest request) {
        return scimOk(scimUserService.getUser(id, baseUrl(request)));
    }

    @PutMapping(value = "/Users/{id}", consumes = {SCIM_MEDIA_TYPE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Map<String, Object>> replaceUser(@PathVariable(name = "id") String id,
                                                           @RequestBody Map<String, Object> body,
                                                           HttpServletRequest request) {
        return scimOk(scimUserService.replaceUser(id, body, baseUrl(request)));
    }

    @PatchMapping(value = "/Users/{id}", consumes = {SCIM_MEDIA_TYPE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Map<String, Object>> patchUser(@PathVariable(name = "id") String id,
                                                         @RequestBody Map<String, Object> body,
                                                         HttpServletRequest request) {
        return scimOk(scimUserService.patchUser(id, body, baseUrl(request)));
    }

    @DeleteMapping("/Users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable(name = "id") String id) {
        scimUserService.deleteUser(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .contentType(MediaType.valueOf(SCIM_MEDIA_TYPE))
                .build();
    }

    @GetMapping("/Groups")
    public ResponseEntity<Map<String, Object>> listGroups(
            @RequestParam(name = "startIndex", defaultValue = "1") int startIndex,
            @RequestParam(name = "count", defaultValue = "20") int count,
            @RequestParam(name = "filter", required = false) String filter,
            HttpServletRequest request) {
        return scimOk(scimUserService.listGroups(startIndex, count, filter, baseUrl(request)));
    }

    @PostMapping(value = "/Groups", consumes = {SCIM_MEDIA_TYPE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Map<String, Object>> createGroup(@RequestBody Map<String, Object> body,
                                                           HttpServletRequest request) {
        Map<String, Object> resource = scimUserService.createGroup(body, baseUrl(request));
        return ResponseEntity.created(URI.create(metaLocation(resource)))
                .contentType(MediaType.valueOf(SCIM_MEDIA_TYPE))
                .body(resource);
    }

    @GetMapping("/Groups/{id}")
    public ResponseEntity<Map<String, Object>> getGroup(@PathVariable(name = "id") String id,
                                                        HttpServletRequest request) {
        return scimOk(scimUserService.getGroup(id, baseUrl(request)));
    }

    @PutMapping(value = "/Groups/{id}", consumes = {SCIM_MEDIA_TYPE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Map<String, Object>> replaceGroup(@PathVariable(name = "id") String id,
                                                            @RequestBody Map<String, Object> body,
                                                            HttpServletRequest request) {
        return scimOk(scimUserService.replaceGroup(id, body, baseUrl(request)));
    }

    @PatchMapping(value = "/Groups/{id}", consumes = {SCIM_MEDIA_TYPE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Map<String, Object>> patchGroup(@PathVariable(name = "id") String id,
                                                          @RequestBody Map<String, Object> body,
                                                          HttpServletRequest request) {
        return scimOk(scimUserService.patchGroup(id, body, baseUrl(request)));
    }

    @DeleteMapping("/Groups/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable(name = "id") String id) {
        scimUserService.deleteGroup(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .contentType(MediaType.valueOf(SCIM_MEDIA_TYPE))
                .build();
    }

    @ExceptionHandler(ScimUserService.ScimException.class)
    public ResponseEntity<Map<String, Object>> handleScimException(ScimUserService.ScimException exception) {
        return scimError(exception.status(), exception.scimType(), exception.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException exception) {
        return scimError(HttpStatus.BAD_REQUEST, "invalidSyntax", "Request body is not valid JSON.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleInternal(Exception exception) {
        return scimError(HttpStatus.INTERNAL_SERVER_ERROR, null, exception.getMessage());
    }

    private ResponseEntity<Map<String, Object>> scimOk(Map<String, Object> body) {
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(SCIM_MEDIA_TYPE))
                .body(body);
    }

    private ResponseEntity<Map<String, Object>> scimError(HttpStatus status, String scimType, String detail) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("schemas", List.of(ScimUserService.ERROR_SCHEMA));
        body.put("status", String.valueOf(status.value()));
        if (scimType != null) {
            body.put("scimType", scimType);
        }
        body.put("detail", detail != null ? detail : status.getReasonPhrase());
        return ResponseEntity.status(status)
                .contentType(MediaType.valueOf(SCIM_MEDIA_TYPE))
                .body(body);
    }

    private String metaLocation(Map<String, Object> resource) {
        Object meta = resource.get("meta");
        if (meta instanceof Map<?, ?> metaMap) {
            Object location = metaMap.get("location");
            if (location != null) {
                return String.valueOf(location);
            }
        }
        throw new IllegalStateException("SCIM resource location is missing.");
    }

    private String baseUrl(HttpServletRequest request) {
        return ServletUriComponentsBuilder.fromRequest(request)
                .replacePath(request.getContextPath() + "/scim/v2")
                .replaceQuery(null)
                .build()
                .toUriString();
    }
}
