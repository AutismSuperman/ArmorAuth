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

import com.armorauth.crypto.SecretCryptoService;
import com.armorauth.data.entity.IdentityProvider;
import com.armorauth.data.repository.IdentityProviderRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import java.util.List;

/**
 * 动态 ClientRegistration 仓库
 * <p>
 * 从 identity_provider 表加载身份源配置，转换为 Spring Security ClientRegistration。
 *
 * @author fulin
 * @since 2026-05-23
 */
public class DynamicClientRegistrationRepository implements ClientRegistrationRepository, Iterable<ClientRegistration> {

    private final IdentityProviderRepository idpRepository;
    private final SecretCryptoService secretCryptoService;

    public DynamicClientRegistrationRepository(IdentityProviderRepository idpRepository) {
        this(idpRepository, null);
    }

    public DynamicClientRegistrationRepository(IdentityProviderRepository idpRepository,
                                               SecretCryptoService secretCryptoService) {
        this.idpRepository = idpRepository;
        this.secretCryptoService = secretCryptoService;
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        return idpRepository.findByRegistrationId(registrationId)
                .filter(IdentityProvider::getEnabled)
                .filter(this::isOAuthClientProvider)
                .map(this::toClientRegistration)
                .orElse(null);
    }

    @Override
    public java.util.Iterator<ClientRegistration> iterator() {
        return findAll().iterator();
    }

    public List<ClientRegistration> findAll() {
        return idpRepository.findByEnabledTrueOrderByDisplayOrderAsc().stream()
                .filter(this::isOAuthClientProvider)
                .map(this::toClientRegistration)
                .toList();
    }

    private boolean isOAuthClientProvider(IdentityProvider idp) {
        return idp.getProviderType() != IdentityProvider.ProviderType.SAML
                && idp.getProviderType() != IdentityProvider.ProviderType.LDAP;
    }

    private ClientRegistration toClientRegistration(IdentityProvider idp) {
        ClientRegistration.Builder builder = ClientRegistration.withRegistrationId(idp.getRegistrationId())
                .clientId(idp.getClientId())
                .clientSecret(revealSecret(idp.getClientSecret()))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}");

        if (idp.getAuthorizationUri() != null) {
            builder.authorizationUri(idp.getAuthorizationUri());
        }
        if (idp.getTokenUri() != null) {
            builder.tokenUri(idp.getTokenUri());
        }
        if (idp.getUserinfoUri() != null) {
            builder.userInfoUri(idp.getUserinfoUri());
        }
        if (idp.getJwkSetUri() != null) {
            builder.jwkSetUri(idp.getJwkSetUri());
        }
        if (idp.getScopes() != null) {
            builder.scope(idp.getScopes().split(","));
        }

        builder.userNameAttributeName("sub");

        // 根据 provider type 设置默认值
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

    private String revealSecret(String secret) {
        return secretCryptoService != null ? secretCryptoService.reveal(secret) : secret;
    }
}
