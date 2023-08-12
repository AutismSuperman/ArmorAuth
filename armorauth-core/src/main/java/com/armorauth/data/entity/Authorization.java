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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * 授权信息
 *
 * @author fulin
 * @since 2022-08-31
 */
@Data
@Entity
public class Authorization implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    private String id;
    private String principalName;
    private String registeredClientId;
    private String state;
    @Column(columnDefinition = "TEXT")
    private String attributes;


    @Column(columnDefinition = "TEXT")
    private String authorizationCodeValue;
    private String authorizedScopes;
    private String authorizationGrantType;
    private Instant authorizationCodeIssuedAt;
    private Instant authorizationCodeExpiresAt;
    @Column(columnDefinition = "TEXT")
    private String authorizationCodeMetadata;


    @Column(columnDefinition = "TEXT")
    private String accessTokenValue;
    @Column(columnDefinition = "TEXT")
    private String accessTokenScopes;
    private String accessTokenType;
    private Instant accessTokenIssuedAt;
    private Instant accessTokenExpiresAt;
    @Column(columnDefinition = "TEXT")
    private String accessTokenMetadata;


    @Column(columnDefinition = "TEXT")
    private String oidcIdTokenValue;
    @Column(columnDefinition = "TEXT")
    private String oidcIdTokenClaims;
    private Instant oidcIdTokenIssuedAt;
    private Instant oidcIdTokenExpiresAt;
    @Column(columnDefinition = "TEXT")
    private String oidcIdTokenMetadata;


    @Column(columnDefinition = "TEXT")
    private String refreshTokenValue;
    private Instant refreshTokenIssuedAt;
    private Instant refreshTokenExpiresAt;
    @Column(columnDefinition = "TEXT")
    private String refreshTokenMetadata;


    @Column(columnDefinition = "TEXT")
    private String userCodeValue;
    private Instant userCodeIssuedAt;
    private Instant userCodeExpiresAt;
    @Column(columnDefinition = "TEXT")
    private String userCodeMetadata;

    @Column(columnDefinition = "TEXT")
    private String deviceCodeValue;
    private Instant deviceCodeIssuedAt;
    private Instant deviceCodeExpiresAt;
    @Column(columnDefinition = "TEXT")
    private String deviceCodeMetadata;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Authorization that = (Authorization) o;
        return id != null && Objects.equals(id, that.id);
    }


}
