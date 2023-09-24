package com.armorauth.federation;

import com.armorauth.federation.web.converter.OAuth2AuthorizationRequestConverter;
import com.armorauth.federation.wechat.web.converter.WechatAuthorizationRequestConverter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

public class DelegatingAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final DefaultOAuth2AuthorizationRequestResolver delegate;

    private final List<OAuth2AuthorizationRequestConverter> authorizationRequestConverters;

    public DelegatingAuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository,
                                                  String authorizationRequestBaseUri,
                                                  List<OAuth2AuthorizationRequestConverter> authorizationRequestConverters) {
        Assert.notNull(clientRegistrationRepository, "clientRegistrationRepository cannot be null");
        if (authorizationRequestBaseUri == null)
            authorizationRequestBaseUri = OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI;
        this.authorizationRequestConverters = authorizationRequestConverters;
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, authorizationRequestBaseUri);
        this.delegate.setAuthorizationRequestCustomizer(this::authorizationRequestCustomizer);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return delegate.resolve(request);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        return delegate.resolve(request, clientRegistrationId);
    }

    public void authorizationRequestCustomizer(OAuth2AuthorizationRequest.Builder builder) {
        builder.attributes(attribute -> {
            String registrationId = (String) attribute.get(OAuth2ParameterNames.REGISTRATION_ID);
            authorizationRequestConverters.stream()
                    .filter(authorizationRequestConverter -> authorizationRequestConverter.supports(registrationId))
                    .findAny()
                    .ifPresent(authorizationRequestConverter -> authorizationRequestConverter.convert(builder));
        });
    }

    public void addOAuth2AuthorizationRequestConverter(OAuth2AuthorizationRequestConverter authorizationRequestConverter) {
        this.authorizationRequestConverters.add(authorizationRequestConverter);
    }


}