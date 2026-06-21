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
package com.armorauth.authentication;

import com.armorauth.data.entity.AuthFactor;
import com.armorauth.data.entity.UserInfo;
import com.armorauth.data.repository.AuthFactorRepository;
import com.armorauth.data.repository.UserInfoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MfaAuthenticationSuccessHandlerTest {

    private final AuthFactorRepository authFactorRepository = mock(AuthFactorRepository.class);

    private final UserInfoRepository userInfoRepository = mock(UserInfoRepository.class);

    private final AuthenticationSuccessHandler delegate = mock(AuthenticationSuccessHandler.class);

    private final Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
            "admin", "ignored", AuthorityUtils.createAuthorityList("ROLE_USER"));

    @Test
    void delegatesWhenFactorExistsButUserMfaPreferenceIsDisabled() throws Exception {
        UserInfo user = user(false);
        when(userInfoRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(authFactorRepository.findByUserIdAndEnabledTrue("user-1")).thenReturn(List.of(readyTotpFactor()));
        MfaAuthenticationSuccessHandler handler =
                new MfaAuthenticationSuccessHandler(authFactorRepository, delegate, null, userInfoRepository);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(request, response, authentication);

        verify(delegate).onAuthenticationSuccess(request, response, authentication);
        assertThat(response.getRedirectedUrl()).isNull();
        assertThat(request.getSession(false)).isNull();
    }

    @Test
    void redirectsToMfaWhenUserMfaPreferenceIsEnabledAndFactorIsReady() throws Exception {
        UserInfo user = user(true);
        when(userInfoRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(authFactorRepository.findByUserIdAndEnabledTrue("user-1")).thenReturn(List.of(readyTotpFactor()));
        MfaAuthenticationSuccessHandler handler =
                new MfaAuthenticationSuccessHandler(authFactorRepository, delegate, null, userInfoRepository);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(request, response, authentication);

        verify(delegate, never()).onAuthenticationSuccess(request, response, authentication);
        assertThat(response.getRedirectedUrl()).isEqualTo("/login/mfa");
        assertThat(request.getSession(false)
                .getAttribute(MfaAuthenticationSuccessHandler.PENDING_MFA_PRINCIPAL)).isEqualTo("admin");
    }

    @Test
    void redirectsToMfaBindingWhenPolicyRequiresMfaAndUserHasNoReadyFactor() throws Exception {
        UserInfo user = user(false);
        MfaPolicyService mfaPolicyService = mock(MfaPolicyService.class);
        when(userInfoRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(authFactorRepository.findByUserIdAndEnabledTrue("user-1")).thenReturn(List.of());
        when(mfaPolicyService.requiresMfa("user-1", "client-a")).thenReturn(true);
        MfaAuthenticationSuccessHandler handler = new MfaAuthenticationSuccessHandler(
                authFactorRepository, delegate, mfaPolicyService, userInfoRepository);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("client_id", "client-a");
        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(request, response, authentication);

        verify(delegate, never()).onAuthenticationSuccess(request, response, authentication);
        assertThat(response.getRedirectedUrl()).isEqualTo("/login/mfa");
        assertThat(request.getSession(false)
                .getAttribute(MfaAuthenticationSuccessHandler.MFA_BINDING_REQUIRED)).isEqualTo(true);
    }

    @Test
    void fallsBackToUsernameForPolicyWhenUserInfoIsMissing() throws Exception {
        MfaPolicyService mfaPolicyService = mock(MfaPolicyService.class);
        when(userInfoRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(authFactorRepository.findByUserIdAndEnabledTrue("admin")).thenReturn(List.of());
        when(mfaPolicyService.requiresMfa("admin", "client-a")).thenReturn(true);
        MfaAuthenticationSuccessHandler handler = new MfaAuthenticationSuccessHandler(
                authFactorRepository, delegate, mfaPolicyService, userInfoRepository);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("client_id", "client-a");
        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(request, response, authentication);

        verify(delegate, never()).onAuthenticationSuccess(request, response, authentication);
        assertThat(response.getRedirectedUrl()).isEqualTo("/login/mfa");
        assertThat(request.getSession(false)
                .getAttribute(MfaAuthenticationSuccessHandler.MFA_BINDING_REQUIRED)).isEqualTo(true);
    }

    private UserInfo user(boolean mfaEnabled) {
        UserInfo user = new UserInfo();
        user.setId("user-1");
        user.setUsername("admin");
        user.setMfaEnabled(mfaEnabled);
        return user;
    }

    private AuthFactor readyTotpFactor() {
        AuthFactor factor = new AuthFactor();
        factor.setUserId("user-1");
        factor.setFactorType("TOTP");
        factor.setVerified(true);
        factor.setEnabled(true);
        return factor;
    }
}
