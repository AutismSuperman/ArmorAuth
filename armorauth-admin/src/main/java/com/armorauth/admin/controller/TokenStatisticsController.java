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
package com.armorauth.admin.controller;

import com.armorauth.admin.service.TokenStatisticsService;
import com.armorauth.common.response.ApiResponse;
import com.armorauth.data.entity.TokenStatistics;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Token 统计API
 *
 * @author fulin
 * @since 2026-05-23
 */
@RestController
@RequestMapping("/api/admin/v1/token-statistics")
public class TokenStatisticsController {

    private final TokenStatisticsService tokenStatisticsService;

    public TokenStatisticsController(TokenStatisticsService tokenStatisticsService) {
        this.tokenStatisticsService = tokenStatisticsService;
    }

    @GetMapping
    public ApiResponse<List<TokenStatistics>> getStatistics(
            @RequestParam(name = "clientId") String clientId,
            @RequestParam(name = "from") LocalDate from,
            @RequestParam(name = "to") LocalDate to) {
        return ApiResponse.ok(tokenStatisticsService.getStatistics(clientId, from, to));
    }

    @GetMapping("/summary")
    public ApiResponse<List<TokenStatistics>> getSummary(
            @RequestParam(name = "from") LocalDate from,
            @RequestParam(name = "to") LocalDate to) {
        return ApiResponse.ok(tokenStatisticsService.getSummary(from, to));
    }
}
