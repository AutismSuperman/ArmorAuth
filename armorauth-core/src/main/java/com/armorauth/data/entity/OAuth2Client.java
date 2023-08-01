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


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.ToString;
import org.hibernate.Hibernate;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * <p>
 *
 * </p>
 *
 * @author fulin
 * @since 2022-08-31
 */
@Data
@Entity
@Table(name = "oauth2_client")
public class OAuth2Client implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * id
     */
    @Id
    @GenericGenerator(name = "uuid-hex", strategy = "uuid.hex")
    @GeneratedValue(generator = "uuid-hex")
    private String id;
    /**
     * 客户端id
     */
    @Column(name = "client_id", unique = true, updatable = false)
    private String clientId;
    /**
     * 客户端名称
     */
    private String clientName;
    /**
     * 密钥
     */
    private String clientSecret;
    /**
     * 客户端类型
     */
    private String clientAuthenticationMethods;
    /**
     * 客户端授权类型
     */
    private String authorizationGrantTypes;
    /**
     * 回调地址
     */
    private String redirectUris;
    /**
     * 作用域
     */
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "client_id", referencedColumnName = "client_id", insertable = false, updatable = false)
    @ToString.Exclude
    private Set<OAuth2Scope> scopes;
    /**
     * 客户端设置
     */
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "client_id", referencedColumnName = "client_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT), insertable = false, updatable = false)
    @ToString.Exclude
    private OAuth2ClientSettings clientSettings;
    /**
     * token设置
     */
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "client_id", referencedColumnName = "client_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT), insertable = false, updatable = false)
    @ToString.Exclude
    private OAuth2TokenSettings tokenSettings;

    /**
     * 客户端创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Instant clientIdIssuedAt;
    /**
     * 客户端过期时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Instant clientSecretExpiresAt;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        OAuth2Client OAuth2Client = (OAuth2Client) o;
        return id != null && Objects.equals(id, OAuth2Client.id);
    }


}
