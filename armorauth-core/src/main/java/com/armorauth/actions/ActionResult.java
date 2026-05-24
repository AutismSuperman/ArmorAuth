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
package com.armorauth.actions;

import java.util.Map;

public record ActionResult(
        boolean allowed,
        boolean terminal,
        String reason,
        Map<String, Object> attributes
) {

    public static ActionResult neutral() {
        return new ActionResult(true, false, null, Map.of());
    }

    public static ActionResult allow(String reason) {
        return new ActionResult(true, true, reason, Map.of());
    }

    public static ActionResult deny(String reason) {
        return new ActionResult(false, true, reason, Map.of());
    }

    public static ActionResult enrich(Map<String, Object> attributes) {
        return new ActionResult(true, false, null, attributes == null ? Map.of() : attributes);
    }
}
