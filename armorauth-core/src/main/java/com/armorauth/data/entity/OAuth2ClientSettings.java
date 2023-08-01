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
import java.util.Objects;

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
@Table(name = "oauth2_client_settings")
public class OAuth2ClientSettings implements Serializable {

    @Id
    @Column(name = "client_id", insertable = false, updatable = false)
    private String clientId;

    private String jwkSetUrl;

    private Boolean requireAuthorizationConsent;

    private Boolean requireProofKey;

    private String signingAlgorithm;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        OAuth2ClientSettings OAuth2ClientSettings = (OAuth2ClientSettings) o;
        return clientId != null && Objects.equals(clientId, OAuth2ClientSettings.clientId);
    }

}
