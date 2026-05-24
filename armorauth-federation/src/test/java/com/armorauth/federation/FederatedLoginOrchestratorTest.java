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
package com.armorauth.federation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FederatedLoginOrchestratorTest {

    @Test
    void shouldStorePendingContextForConfirmMode() throws Exception {
        FederatedAccountService federatedAccountService = mock(FederatedAccountService.class);
        UserFederatedBindingService userFederatedBindingService = mock(UserFederatedBindingService.class);
        FederatedSessionContextRepository federatedSessionContextRepository = mock(FederatedSessionContextRepository.class);
        FederatedLoginCompletionService federatedLoginCompletionService = mock(FederatedLoginCompletionService.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        FederatedLoginOrchestrator orchestrator = new FederatedLoginOrchestrator(
                new ObjectMapper(),
                federatedAccountService,
                userFederatedBindingService,
                federatedSessionContextRepository,
                federatedLoginCompletionService,
                eventPublisher,
                null
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2AuthenticationToken authenticationToken = new OAuth2AuthenticationToken(
                new DefaultOAuth2User(
                        AuthorityUtils.createAuthorityList("ROLE_USER"),
                        Map.of("id", "abc12345", "nickname", "Armor User"),
                        "id"
                ),
                AuthorityUtils.createAuthorityList("ROLE_USER"),
                "gitee"
        );
        when(federatedAccountService.currentTimestamp()).thenReturn(java.time.Instant.parse("2026-04-17T10:00:00Z"));
        when(userFederatedBindingService.findBinding("gitee", "abc12345")).thenReturn(Optional.empty());
        when(federatedSessionContextRepository.loadAuthorizationContext(request)).thenReturn(
                Optional.of(new FederatedAuthorizationContext(
                        "gitee",
                        FederatedLoginMode.CONFIRM,
                        "/oauth2/authorization/gitee?mode=confirm",
                        System.currentTimeMillis()
                ))
        );
        when(federatedAccountService.generateAvailableUsername("gitee", "abc12345", "Armor User"))
                .thenReturn("armor_user");

        boolean handled = orchestrator.handleSuccess(request, response, authenticationToken);

        assertThat(handled).isTrue();
        assertThat(response.getRedirectedUrl()).isEqualTo("/federated/confirm");
        ArgumentCaptor<PendingFederatedContext> pendingCaptor = ArgumentCaptor.forClass(PendingFederatedContext.class);
        verify(federatedSessionContextRepository).savePendingContext(eq(request), pendingCaptor.capture());
        assertThat(pendingCaptor.getValue().registrationId()).isEqualTo("gitee");
        assertThat(pendingCaptor.getValue().providerUserId()).isEqualTo("abc12345");
        assertThat(pendingCaptor.getValue().suggestedUsername()).isEqualTo("armor_user");
        verify(federatedLoginCompletionService).clearAuthentication(request, response);
        verify(federatedLoginCompletionService, never()).complete(any(), any(), any());
    }
}
