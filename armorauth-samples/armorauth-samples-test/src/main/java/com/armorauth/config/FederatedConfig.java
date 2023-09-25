package com.armorauth.config;


import com.armorauth.federation.ExtendedOAuth2ClientPropertiesMapper;
import com.armorauth.federation.web.configurers.FederatedLoginConfigurer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationEntryPointFailureHandler;

import java.util.Map;

@EnableWebSecurity(debug = true)
@Configuration
public class FederatedConfig {


    @Bean
    public SecurityFilterChain federatedConfigSecurityFilterChain(HttpSecurity httpSecurity,
                                                                  ClientRegistrationRepository clientRegistrationRepository
    ) throws Exception {
        FederatedAuthenticationEntryPoint authenticationEntryPoint =
                new FederatedAuthenticationEntryPoint("/login", clientRegistrationRepository);
        httpSecurity.exceptionHandling(exceptionHandling ->
                exceptionHandling
                        .authenticationEntryPoint(authenticationEntryPoint)
        );
        FederatedLoginConfigurer federatedLoginConfigurer = new FederatedLoginConfigurer();
        federatedLoginConfigurer.failureHandler(new AuthenticationEntryPointFailureHandler(authenticationEntryPoint));
        httpSecurity.apply(federatedLoginConfigurer);
        return httpSecurity.build();
    }


    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(@Autowired(required = false) OAuth2ClientProperties properties) {
        InMemoryClientRegistrationRepository clientRegistrations;
        ExtendedOAuth2ClientPropertiesMapper extendedOAuth2ClientPropertiesMapper = new ExtendedOAuth2ClientPropertiesMapper(properties);
        Map<String, ClientRegistration> extendedClientRegistrations = extendedOAuth2ClientPropertiesMapper.asClientRegistrations();
        clientRegistrations = new InMemoryClientRegistrationRepository(extendedClientRegistrations);
        return clientRegistrations;
    }


}
