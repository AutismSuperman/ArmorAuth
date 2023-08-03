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
package com.armorauth.config;


import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.endpoint.*;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.*;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {


    @Bean
    SecurityFilterChain customSecurityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeRequests(authorizeRequests -> authorizeRequests
                        .anyRequest().authenticated())
                .formLogin(Customizer.withDefaults())
        ;
        return http.build();
    }

    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientRepository authorizedClientRepository) {
        OAuth2AuthorizedClientProvider authorizedClientProvider =
                OAuth2AuthorizedClientProviderBuilder.builder()
                        .clientCredentials(clientCredentials -> {
                            clientCredentials.accessTokenResponseClient(accessTokenResponseClient());
                        })
                        .refreshToken()
                        .build();
        DefaultOAuth2AuthorizedClientManager authorizedClientManager =
                new DefaultOAuth2AuthorizedClientManager(
                        clientRegistrationRepository, authorizedClientRepository);
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
        return authorizedClientManager;
    }


    private OAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest> accessTokenResponseClient() {
        OAuth2ClientCredentialsGrantRequestEntityConverter requestEntityConverter = new OAuth2ClientCredentialsGrantRequestEntityConverter();
        NimbusJwtClientAuthenticationParametersConverter<OAuth2ClientCredentialsGrantRequest>
                converter = new NimbusJwtClientAuthenticationParametersConverter<>(jwkResolver);
        requestEntityConverter.addParametersConverter(converter);
        requestEntityConverter.addParametersConverter(new ClientIdClientAuthenticationParametersConverter<>());
        DefaultClientCredentialsTokenResponseClient tokenResponseClient = new DefaultClientCredentialsTokenResponseClient();
        tokenResponseClient.setRequestEntityConverter(requestEntityConverter);
        return tokenResponseClient;
    }

    Function<ClientRegistration, JWK> jwkResolver = (clientRegistration) -> {
        if (clientRegistration.getClientAuthenticationMethod().equals(ClientAuthenticationMethod.CLIENT_SECRET_JWT)) {
            SecretKeySpec secretKey = new SecretKeySpec(
                    clientRegistration.getClientSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");
            return new OctetSequenceKey.Builder(secretKey)
                    .keyID(UUID.randomUUID().toString())
                    .build();
        }
        return null;
    };

    public static class ClientIdClientAuthenticationParametersConverter<T extends AbstractOAuth2AuthorizationGrantRequest>
            implements Converter<T, MultiValueMap<String, String>> {
        @Override
        public MultiValueMap<String, String> convert(T authorizationGrantRequest) {
            Assert.notNull(authorizationGrantRequest, "authorizationGrantRequest cannot be null");
            ClientRegistration clientRegistration = authorizationGrantRequest.getClientRegistration();
            MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
            parameters.set(OAuth2ParameterNames.CLIENT_ID, clientRegistration.getClientId());
            return parameters;
        }
    }

}
