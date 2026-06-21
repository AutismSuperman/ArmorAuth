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

import com.armorauth.authentication.CaptchaVerifyService;
import com.armorauth.authentication.SmsOtpService;
import com.armorauth.captcha.GraphicCaptchaService;
import com.armorauth.data.entity.IdentityProvider;
import com.armorauth.data.repository.IdentityProviderDisplayPreferenceRepository;
import com.armorauth.data.repository.IdentityProviderRepository;
import com.armorauth.federation.config.FederationProperties;
import com.armorauth.data.entity.OAuth2Scope;
import com.armorauth.data.repository.OAuth2ScopeRepository;
import com.armorauth.federation.FederatedLoginMode;
import com.armorauth.security.LoginRateLimiter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Controller
public class OAuth2FrontendController {

    private final RegisteredClientRepository registeredClientRepository;

    private final ClientRegistrationRepository clientRegistrationRepository;

    private final IdentityProviderRepository identityProviderRepository;

    private final IdentityProviderDisplayPreferenceRepository displayPreferenceRepository;

    private final RelyingPartyRegistrationRepository relyingPartyRegistrationRepository;

    private final OAuth2AuthorizationConsentService authorizationConsentService;

    private final OAuth2ScopeRepository oAuth2ScopeRepository;

    private final AuthorizationServerSettings authorizationServerSettings;

    private final CaptchaVerifyService captchaVerifyService;

    private final GraphicCaptchaService graphicCaptchaService;

    private final SmsOtpService smsOtpService;

    private final boolean exposeSmsOtp;

    private final UserDetailsService userDetailsService;

    private final SecurityContextRepository securityContextRepository;

    private final AuthenticationSuccessHandler authenticationSuccessHandler;

    private final FederatedLoginMode defaultFederatedLoginMode;

    private final LoginRateLimiter loginRateLimiter;

    public OAuth2FrontendController(RegisteredClientRepository registeredClientRepository,
                                    ClientRegistrationRepository clientRegistrationRepository,
                                    ObjectProvider<IdentityProviderRepository> identityProviderRepositoryProvider,
                                    ObjectProvider<IdentityProviderDisplayPreferenceRepository> displayPreferenceRepositoryProvider,
                                    RelyingPartyRegistrationRepository relyingPartyRegistrationRepository,
                                    OAuth2AuthorizationConsentService authorizationConsentService,
                                    OAuth2ScopeRepository oAuth2ScopeRepository,
                                    AuthorizationServerSettings authorizationServerSettings,
                                    ObjectProvider<CaptchaVerifyService> captchaVerifyServiceProvider,
                                    ObjectProvider<GraphicCaptchaService> graphicCaptchaServiceProvider,
                                    ObjectProvider<SmsOtpService> smsOtpServiceProvider,
                                    ObjectProvider<LoginRateLimiter> loginRateLimiterProvider,
                                    UserDetailsService userDetailsService,
                                    SecurityContextRepository securityContextRepository,
                                    @Qualifier("formAuthenticationSuccessHandler")
                                    AuthenticationSuccessHandler authenticationSuccessHandler,
                                    FederationProperties federationProperties,
                                    @Value("${armorauth.captcha.sms.expose-code:false}") boolean exposeSmsOtp) {
        this.registeredClientRepository = registeredClientRepository;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.identityProviderRepository = identityProviderRepositoryProvider.getIfAvailable();
        this.displayPreferenceRepository = displayPreferenceRepositoryProvider.getIfAvailable();
        this.relyingPartyRegistrationRepository = relyingPartyRegistrationRepository;
        this.authorizationConsentService = authorizationConsentService;
        this.oAuth2ScopeRepository = oAuth2ScopeRepository;
        this.authorizationServerSettings = authorizationServerSettings;
        this.graphicCaptchaService = graphicCaptchaServiceProvider.getIfAvailable(null);
        this.smsOtpService = smsOtpServiceProvider.getIfAvailable(null);
        this.exposeSmsOtp = exposeSmsOtp;
        this.loginRateLimiter = loginRateLimiterProvider.getIfAvailable(null);

        this.captchaVerifyService = captchaVerifyServiceProvider.getIfAvailable(
                () -> (account, code) -> "1234".equals(code));

        this.userDetailsService = userDetailsService;
        this.securityContextRepository = securityContextRepository;
        this.authenticationSuccessHandler = authenticationSuccessHandler;
        this.defaultFederatedLoginMode =
                FederatedLoginMode.resolveConfiguredDefault(federationProperties.getDefaultLoginMode());
    }

