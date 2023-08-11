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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.time.Duration;
import java.util.Objects;

/**
 * 客户端Token设置信息
 * @author fulin
 * @since 2022-08-31
 */
@Data
@Entity
@Table(name = "oauth2_token_settings")
public class OAuth2TokenSettings implements Serializable {


    @Id
    @Column(name = "client_id", insertable = false, updatable = false)
    private String clientId;

    private Duration accessTokenTimeToLive;

    private String idTokenSignatureAlgorithm;

    private Duration refreshTokenTimeToLive;

    private Boolean reuseRefreshTokens;

    private String tokenFormat;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        OAuth2TokenSettings OAuth2TokenSettings = (OAuth2TokenSettings) o;
        return clientId != null && Objects.equals(clientId, OAuth2TokenSettings.clientId);
    }

}
