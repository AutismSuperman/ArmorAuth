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
 * 组织成员关系
 *
 * @author fulin
 * @since 2026-05-23
 */
@Data
@Entity
@Table(name = "organization_member", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"org_id", "user_id"})
})
public class OrganizationMember implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "org_id", nullable = false)
    private String orgId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "org_role", length = 100)
    private String orgRole;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