    @GetMapping(path = "/", produces = MediaType.TEXT_HTML_VALUE)
    @RegisterReflectionForBinding(String.class)
    public String index(@CurrentSecurityContext(expression = "authentication") Authentication authentication,
                        Model model) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }
        model.addAttribute("userName", authentication.getName());
        return "home";
    }

    @GetMapping(path = "/login", produces = MediaType.TEXT_HTML_VALUE)
    @RegisterReflectionForBinding(String.class)
    public String login(@CurrentSecurityContext(expression = "authentication") Authentication authentication,
                        HttpServletRequest request,
                        Model model,
                        @RequestParam(name = "mode", required = false) String mode,
                        @RequestParam(name = "tab", required = false) String tab,
                        @RequestParam(name = "error", required = false) String error,
                        @RequestParam(name = "logout", required = false) String logout) {
        if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/";
        }

        model.addAttribute("federatedProviders", getFederatedProviders());
        model.addAttribute("loggedOut", logout != null);
        model.addAttribute("selectedFederatedMode",
                FederatedLoginMode.resolveForPage(mode, defaultFederatedLoginMode).getParameterValue());
        model.addAttribute("graphicCaptcha", graphicCaptchaService != null);
        model.addAttribute("smsCaptcha", smsOtpService != null);
        model.addAttribute("selectedLoginTab", resolveSelectedLoginTab(tab));

        if (error != null) {
            model.addAttribute("errorMessage", resolveLoginErrorMessage(request));
        }

        return "login";
    }

    @PostMapping(path = "/login/captcha/send", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> sendCaptcha(@RequestParam("account") String account) {
        if (!StringUtils.hasText(account)) {
            return ResponseEntity.badRequest().body(Map.of("message", "请输入手机号后再获取验证码。"));
        }
        String normalizedAccount = account.trim();
        if (smsOtpService != null) {
            String otp = smsOtpService.generateOtp(normalizedAccount);
            if (exposeSmsOtp) {
                return ResponseEntity.ok(Map.of(
                        "message", "验证码已发送，Mock 验证码：" + otp + "。",
                        "captcha", otp,
                        "debugCode", otp
                ));
            }
            return ResponseEntity.ok(Map.of("message", "验证码已发送，请查看短信。"));
        }
        return ResponseEntity.ok(Map.of(
                "message", "验证码已发送，当前演示环境固定验证码为 1234。",
                "captcha", "1234",
                "debugCode", "1234"
        ));
    }

    @GetMapping(path = "/login/captcha")
    public String captchaLoginPage() {
        return "redirect:/login";
    }

    @PostMapping(path = "/login/captcha", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public void captchaLogin(@RequestParam(name = "account", required = false) String account,
                             @RequestParam(name = "captcha", required = false) String captcha,
                             @RequestParam(name = "captchaId", required = false) String captchaId,
                             HttpServletRequest request,
                             HttpServletResponse response) throws IOException, ServletException {
        String clientIp = getClientIp(request);

        // 检查限流
        if (loginRateLimiter != null && account != null
                && loginRateLimiter.isBlocked(account, clientIp)) {
            saveAuthenticationException(request, new BadCredentialsException("登录尝试次数过多，请稍后再试。"));
            response.sendRedirect(request.getContextPath() + "/login?error&tab=" + captchaFailureTab(captchaId));
            return;
        }

        boolean verified;
        if (StringUtils.hasText(captchaId)) {
            verified = graphicCaptchaService != null && graphicCaptchaService.verify(captchaId, captcha);
        } else if (smsOtpService != null) {
            verified = smsOtpService.verifyOtp(
                    account != null ? account.trim() : "",
                    captcha != null ? captcha.trim() : "");
        } else {
            // 兼容旧模式
            verified = this.captchaVerifyService.verifyCaptcha(
                    account != null ? account.trim() : "",
                    captcha != null ? captcha.trim() : "");
        }

        if (!verified || !StringUtils.hasText(account)) {
            // 记录失败
            if (loginRateLimiter != null) {
                loginRateLimiter.recordFailure(account, clientIp);
            }
            saveAuthenticationException(request, new BadCredentialsException("验证码不正确。"));
            response.sendRedirect(request.getContextPath() + "/login?error&tab=" + captchaFailureTab(captchaId));
            return;
        }

        UserDetails userDetails;
        try {
            userDetails = this.userDetailsService.loadUserByUsername(account.trim());
        } catch (AuthenticationException ex) {
            if (loginRateLimiter != null) {
                loginRateLimiter.recordFailure(account, clientIp);
            }
            saveAuthenticationException(request, ex);
            response.sendRedirect(request.getContextPath() + "/login?error&tab=" + captchaFailureTab(captchaId));
            return;
        }
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                userDetails, null, userDetails.getAuthorities());
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        this.securityContextRepository.saveContext(securityContext, request, response);

        // 登录成功，清除失败记录
        if (loginRateLimiter != null) {
            loginRateLimiter.clearFailures(account);
            loginRateLimiter.clearFailures(clientIp);
        }

        this.authenticationSuccessHandler.onAuthenticationSuccess(request, response, authentication);
    }

    @GetMapping(path = "/login/mfa", produces = MediaType.TEXT_HTML_VALUE)
    public String mfaChallenge(HttpServletRequest request, Model model,
                               @RequestParam(name = "error", required = false) String error) {
        String principal = (String) request.getSession().getAttribute("PENDING_MFA_PRINCIPAL");
        if (principal == null) {
            return "redirect:/login";
        }
        model.addAttribute("principal", principal);
        if (error != null) {
            model.addAttribute("errorMessage", resolveMfaErrorMessage(request));
        }
        return "mfa";
    }

    @GetMapping(path = "/consent", produces = MediaType.TEXT_HTML_VALUE)
    @RegisterReflectionForBinding(String.class)
    public String consent(Principal principal, Model model,
                          @RequestParam(OAuth2ParameterNames.CLIENT_ID) String clientId,
                          @RequestParam(OAuth2ParameterNames.SCOPE) String scope,
                          @RequestParam(OAuth2ParameterNames.STATE) String state,
                          @RequestParam(name = OAuth2ParameterNames.USER_CODE, required = false) String userCode) {
        RegisteredClient registeredClient = this.registeredClientRepository.findByClientId(clientId);
        assert registeredClient != null;
        String id = registeredClient.getId();
        OAuth2AuthorizationConsent currentAuthorizationConsent =
                this.authorizationConsentService.findById(id, principal.getName());
        Set<String> authorizedScopes = currentAuthorizationConsent != null
                ? currentAuthorizationConsent.getScopes()
                : Collections.emptySet();
        Set<OAuth2Scope> scopesToApproves = new HashSet<>();
        Set<OAuth2Scope> previouslyApprovedScopesSet = new HashSet<>();
        String[] scopes = StringUtils.delimitedListToStringArray(scope, " ");
        List<OAuth2Scope> oAuth2Scopes =
                oAuth2ScopeRepository.findAllByClientIdAndScopeIn(clientId, Arrays.asList(scopes));

        oAuth2Scopes.forEach(oAuth2Scope -> {
            if (authorizedScopes.contains(oAuth2Scope.getScope())) {
                previouslyApprovedScopesSet.add(oAuth2Scope);
            } else {
                scopesToApproves.add(oAuth2Scope);
            }
        });

        model.addAttribute("userCode", userCode);
        model.addAttribute("requestUri", StringUtils.hasText(userCode)
                ? authorizationServerSettings.getDeviceVerificationEndpoint()
                : authorizationServerSettings.getAuthorizationEndpoint());
        model.addAttribute("clientId", clientId);
        model.addAttribute("clientName", registeredClient.getClientName());
        model.addAttribute("state", state);
        model.addAttribute("scopes", scopesToApproves);
        model.addAttribute("previouslyApprovedScopes", previouslyApprovedScopesSet);
        model.addAttribute("principalName", principal.getName());
        return "consent";
    }

    private List<FederatedLoginProvider> getFederatedProviders() {
        List<FederatedLoginProvider> providers = new ArrayList<>();
        Set<String> knownRegistrationIds = new HashSet<>();
        if (this.identityProviderRepository != null) {
            this.identityProviderRepository.findAll().forEach(provider ->
                    knownRegistrationIds.add(provider.getRegistrationId()));
            this.identityProviderRepository.findByEnabledTrueAndDisplayOnLoginTrueOrderByDisplayOrderAsc()
                    .stream()
                    .filter(this::isFederatedLoginProvider)
                    .forEach(provider -> providers.add(new FederatedLoginProvider(
                            provider.getRegistrationId(),
                            provider.getProviderName(),
                            authorizationUrl(provider),
                            resolveIconKey(provider.getIconKey(), provider.getProviderType(), provider.getRegistrationId()),
                            provider.getIconUrl(),
                            resolveIconText(provider.getIconKey(), provider.getProviderType(), provider.getRegistrationId()))));
        }
        if (this.clientRegistrationRepository instanceof Iterable<?> registrations) {
            for (Object registration : registrations) {
                if (registration instanceof ClientRegistration clientRegistration) {
                    if (!knownRegistrationIds.add(clientRegistration.getRegistrationId())) {
                        continue;
                    }
                    if (!isConfiguredProviderDisplayed(clientRegistration.getRegistrationId())) {
                        continue;
                    }
                    String iconKey = normalizeIconKey(clientRegistration.getRegistrationId());
                    providers.add(new FederatedLoginProvider(
                            clientRegistration.getRegistrationId(),
                            clientRegistration.getClientName(),
                            "/oauth2/authorization/" + clientRegistration.getRegistrationId(),
                            iconKey,
                            null,
                            resolveIconText(iconKey, null, clientRegistration.getRegistrationId())));
                }
            }
        }
        if (this.relyingPartyRegistrationRepository instanceof Iterable<?> registrations) {
            for (Object registration : registrations) {
                if (registration instanceof RelyingPartyRegistration relyingPartyRegistration) {
                    if (!knownRegistrationIds.add(relyingPartyRegistration.getRegistrationId())) {
                        continue;
                    }
                    providers.add(new FederatedLoginProvider(
                            relyingPartyRegistration.getRegistrationId(),
                            relyingPartyRegistration.getRegistrationId(),
                            "/saml2/authorization/" + relyingPartyRegistration.getRegistrationId(),
                            "saml",
                            null,
                            "SSO"));
                }
            }
        }
        return providers;
    }

    private boolean isConfiguredProviderDisplayed(String registrationId) {
        if (this.displayPreferenceRepository == null || !StringUtils.hasText(registrationId)) {
            return true;
        }
        return this.displayPreferenceRepository.findById(registrationId)
                .map(preference -> preference.getDisplayOnLogin() == null || preference.getDisplayOnLogin())
                .orElse(true);
    }

    private boolean isFederatedLoginProvider(IdentityProvider provider) {
        return provider.getProviderType() != IdentityProvider.ProviderType.LDAP;
    }

    private String authorizationUrl(IdentityProvider provider) {
        if (provider.getProviderType() == IdentityProvider.ProviderType.SAML) {
            return "/saml2/authorization/" + provider.getRegistrationId();
        }
        return "/oauth2/authorization/" + provider.getRegistrationId();
    }

    private String resolveIconKey(String iconKey, IdentityProvider.ProviderType providerType, String registrationId) {
        if (StringUtils.hasText(iconKey)) {
            return normalizeIconKey(iconKey);
        }
        if (providerType != null) {
            return normalizeIconKey(providerType.name());
        }
        return normalizeIconKey(registrationId);
    }

    private String normalizeIconKey(String value) {
        if (!StringUtils.hasText(value)) {
            return "custom";
        }
        return value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private String resolveIconText(String iconKey, IdentityProvider.ProviderType providerType, String registrationId) {
        String resolvedIconKey = resolveIconKey(iconKey, providerType, registrationId);
        return switch (resolvedIconKey) {
            case "oidc" -> "OIDC";
            case "saml" -> "SSO";
            case "ldap" -> "LDAP";
            case "wechat", "weixin" -> "微";
            case "wecom" -> "企";
            case "dingtalk" -> "钉";
            case "feishu" -> "飞";
            case "alipay" -> "支";
            case "qq" -> "QQ";
            case "gitee" -> "G";
            case "github" -> "GH";
            case "google" -> "G";
            case "microsoft" -> "MS";
            case "custom" -> "ID";
            default -> fallbackIconText(registrationId);
        };
    }

    private String fallbackIconText(String value) {
        String normalized = normalizeIconKey(value).replace("-", "");
        if (normalized.length() <= 2) {
            return normalized.toUpperCase(Locale.ROOT);
        }
        return normalized.substring(0, 2).toUpperCase(Locale.ROOT);
    }

    private void saveAuthenticationException(HttpServletRequest request, Exception exception) {
        request.getSession(true).setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, exception);
    }

    private String resolveLoginErrorMessage(HttpServletRequest request) {
        String message = popAuthenticationExceptionMessage(request);
        if (!StringUtils.hasText(message)) {
            return "账号或密码不正确，请检查后重新登录。";
        }
        String normalizedMessage = message.trim();
        if (containsHan(normalizedMessage)) {
            return normalizedMessage;
        }
        String lowerMessage = normalizedMessage.toLowerCase(Locale.ROOT);
        return switch (lowerMessage) {
            case "user is disabled" -> "账号已被禁用，请联系管理员。";
            case "user account is locked" -> "账号已被锁定，请稍后再试或联系管理员。";
            case "user account has expired" -> "账号已过期，请联系管理员。";
            case "user credentials have expired" -> "密码已过期，请联系管理员。";
            default -> "账号或密码不正确，请检查后重新登录。";
        };
    }

    private String resolveMfaErrorMessage(HttpServletRequest request) {
        String message = popAuthenticationExceptionMessage(request);
        if (!StringUtils.hasText(message)) {
            return "验证码不正确，请检查后重试。";
        }
        String normalizedMessage = message.trim();
        if (containsHan(normalizedMessage)) {
            return normalizedMessage;
        }
        String lowerMessage = normalizedMessage.toLowerCase(Locale.ROOT);
        return switch (lowerMessage) {
            case "mfa factor not found" -> "当前二次验证方式不可用，请返回登录页重新登录。";
            case "no mfa factors configured" -> "当前账号尚未配置二次验证方式，请返回登录页重新登录。";
            default -> "验证码不正确，请检查后重试。";
        };
    }

    private String popAuthenticationExceptionMessage(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        try {
            Object authenticationException = session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
            if (authenticationException instanceof Exception exception) {
                return exception.getMessage();
            }
            return null;
        } finally {
            session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        }
    }

    private boolean containsHan(String value) {
        for (int index = 0; index < value.length(); index++) {
            if (Character.UnicodeScript.of(value.charAt(index)) == Character.UnicodeScript.HAN) {
                return true;
            }
        }
        return false;
    }

    private String resolveSelectedLoginTab(String tab) {
        if ("captcha".equals(tab) || "graphic".equals(tab)) {
            return graphicCaptchaService != null ? "graphic" : "password";
        }
        if ("sms".equals(tab)) {
            return smsOtpService != null ? "sms" : "password";
        }
        return "password";
    }

    private String captchaFailureTab(String captchaId) {
        return StringUtils.hasText(captchaId) ? "graphic" : "sms";
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty()) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty()) {
            return ip;
        }
        return request.getRemoteAddr();
    }

    public static final class FederatedLoginProvider {

        private final String registrationId;

        private final String clientName;

        private final String authorizationUrl;

        private final String iconKey;

        private final String iconUrl;

        private final String iconText;

        public FederatedLoginProvider(String registrationId, String clientName, String authorizationUrl,
                                      String iconKey, String iconUrl, String iconText) {
            this.registrationId = registrationId;
            this.clientName = StringUtils.hasText(clientName) ? clientName : registrationId;
            this.authorizationUrl = authorizationUrl;
            this.iconKey = StringUtils.hasText(iconKey) ? iconKey : "custom";
            this.iconUrl = StringUtils.hasText(iconUrl) ? iconUrl : null;
            this.iconText = StringUtils.hasText(iconText) ? iconText : "ID";
        }

        public String getRegistrationId() {
            return registrationId;
        }

        public String getClientName() {
            return clientName;
        }

        public String getAuthorizationUrl() {
            return authorizationUrl;
        }

        public String getIconKey() {
            return iconKey;
        }

        public String getIconUrl() {
            return iconUrl;
        }

        public String getIconText() {
            return iconText;
        }
    }
}
