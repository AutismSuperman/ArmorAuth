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

import lombok.Data;

import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;

/**
 * 权限表
 *
 * @author fulin
 * @since 2026-05-23
 */
@Data
@Entity
@Table(name = "sys_permission")
public class Permission implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "permission_code", nullable = false, unique = true, length = 200)
    private String permissionCode;

    @Column(name = "permission_name", nullable = false, length = 200)
    private String permissionName;

    @Column(name = "resource_type", length = 100)
    private String resourceType;

    @Column(name = "action", length = 100)
    private String action;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "builtin")
    private Boolean builtin;
}
