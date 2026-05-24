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

import com.armorauth.admin.dto.JwkDTO;
import com.armorauth.common.audit.AuditContext;
import com.armorauth.common.response.ApiResponse;
import com.armorauth.data.entity.JwkKey;
import com.armorauth.jose.PersistentJwkSource;
import com.armorauth.admin.service.AuditEventService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * JWK 密钥管理 API
 *
 * @author fulin
 * @since 2026-05-23
 */
@RestController
@RequestMapping("/api/admin/v1/jwk-keys")
public class JwkController {

    private final PersistentJwkSource persistentJwkSource;
    private final AuditEventService auditEventService;

    public JwkController(PersistentJwkSource persistentJwkSource,
                         AuditEventService auditEventService) {
        this.persistentJwkSource = persistentJwkSource;
        this.auditEventService = auditEventService;
    }

    /**
     * 获取所有密钥列表
     */
    @GetMapping
    public ApiResponse<List<JwkDTO.Response>> listKeys() {
        List<JwkDTO.Response> keys = persistentJwkSource.listAllKeys().stream()
                .map(key -> new JwkDTO.Response(
                        key.getId(),
                        key.getKid(),
                        key.getKeyType(),
                        key.getAlgorithm(),
                        key.getStatus().name(),
                        key.getCreatedAt(),
                        key.getExpiresAt()
                ))
                .toList();
        return ApiResponse.ok(keys);
    }

    /**
     * 轮换密钥：生成新密钥，旧密钥降级为 standby
     */
    @PostMapping("/rotate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<JwkDTO.RotateResponse> rotateKey(@RequestBody(required = false) JwkDTO.RotateRequest request) {
        String algorithm = request != null ? request.algorithm() : null;
        String newKid = persistentJwkSource.rotateKey(algorithm);

        auditEventService.record("JWK_KEY_ROTATED",
                AuditContext.getCurrentPrincipal(), "jwk_key", newKid,
                "轮换 JWK 密钥，新 kid=" + newKid + ", algorithm=" + algorithm, AuditContext.getClientIp());

        return ApiResponse.ok(new JwkDTO.RotateResponse(newKid, "密钥轮换成功"));
    }

    /**
     * 废弃 standby 密钥
     */
    @PostMapping("/{kid}/retire")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<Void> retireKey(@PathVariable(name = "kid") String kid) {
        persistentJwkSource.retireKey(kid);

        auditEventService.record("JWK_KEY_RETIRED",
                AuditContext.getCurrentPrincipal(), "jwk_key", kid,
                "废弃 JWK 密钥: kid=" + kid, AuditContext.getClientIp());

        return ApiResponse.ok();
    }

    /**
     * 删除非 active 密钥
     */
    @DeleteMapping("/{kid}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<Void> deleteKey(@PathVariable(name = "kid") String kid) {
        persistentJwkSource.deleteKey(kid);

        auditEventService.record("JWK_KEY_DELETED",
                AuditContext.getCurrentPrincipal(), "jwk_key", kid,
                "删除 JWK 密钥: kid=" + kid, AuditContext.getClientIp());

        return ApiResponse.ok();
    }
}
