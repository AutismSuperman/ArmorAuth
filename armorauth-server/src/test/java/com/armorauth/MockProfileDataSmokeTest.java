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
package com.armorauth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.datasource.url=jdbc:h2:mem:armorauth_mock_profile;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;NON_KEYWORDS=USER,AUTHORIZATION",
        "spring.jpa.show-sql=false"
})
@ActiveProfiles("mock")
class MockProfileDataSmokeTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void mockProfileSeedsUsersClientsIdentitySourcesAndOperationsData() {
        assertThat(count("user_info", "username in ('admin','app.manager','audit.viewer','demo.user')"))
                .isEqualTo(4);
        assertThat(count("oauth2_client", "client_id in ('spring-pkce','mock-dashboard')"))
                .isEqualTo(2);
        assertThat(count("tenant", "tenant_code in ('default','acme')")).isEqualTo(2);
        assertThat(count("organization", "tenant_id = 'tenant-acme'")).isEqualTo(3);
        assertThat(count("identity_provider", "registration_id like 'mock-%'")).isEqualTo(4);
        assertThat(count("webhook_endpoint", "id = 'mock-webhook-audit'")).isEqualTo(1);
        assertThat(count("audit_event", "id like 'mock-audit-%'")).isEqualTo(3);
        assertThat(count("token_statistics", "id like 'mock-token-%'")).isEqualTo(2);
        assertThat(count("oauth2_client_settings", "client_id = 'spring-pkce' and require_proof_key = true"))
                .isEqualTo(1);
    }

    private Integer count(String tableName, String whereClause) {
        return jdbcTemplate.queryForObject("select count(*) from `" + tableName + "` where " + whereClause,
                Integer.class);
    }
}
