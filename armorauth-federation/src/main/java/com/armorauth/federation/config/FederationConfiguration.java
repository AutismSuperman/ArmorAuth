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
package com.armorauth.federation.config;

import com.armorauth.config.ArmorAuthSecurityCustomizer;
import com.armorauth.crypto.SecretCryptoService;
import com.armorauth.data.entity.IdentityProvider;
import com.armorauth.data.repository.IdentityProviderRepository;
import com.armorauth.federation.DynamicRelyingPartyRegistrationRepository;
import com.armorauth.federation.FederatedLoginOrchestrator;
import com.armorauth.federation.provider.ExtendedOAuth2ClientPropertiesMapper;
import com.armorauth.federation.provider.FederatedOAuth2ProviderRegistry;
import com.armorauth.federation.configurer.OAuth2FederatedLoginServerConfigurer;
import com.armorauth.federation.security.FederatedAuthenticationSuccessHandler;
import com.armorauth.federation.security.FederatedSamlAuthenticationSuccessHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.client.JdbcOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.RequestCache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OAuth2ClientProperties.class)
public class FederationConfiguration {

    @Bean
    @Primary
    public FederatedAuthenticationSuccessHandler federatedAuthenticationSuccessHandler(
            RequestCache requestCache,
            FederatedLoginOrchestrator federatedLoginOrchestrator) {
        FederatedAuthenticationSuccessHandler successHandler =
                new FederatedAuthenticationSuccessHandler("/", requestCache);
        successHandler.setFederatedLoginOrchestrator(federatedLoginOrchestrator);
        return successHandler;
    }

    @Bean
    public FederatedSamlAuthenticationSuccessHandler federatedSamlAuthenticationSuccessHandler(
            RequestCache requestCache,
            ObjectMapper objectMapper,
            FederatedLoginOrchestrator federatedLoginOrchestrator) {
        return new FederatedSamlAuthenticationSuccessHandler(
                requestCache, objectMapper, federatedLoginOrchestrator);
    }

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(
            @Autowired(required = false) OAuth2ClientProperties properties,
            FederatedOAuth2ProviderRegistry providerRegistry,
            ObjectProvider<IdentityProviderRepository> idpRepositoryProvider,
            SecretCryptoService secretCryptoService) {
        List<ClientRegistration> registrations = new ArrayList<>();

        // 从 application.yml 加载配置文件中的身份源
        if (properties != null && !properties.getRegistration().isEmpty()) {
            ExtendedOAuth2ClientPropertiesMapper propertiesMapper =
                    new ExtendedOAuth2ClientPropertiesMapper(properties, providerRegistry);
            Map<String, ClientRegistration> clientRegistrations = propertiesMapper.asClientRegistrations();
            registrations.addAll(clientRegistrations.values());
        }

        // 从数据库加载动态身份源配置
        IdentityProviderRepository idpRepository = idpRepositoryProvider.getIfAvailable();
        if (idpRepository != null) {
            List<ClientRegistration> dbRegistrations = idpRepository.findByEnabledTrueOrderByDisplayOrderAsc().stream()
                    .filter(this::isOAuthClientProvider)
                    .map(idp -> toClientRegistration(idp, secretCryptoService))
                    .toList();
            // 数据库配置优先，同 registrationId 覆盖配置文件
            for (ClientRegistration dbReg : dbRegistrations) {
                registrations.removeIf(r -> r.getRegistrationId().equals(dbReg.getRegistrationId()));
                registrations.add(dbReg);
            }
        }

        if (registrations.isEmpty()) {
            return new EmptyClientRegistrationRepository();
        }
        return new InMemoryClientRegistrationRepository(registrations);
    }

    @Bean
    public RelyingPartyRegistrationRepository relyingPartyRegistrationRepository(
            ObjectProvider<IdentityProviderRepository> idpRepositoryProvider) {
        IdentityProviderRepository idpRepository = idpRepositoryProvider.getIfAvailable();
        if (idpRepository == null) {
            return new EmptyRelyingPartyRegistrationRepository();
        }
        return new DynamicRelyingPartyRegistrationRepository(idpRepository);
    }

