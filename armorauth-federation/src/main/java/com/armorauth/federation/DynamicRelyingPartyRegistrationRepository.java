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

import com.armorauth.data.entity.IdentityProvider;
import com.armorauth.data.repository.IdentityProviderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class DynamicRelyingPartyRegistrationRepository
        implements RelyingPartyRegistrationRepository, Iterable<RelyingPartyRegistration> {

    private static final Logger log = LoggerFactory.getLogger(DynamicRelyingPartyRegistrationRepository.class);

    private final IdentityProviderRepository identityProviderRepository;

    public DynamicRelyingPartyRegistrationRepository(IdentityProviderRepository identityProviderRepository) {
        this.identityProviderRepository = identityProviderRepository;
    }

    @Override
    public RelyingPartyRegistration findByRegistrationId(String registrationId) {
        if (!StringUtils.hasText(registrationId)) {
            return null;
        }
        return this.identityProviderRepository.findByRegistrationId(registrationId)
                .filter(this::isSamlProvider)
                .flatMap(this::toRelyingPartyRegistration)
                .orElse(null);
    }

    @Override
    public Iterator<RelyingPartyRegistration> iterator() {
        return findAll().iterator();
    }

    public List<RelyingPartyRegistration> findAll() {
        return this.identityProviderRepository.findByEnabledTrueOrderByDisplayOrderAsc().stream()
                .filter(this::isSamlProvider)
                .map(this::toRelyingPartyRegistration)
                .flatMap(Optional::stream)
                .toList();
    }

    private boolean isSamlProvider(IdentityProvider provider) {
        return Boolean.TRUE.equals(provider.getEnabled())
                && provider.getProviderType() == IdentityProvider.ProviderType.SAML;
    }

    private Optional<RelyingPartyRegistration> toRelyingPartyRegistration(IdentityProvider provider) {
        try {
            RelyingPartyRegistration.Builder builder = baseBuilder(provider);
            builder.registrationId(provider.getRegistrationId())
                    .entityId(resolveSpEntityId(provider))
                    .assertionConsumerServiceLocation(resolveAcsLocation(provider))
                    .assertionConsumerServiceBinding(Saml2MessageBinding.POST)
                    .authnRequestsSigned(false)
                    .assertingPartyMetadata(assertingParty -> assertingParty.wantAuthnRequestsSigned(false));
            if (StringUtils.hasText(provider.getSamlNameIdFormat())) {
                builder.nameIdFormat(provider.getSamlNameIdFormat().trim());
            }
            return Optional.of(builder.build());
        } catch (RuntimeException ex) {
            log.warn("Skipping invalid SAML provider registrationId={} reason={}",
                    provider.getRegistrationId(), ex.getMessage());
            return Optional.empty();
        }
    }

    private RelyingPartyRegistration.Builder baseBuilder(IdentityProvider provider) {
        if (StringUtils.hasText(provider.getSamlMetadataUrl())) {
            return RelyingPartyRegistrations.fromMetadataLocation(provider.getSamlMetadataUrl().trim());
        }
        if (!StringUtils.hasText(provider.getSamlEntityId())
                || !StringUtils.hasText(provider.getSamlSsoUrl())
                || !StringUtils.hasText(provider.getSamlX509Certificate())) {
            throw new IllegalArgumentException("SAML entityId, SSO URL and certificate are required");
        }
        X509Certificate certificate = parseCertificate(provider.getSamlX509Certificate());
        return RelyingPartyRegistration.withRegistrationId(provider.getRegistrationId())
                .assertingPartyMetadata(assertingParty -> assertingParty
                        .entityId(provider.getSamlEntityId().trim())
                        .wantAuthnRequestsSigned(false)
                        .singleSignOnServiceLocation(provider.getSamlSsoUrl().trim())
                        .singleSignOnServiceBinding(Saml2MessageBinding.REDIRECT)
                        .verificationX509Credentials(credentials ->
                                credentials.add(Saml2X509Credential.verification(certificate))));
    }

    private String resolveSpEntityId(IdentityProvider provider) {
        if (StringUtils.hasText(provider.getSamlSpEntityId())) {
            return provider.getSamlSpEntityId().trim();
        }
        return "{baseUrl}/saml2/service-provider-metadata/{registrationId}";
    }

    private String resolveAcsLocation(IdentityProvider provider) {
        if (StringUtils.hasText(provider.getSamlAcsUrl())) {
            return provider.getSamlAcsUrl().trim();
        }
        return "{baseUrl}/login/saml2/sso/{registrationId}";
    }

    private X509Certificate parseCertificate(String value) {
        try {
            String certificate = value.trim();
            if (certificate.contains("-----BEGIN CERTIFICATE-----")) {
                int begin = certificate.indexOf("-----BEGIN CERTIFICATE-----");
                int end = certificate.indexOf("-----END CERTIFICATE-----", begin);
                if (begin < 0 || end < 0) {
                    throw new IllegalArgumentException("Invalid PEM certificate");
                }
                certificate = certificate.substring(begin + "-----BEGIN CERTIFICATE-----".length(), end);
            }
            certificate = certificate.replaceAll("\\s", "");
            byte[] der = Base64.getDecoder().decode(certificate);
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(der));
        } catch (IllegalArgumentException | CertificateException ex) {
            throw new IllegalArgumentException("Invalid SAML X.509 certificate", ex);
        }
    }
}
