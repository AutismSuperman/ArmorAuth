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

import com.armorauth.data.entity.OAuth2Scope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author fulin
 * @since 2022-08-31
 */
@Repository
public interface OAuth2ScopeRepository extends JpaRepository<OAuth2Scope, OAuth2Scope.OAuth2ScopeId> {

    List<OAuth2Scope> findAllByClientIdAndScopeIn(String clientId, Collection<String> scopes);

    Page<OAuth2Scope> findByClientId(String clientId, Pageable pageable);

    Optional<OAuth2Scope> findByClientIdAndScope(String clientId, String scope);

    boolean existsByClientIdAndScope(String clientId, String scope);

    void deleteByClientIdAndScope(String clientId, String scope);

}
