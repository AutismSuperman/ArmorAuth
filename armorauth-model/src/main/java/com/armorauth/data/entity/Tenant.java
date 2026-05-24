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
package com.armorauth.data.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * 租户/用户池
 *
 * @author fulin
 * @since 2026-05-23
 */
@Data
@Entity
@Table(name = "tenant")
public class Tenant implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "tenant_code", unique = true, nullable = false, length = 100)
    private String tenantCode;

    @Column(name = "tenant_name", nullable = false, length = 200)
    private String tenantName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "logo", length = 512)
    private String logo;

    @Column(name = "primary_color", length = 20)
    private String primaryColor;

    @Column(name = "custom_domain", length = 255)
    private String customDomain;

    @Column(name = "login_page_title", length = 200)
    private String loginPageTitle;

    @Column(name = "privacy_policy_url", length = 512)
    private String privacyPolicyUrl;

    @Column(name = "terms_of_service_url", length = 512)
    private String termsOfServiceUrl;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
