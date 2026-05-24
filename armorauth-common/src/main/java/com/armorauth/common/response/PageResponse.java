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
package com.armorauth.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 分页响应
 *
 * @author fulin
 * @since 2026-05-23
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PageResponse<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int page,
        int size
) {
}
