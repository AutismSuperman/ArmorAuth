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
package com.armorauth.authorization.client;

import com.armorauth.data.entity.OAuth2ClientSettings;
import com.armorauth.data.entity.OAuth2TokenSettings;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import static org.assertj.core.api.Assertions.assertThat;

class ClientTransformUtilTest {

    @Test
    void resolvesSpringSecurityClientAuthenticationMethods() {
        assertThat(ClientTransformUtil.resolveClientAuthenticationMethod("client_secret_jwt"))
                .isEqualTo(ClientAuthenticationMethod.CLIENT_SECRET_JWT);
        assertThat(ClientTransformUtil.resolveClientAuthenticationMethod("private_key_jwt"))
                .isEqualTo(ClientAuthenticationMethod.PRIVATE_KEY_JWT);
        assertThat(ClientTransformUtil.resolveClientAuthenticationMethod("tls_client_auth"))
                .isEqualTo(ClientAuthenticationMethod.TLS_CLIENT_AUTH);
        assertThat(ClientTransformUtil.resolveClientAuthenticationMethod("self_signed_tls_client_auth"))
                .isEqualTo(ClientAuthenticationMethod.SELF_SIGNED_TLS_CLIENT_AUTH);
    }

    @Test
    void mapsX509ClientAndTokenSettingsToSpringSecuritySettings() {
        OAuth2ClientSettings clientSettings = new OAuth2ClientSettings();
        clientSettings.setRequireAuthorizationConsent(false);
        clientSettings.setRequireProofKey(false);
        clientSettings.setX509CertificateSubjectDN("CN=client.example.com,O=ArmorAuth");

        OAuth2TokenSettings tokenSettings = new OAuth2TokenSettings();
        tokenSettings.setReuseRefreshTokens(false);
        tokenSettings.setX509CertificateBoundAccessTokens(true);

        ClientSettings springClientSettings = ClientTransformUtil.toClientSettings(clientSettings);
        TokenSettings springTokenSettings = ClientTransformUtil.toTokenSettings(tokenSettings);

        assertThat(springClientSettings.getX509CertificateSubjectDN())
                .isEqualTo("CN=client.example.com,O=ArmorAuth");
        assertThat(springTokenSettings.isX509CertificateBoundAccessTokens()).isTrue();
    }

    @Test
    void mapsX509SettingsFromSpringSecuritySettings() {
        ClientSettings clientSettings = ClientSettings.builder()
                .requireAuthorizationConsent(false)
                .requireProofKey(false)
                .x509CertificateSubjectDN("CN=client.example.com,O=ArmorAuth")
                .build();
        TokenSettings tokenSettings = TokenSettings.builder()
                .x509CertificateBoundAccessTokens(true)
                .build();

        OAuth2ClientSettings entityClientSettings = ClientTransformUtil.fromClientSettings(clientSettings);
        OAuth2TokenSettings entityTokenSettings = ClientTransformUtil.fromTokenSettings(tokenSettings);

        assertThat(entityClientSettings.getX509CertificateSubjectDN())
                .isEqualTo("CN=client.example.com,O=ArmorAuth");
        assertThat(entityTokenSettings.getX509CertificateBoundAccessTokens()).isTrue();
    }

    @Test
    void mapsDpopPolicyThroughClientSettings() {
        OAuth2ClientSettings clientSettings = new OAuth2ClientSettings();
        clientSettings.setRequireAuthorizationConsent(false);
        clientSettings.setRequireProofKey(true);
        clientSettings.setDpopEnabled(true);
        clientSettings.setDpopRequired(true);
        clientSettings.setDpopAllowedAlgorithms("ES256,ES384");

        ClientSettings springClientSettings = ClientTransformUtil.toClientSettings(clientSettings);

        assertThat(springClientSettings.<Boolean>getSetting(ClientTransformUtil.CLIENT_SETTING_DPOP_ENABLED)).isTrue();
        assertThat(springClientSettings.<Boolean>getSetting(ClientTransformUtil.CLIENT_SETTING_DPOP_REQUIRED)).isTrue();
        assertThat(springClientSettings.<String>getSetting(ClientTransformUtil.CLIENT_SETTING_DPOP_ALLOWED_ALGORITHMS))
                .isEqualTo("ES256,ES384");

        OAuth2ClientSettings mapped = ClientTransformUtil.fromClientSettings(springClientSettings);
        assertThat(mapped.getDpopEnabled()).isTrue();
        assertThat(mapped.getDpopRequired()).isTrue();
        assertThat(mapped.getDpopAllowedAlgorithms()).isEqualTo("ES256,ES384");
    }
}
