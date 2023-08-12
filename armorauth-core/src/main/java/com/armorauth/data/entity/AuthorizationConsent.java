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

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * 授权确认信息
 * @author fulin
 * @since 2022-08-31
 */
@Data
@IdClass(AuthorizationConsent.AuthorizationConsentId.class)
@Entity
public class AuthorizationConsent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    private String principalName;

    @Id
    private String registeredClientId;

    private String authorities;


    @Data
    public static class AuthorizationConsentId implements Serializable {
        private static final long serialVersionUID = -1813040366041244907L;
        private String registeredClientId;
        private String principalName;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AuthorizationConsentId that = (AuthorizationConsentId) o;
            return registeredClientId.equals(that.registeredClientId) && principalName.equals(that.principalName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(registeredClientId, principalName);
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        AuthorizationConsent that = (AuthorizationConsent) o;
        return registeredClientId != null && Objects.equals(registeredClientId, that.registeredClientId)
                && principalName != null && Objects.equals(principalName, that.principalName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(registeredClientId, principalName);
    }

}
