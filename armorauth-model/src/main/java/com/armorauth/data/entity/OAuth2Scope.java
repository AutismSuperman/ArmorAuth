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

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * 作用域信息
 * @author fulin
 * @since 2022-08-31
 */
@Data
@Entity
@Table(name = "oauth2_scope")
@IdClass(OAuth2Scope.OAuth2ScopeId.class)
public class OAuth2Scope implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "client_id", insertable = false, updatable = false)
    private String clientId;

    @Id
    private String scope;

    private String description;


    @Data
    public static class OAuth2ScopeId implements Serializable {
        private static final long serialVersionUID = 1991088202139468930L;
        private String clientId;
        private String scope;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        OAuth2Scope that = (OAuth2Scope) o;
        return clientId != null && Objects.equals(clientId, that.clientId)
                && scope != null && Objects.equals(scope, that.scope);
    }


    @Override
    public int hashCode() {
        return Objects.hash(clientId, scope);
    }


}
