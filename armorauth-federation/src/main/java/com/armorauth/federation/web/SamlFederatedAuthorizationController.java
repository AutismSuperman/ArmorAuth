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
package com.armorauth.federation.web;

import com.armorauth.federation.FederatedAuthorizationContext;
import com.armorauth.federation.FederatedLoginMode;
import com.armorauth.federation.FederatedSessionContextRepository;
import com.armorauth.federation.config.FederationProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class SamlFederatedAuthorizationController {

    private static final Logger log = LoggerFactory.getLogger(SamlFederatedAuthorizationController.class);

    private final RelyingPartyRegistrationRepository relyingPartyRegistrationRepository;

    private final FederatedSessionContextRepository sessionContextRepository;

    private final FederatedLoginMode defaultLoginMode;

    public SamlFederatedAuthorizationController(
            RelyingPartyRegistrationRepository relyingPartyRegistrationRepository,
            FederatedSessionContextRepository sessionContextRepository,
            FederationProperties federationProperties) {
        this.relyingPartyRegistrationRepository = relyingPartyRegistrationRepository;
        this.sessionContextRepository = sessionContextRepository;
        this.defaultLoginMode =
                FederatedLoginMode.resolveConfiguredDefault(federationProperties.getDefaultLoginMode());
    }

    @GetMapping("/saml2/authorization/{registrationId}")
    public String authorize(@PathVariable String registrationId,
                            @RequestParam(name = "mode", required = false) String mode,
                            HttpServletRequest request) {
        try {
            if (!StringUtils.hasText(registrationId)
                    || this.relyingPartyRegistrationRepository.findByRegistrationId(registrationId) == null) {
                throw new IllegalArgumentException("SAML 身份源不存在或未启用。");
            }
            FederatedLoginMode loginMode =
                    FederatedLoginMode.resolveForAuthorization(mode, this.defaultLoginMode);
            this.sessionContextRepository.saveAuthorizationContext(
                    request,
                    new FederatedAuthorizationContext(
                            registrationId,
                            loginMode,
                            request.getRequestURI()
                                    + (request.getQueryString() != null ? "?" + request.getQueryString() : ""),
                            System.currentTimeMillis()
                    )
            );
            this.sessionContextRepository.clearPendingContext(request);
            log.info("Resolved SAML authorization request registrationId={} mode={} uri={}",
                    registrationId, loginMode, request.getRequestURI());
            return "redirect:/saml2/authenticate/" + registrationId;
        } catch (IllegalArgumentException ex) {
            this.sessionContextRepository.clearAll(request);
            request.getSession(true)
                    .setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, new BadCredentialsException(ex.getMessage(), ex));
            return "redirect:/login?error";
        }
    }
}
