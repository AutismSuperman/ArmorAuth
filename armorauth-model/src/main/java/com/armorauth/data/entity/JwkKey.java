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
import org.hibernate.Hibernate;

import jakarta.persistence.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * JWK 签名密钥
 *
 * @author fulin
 * @since 2026-05-22
 */
@Data
@Entity
@Table(name = "jwk_key")
public class JwkKey implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @Column(unique = true, nullable = false)
    private String kid;

    @Column(name = "key_type", nullable = false, length = 20)
    private String keyType;

    @Column(nullable = false, length = 50)
    private String algorithm;

    @Column(name = "public_key", nullable = false, columnDefinition = "TEXT")
    private String publicKey;

    @Column(name = "private_key", nullable = false, columnDefinition = "TEXT")
    private String privateKey;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private JwkKeyStatus status = JwkKeyStatus.active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public enum JwkKeyStatus {
        active,
        standby,
        retired
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        JwkKey jwkKey = (JwkKey) o;
        return id != null && Objects.equals(id, jwkKey.id);
    }

}