    private boolean isOAuthClientProvider(IdentityProvider idp) {
        return idp.getProviderType() != IdentityProvider.ProviderType.SAML
                && idp.getProviderType() != IdentityProvider.ProviderType.LDAP;
    }

    private ClientRegistration toClientRegistration(IdentityProvider idp, SecretCryptoService secretCryptoService) {
        ClientRegistration.Builder builder = ClientRegistration.withRegistrationId(idp.getRegistrationId())
                .clientId(idp.getClientId())
                .clientSecret(secretCryptoService.reveal(idp.getClientSecret()))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}");

        if (idp.getAuthorizationUri() != null) builder.authorizationUri(idp.getAuthorizationUri());
        if (idp.getTokenUri() != null) builder.tokenUri(idp.getTokenUri());
        if (idp.getUserinfoUri() != null) builder.userInfoUri(idp.getUserinfoUri());
        if (idp.getJwkSetUri() != null) builder.jwkSetUri(idp.getJwkSetUri());
        if (idp.getScopes() != null) builder.scope(idp.getScopes().split(","));

        builder.userNameAttributeName("sub");

        if (idp.getProviderType() != null) {
            switch (idp.getProviderType()) {
                case WECHAT -> {
                    if (idp.getScopes() == null) builder.scope("snsapi_login");
                    builder.userNameAttributeName("openid");
                }
                case WECOM -> {
                    if (idp.getScopes() == null) builder.scope("snsapi_userinfo");
                    builder.userNameAttributeName("userid");
                }
                case DINGTALK -> {
                    if (idp.getScopes() == null) builder.scope("openid");
                    builder.userNameAttributeName("openid");
                }
                case FEISHU -> {
                    if (idp.getScopes() == null) builder.scope("contact:user.id:readonly");
                    builder.userNameAttributeName("user_id");
                }
                case GITEE -> {
                    if (idp.getScopes() == null) builder.scope("user_info");
                    builder.userNameAttributeName("id");
                }
                case QQ -> {
                    if (idp.getScopes() == null) builder.scope("get_user_info");
                    builder.userNameAttributeName("openid");
                }
                default -> { /* OIDC and others use defaults */ }
            }
        }

        return builder.build();
    }

    @Bean
    public OAuth2AuthorizedClientService authorizedClientService(
            JdbcTemplate jdbcTemplate,
            ClientRegistrationRepository clientRegistrationRepository) {
        return new JdbcOAuth2AuthorizedClientService(jdbcTemplate, clientRegistrationRepository);
    }

    @Bean
    public ArmorAuthSecurityCustomizer federationSecurityCustomizer(FederationProperties federationProperties) {
        return http -> {
            if (!federationProperties.isEnabled()) {
                return;
            }
            http.with(new OAuth2FederatedLoginServerConfigurer(), federatedLogin ->
                    federatedLogin.federatedAuthorization(federatedAuthorization ->
                            federatedAuthorization.loginPageUrl("/login")));
        };
    }

    private static final class EmptyClientRegistrationRepository
            implements ClientRegistrationRepository, Iterable<ClientRegistration> {

        @Override
        public ClientRegistration findByRegistrationId(String registrationId) {
            return null;
        }

        @Override
        public Iterator<ClientRegistration> iterator() {
            return Collections.emptyIterator();
        }
    }

    private static final class EmptyRelyingPartyRegistrationRepository
            implements RelyingPartyRegistrationRepository, Iterable<RelyingPartyRegistration> {

        @Override
        public RelyingPartyRegistration findByRegistrationId(String registrationId) {
            return null;
        }

        @Override
        public Iterator<RelyingPartyRegistration> iterator() {
            return Collections.emptyIterator();
        }
    }

}
