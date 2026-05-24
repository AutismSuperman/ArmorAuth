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
package com.armorauth.admin.dto;

import java.util.List;

public class SecretProtectionDTO {

    public record RekeyRequest(
            Boolean dryRun
    ) {}

    public record RekeyResponse(
            String activeKeyId,
            List<String> configuredKeyIds,
            Boolean dryRun,
            RekeyStats identityProviders,
            RekeyStats webhookEndpoints,
            RekeyStats authFactors,
            RekeyStats jwkKeys,
            RekeyStats total
    ) {}

    public record RekeyStats(
            Integer scanned,
            Integer blank,
            Integer alreadyActive,
            Integer plaintext,
            Integer differentKey,
            Integer wouldRekey,
            Integer rekeyed,
            Integer failed
    ) {}
}
