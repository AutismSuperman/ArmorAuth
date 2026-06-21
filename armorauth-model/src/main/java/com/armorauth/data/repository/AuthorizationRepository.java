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
package com.armorauth.data.repository;

import com.armorauth.data.entity.Authorization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * @author fulin
 * @since 2022-08-31
 */
@Repository
public interface AuthorizationRepository  extends JpaRepository<Authorization, String> {

    Optional<Authorization> findByTenantIdAndId(String tenantId, String id);
    Optional<Authorization> findByState(String state);
    Optional<Authorization> findByTenantIdAndState(String tenantId, String state);
    Optional<Authorization> findByAuthorizationCodeValue(String authorizationCode);
    Optional<Authorization> findByTenantIdAndAuthorizationCodeValue(String tenantId, String authorizationCode);
    Optional<Authorization> findByAccessTokenValue(String accessToken);
    Optional<Authorization> findByTenantIdAndAccessTokenValue(String tenantId, String accessToken);
    Optional<Authorization> findByRefreshTokenValue(String refreshToken);
    Optional<Authorization> findByTenantIdAndRefreshTokenValue(String tenantId, String refreshToken);
    Optional<Authorization> findByOidcIdTokenValue(String idToken);
    Optional<Authorization> findByTenantIdAndOidcIdTokenValue(String tenantId, String idToken);
    Optional<Authorization> findByUserCodeValue(String userCode);
    Optional<Authorization> findByTenantIdAndUserCodeValue(String tenantId, String userCode);
    Optional<Authorization> findByDeviceCodeValue(String deviceCode);
    Optional<Authorization> findByTenantIdAndDeviceCodeValue(String tenantId, String deviceCode);
    @Query("select a from Authorization a where a.state = :token" +
            " or a.authorizationCodeValue = :token" +
            " or a.accessTokenValue = :token" +
            " or a.refreshTokenValue = :token" +
            " or a.oidcIdTokenValue = :token" +
            " or a.userCodeValue = :token" +
            " or a.deviceCodeValue = :token"
    )
    Optional<Authorization> findByStateOrAuthorizationCodeValueOrAccessTokenValueOrRefreshTokenValueOrOidcIdTokenValueOrUserCodeValueOrDeviceCodeValue(@Param("token") String token);

    @Query("select a from Authorization a where a.tenantId = :tenantId and (a.state = :token" +
            " or a.authorizationCodeValue = :token" +
            " or a.accessTokenValue = :token" +
            " or a.refreshTokenValue = :token" +
            " or a.oidcIdTokenValue = :token" +
            " or a.userCodeValue = :token" +
            " or a.deviceCodeValue = :token)"
    )
    Optional<Authorization> findByTenantIdAndAnyToken(@Param("tenantId") String tenantId, @Param("token") String token);

    @Query("select a from Authorization a where a.accessTokenValue is not null" +
            " and a.accessTokenIssuedAt >= :from" +
            " and a.accessTokenIssuedAt < :to")
    List<Authorization> findIssuedAccessTokens(@Param("from") Instant from, @Param("to") Instant to);

    long countByPrincipalName(String principalName);

    List<Authorization> findTop20ByPrincipalNameOrderByAccessTokenIssuedAtDesc(String principalName);

}
