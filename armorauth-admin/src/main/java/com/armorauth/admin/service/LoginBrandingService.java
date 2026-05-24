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
package com.armorauth.admin.service;

import com.armorauth.data.entity.Tenant;
import com.armorauth.data.repository.TenantRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 登录页品牌配置服务
 *
 * @author fulin
 * @since 2026-05-23
 */
@Service
public class LoginBrandingService {

    private final TenantRepository tenantRepository;

    public LoginBrandingService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    /**
     * 获取默认租户的品牌配置
     */
    public Optional<Tenant> getDefaultBranding() {
        return tenantRepository.findAll().stream().findFirst();
    }

    /**
     * 获取指定租户的品牌配置
     */
    public Optional<Tenant> getBranding(String tenantCode) {
        return tenantRepository.findByTenantCode(tenantCode);
    }
}
