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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.Instant;

/**
 * 登录页身份源展示偏好。
 */
@Data
@Entity
@Table(name = "identity_provider_display_preference")
public class IdentityProviderDisplayPreference {

    @Id
    @Column(name = "registration_id", nullable = false, length = 100)
    private String registrationId;

    @Column(name = "display_on_login", nullable = false)
    private Boolean displayOnLogin = true;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
