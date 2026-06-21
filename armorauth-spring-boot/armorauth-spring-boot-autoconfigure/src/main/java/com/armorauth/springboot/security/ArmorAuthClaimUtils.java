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
package com.armorauth.springboot.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.util.StringUtils;

final class ArmorAuthClaimUtils {

    private ArmorAuthClaimUtils() {
    }

    static List<String> strings(Map<String, Object> claims, String claimName) {
        if (claims == null || !StringUtils.hasText(claimName)) {
            return List.of();
        }
        return strings(claims.get(claimName));
    }

    static List<String> strings(Object value) {
        Set<String> values = new LinkedHashSet<>();
        collect(value, values);
        return List.copyOf(values);
    }

    private static void collect(Object value, Set<String> values) {
        if (value == null) {
            return;
        }
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                collect(item, values);
            }
            return;
        }
        if (value instanceof String text) {
            for (String item : text.split("[,\\s]+")) {
                if (StringUtils.hasText(item)) {
                    values.add(item.trim());
                }
            }
            return;
        }
        values.add(String.valueOf(value));
    }
}
