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
package com.armorauth.autoconfigure;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.armorauth.springboot.client.ArmorAuthAdminRestClient;
import com.armorauth.springboot.client.ArmorAuthAdminRestClientCustomizer;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ArmorAuthAdminClientAutoConfigurationTest {

    private static final AtomicReference<MockRestServiceServer> server = new AtomicReference<>();

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ArmorAuthAdminClientAutoConfiguration.class));

    @Test
    void backsOffByDefault() {
        this.contextRunner
                .run(context -> assertThat(context).doesNotHaveBean(ArmorAuthAdminRestClient.class));
    }

    @Test
    void failsWhenBaseUrlIsMissing() {
        this.contextRunner
                .withPropertyValues("armorauth.admin-client.enabled=true")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void createsBasicAuthenticatedAdminClient() {
        this.contextRunner
                .withPropertyValues(
                        "armorauth.admin-client.enabled=true",
                        "armorauth.admin-client.base-url=http://localhost:9000/",
                        "armorauth.admin-client.username=admin",
                        "armorauth.admin-client.password=secret")
                .withUserConfiguration(MockServerCustomizerConfiguration.class)
                .run(context -> {
                    MockRestServiceServer mockServer = server.get();
                    mockServer.expect(once(), requestTo("http://localhost:9000/api/admin/v1/applications?page=0&size=10"))
                            .andExpect(header(HttpHeaders.AUTHORIZATION, "Basic YWRtaW46c2VjcmV0"))
                            .andRespond(withSuccess("{\"totalElements\":0}", MediaType.APPLICATION_JSON));

                    Map<String, Object> response = context.getBean(ArmorAuthAdminRestClient.class)
                            .listApplications(0, 10);

                    assertThat(response).containsEntry("totalElements", 0);
                    mockServer.verify();
                });
    }

    @Test
    void bearerTokenTakesPrecedenceOverBasicAuthentication() {
        this.contextRunner
                .withPropertyValues(
                        "armorauth.admin-client.enabled=true",
                        "armorauth.admin-client.base-url=http://localhost:9000",
                        "armorauth.admin-client.username=admin",
                        "armorauth.admin-client.password=secret",
                        "armorauth.admin-client.bearer-token=token-value")
                .withUserConfiguration(MockServerCustomizerConfiguration.class)
                .run(context -> {
                    MockRestServiceServer mockServer = server.get();
                    mockServer.expect(once(), requestTo("http://localhost:9000/api/admin/v1/users?page=0&size=20"))
                            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token-value"))
                            .andRespond(withSuccess("{\"totalElements\":1}", MediaType.APPLICATION_JSON));

                    Map<String, Object> response = context.getBean(ArmorAuthAdminRestClient.class)
                            .listUsers(0, 20);

                    assertThat(response).containsEntry("totalElements", 1);
                    mockServer.verify();
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class MockServerCustomizerConfiguration {

        @Bean
        ArmorAuthAdminRestClientCustomizer mockServerCustomizer() {
            return builder -> server.set(MockRestServiceServer.bindTo(builder).build());
        }
    }
}
