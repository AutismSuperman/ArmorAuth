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
import java.time.LocalDate;

/**
 * Token 签发统计
 *
 * @author fulin
 * @since 2026-05-23
 */
@Data
@Entity
@Table(name = "token_statistics", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"client_id", "grant_type", "token_type", "date"})
})
public class TokenStatistics implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "client_id", nullable = false)
    private String clientId;

    @Column(name = "grant_type", nullable = false, length = 100)
    private String grantType;

    @Column(name = "token_type", nullable = false, length = 50)
    private String tokenType;

    @Column(name = "count", nullable = false)
    private Long count = 0L;

    @Column(name = "last_issued_at")
    private Instant lastIssuedAt;

    @Column(name = "date", nullable = false)
    private LocalDate date;
}
