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
package com.armorauth.data.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * 身份源配置
 *
 * @author fulin
 * @since 2026-05-23
 */
@Data
@Entity
@Table(name = "identity_provider")
public class IdentityProvider implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public enum ProviderType {
        OIDC, SAML, LDAP, WECHAT, WECOM, DINGTALK, FEISHU, ALIPAY, QQ, GITEE, CUSTOM
    }

    public enum LinkingStrategy {
        AUTO_REGISTER, CONFIRM, EMAIL_MATCH, NONE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "provider_name", nullable = false, length = 100)
    private String providerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false, length = 50)
    private ProviderType providerType;

    @Column(name = "registration_id", unique = true, nullable = false, length = 100)
    private String registrationId;

    @Column(name = "client_id", length = 500)
    private String clientId;

    @Column(name = "client_secret", length = 500)
    private String clientSecret;

    @Column(name = "authorization_uri", length = 1000)
    private String authorizationUri;

    @Column(name = "token_uri", length = 1000)
    private String tokenUri;

    @Column(name = "userinfo_uri", length = 1000)
    private String userinfoUri;

    @Column(name = "jwk_set_uri", length = 1000)
    private String jwkSetUri;

    @Column(name = "saml_entity_id", length = 500)
    private String samlEntityId;

    @Column(name = "saml_sso_url", length = 1000)
    private String samlSsoUrl;

    @Column(name = "saml_slo_url", length = 1000)
    private String samlSloUrl;

    @Column(name = "saml_x509_certificate", columnDefinition = "text")
    private String samlX509Certificate;

    @Column(name = "saml_metadata_url", length = 1000)
    private String samlMetadataUrl;

    @Column(name = "saml_sp_entity_id", length = 500)
    private String samlSpEntityId;

    @Column(name = "saml_acs_url", length = 1000)
    private String samlAcsUrl;

    @Column(name = "saml_name_id_format", length = 200)
    private String samlNameIdFormat;

    @Column(name = "ldap_url", length = 1000)
    private String ldapUrl;

    @Column(name = "ldap_base_dn", length = 500)
    private String ldapBaseDn;

    @Column(name = "ldap_bind_dn", length = 500)
    private String ldapBindDn;

    @Column(name = "ldap_bind_password", length = 1000)
    private String ldapBindPassword;

    @Column(name = "ldap_user_search_base", length = 500)
    private String ldapUserSearchBase;

    @Column(name = "ldap_user_search_filter", length = 500)
    private String ldapUserSearchFilter;

    @Column(name = "ldap_username_attribute", length = 100)
    private String ldapUsernameAttribute;

    @Column(name = "ldap_email_attribute", length = 100)
    private String ldapEmailAttribute;

    @Column(name = "ldap_phone_attribute", length = 100)
    private String ldapPhoneAttribute;

    @Column(name = "ldap_display_name_attribute", length = 100)
    private String ldapDisplayNameAttribute;

    @Column(name = "ldap_group_attribute", length = 100)
    private String ldapGroupAttribute;

    @Column(name = "ldap_use_ssl")
    private Boolean ldapUseSsl;

    @Column(name = "ldap_start_tls")
    private Boolean ldapStartTls;

    @Column(name = "ldap_page_size")
    private Integer ldapPageSize;

    @Column(name = "scopes", length = 500)
    private String scopes;

    @Column(name = "attribute_mapping", columnDefinition = "text")
    private String attributeMapping;

    @Enumerated(EnumType.STRING)
    @Column(name = "linking_strategy", length = 50)
    private LinkingStrategy linkingStrategy = LinkingStrategy.AUTO_REGISTER;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
