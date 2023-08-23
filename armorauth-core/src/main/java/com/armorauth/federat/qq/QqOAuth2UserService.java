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
package com.armorauth.federat.qq;

import com.armorauth.federat.gitee.GiteeOAuth2User;
import com.armorauth.federat.wechat.WechatOAuth2User;
import com.armorauth.federat.wechat.WechatOAuth2UserService;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.*;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.http.converter.json.JsonbHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequestEntityConverter;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.util.*;
import org.springframework.web.client.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class QqOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private static final String MISSING_USER_INFO_URI_ERROR_CODE = "missing_user_info_uri";

    private static final String MISSING_OPENID_ERROR_CODE = "missing_openid_attribute";

    private static final String MISSING_USER_NAME_ATTRIBUTE_ERROR_CODE = "missing_user_name_attribute";

    private static final String INVALID_USER_INFO_RESPONSE_ERROR_CODE = "invalid_user_info_response";

    private static final String OPENID_KEY = "openid";

    private static final ParameterizedTypeReference<Map<String, Object>> PARAMETERIZED_RESPONSE_TYPE = new ParameterizedTypeReference<>() {
    };

    private static final ParameterizedTypeReference<QqOAuth2User> OAUTH2_USER_OBJECT = new ParameterizedTypeReference<>() {
    };


    private Converter<OAuth2UserRequest, RequestEntity<?>> requestEntityConverter = userRequest -> {
        MultiValueMap<String, String> queryParameters = new LinkedMultiValueMap<>();
        queryParameters.add(OAuth2ParameterNames.ACCESS_TOKEN, userRequest.getAccessToken().getTokenValue());
        queryParameters.add(QqParameterNames.OAUTH_CONSUMER_KEY, userRequest.getClientRegistration().getClientId());
        queryParameters.add(QqParameterNames.OPENID, userRequest.getAdditionalParameters().get(QqParameterNames.OPENID).toString());
        String userInfoUri = userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUri();
        URI uri = UriComponentsBuilder.fromUriString(userInfoUri).queryParams(queryParameters).build().toUri();
        return RequestEntity.get(uri).build();
    };

    private final RestOperations restOperations;

    public QqOAuth2UserService() {
        RestTemplate restTemplate = new RestTemplate(Collections.singletonList(new QqOAuth2UserService.QqOAuth2UserHttpMessageConverter()));
        restTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());
        this.restOperations = restTemplate;
    }

    @Override
    public QqOAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        Assert.notNull(userRequest, "userRequest cannot be null");
        ClientRegistration clientRegistration = userRequest.getClientRegistration();
        String registrationId = clientRegistration.getRegistrationId();
        if (!StringUtils
                .hasText(userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUri())) {
            OAuth2Error oauth2Error = new OAuth2Error(MISSING_USER_INFO_URI_ERROR_CODE,
                    "Missing required UserInfo Uri in UserInfoEndpoint for Client Registration: "
                            + userRequest.getClientRegistration().getRegistrationId(),
                    null);
            throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
        }
        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint()
                .getUserNameAttributeName();

        if (!StringUtils.hasText(userNameAttributeName)) {
            OAuth2Error oauth2Error = new OAuth2Error(MISSING_USER_NAME_ATTRIBUTE_ERROR_CODE,
                    "Missing required \"user name\" attribute name in UserInfoEndpoint for Client Registration: "
                            + userRequest.getClientRegistration().getRegistrationId(),
                    null);
            throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
        }
        String openid = (String) userRequest.getAdditionalParameters().get(userNameAttributeName);
        if (!StringUtils.hasText(openid)) {
            OAuth2Error oauth2Error = new OAuth2Error(MISSING_OPENID_ERROR_CODE,
                    "Missing required \"openid\" attribute in UserInfoEndpoint for Client Registration: "
                            + registrationId,
                    null);
            throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
        }
        RequestEntity<?> request = this.requestEntityConverter.convert(userRequest);
        ResponseEntity<Map<String, Object>> response = getResponse(userRequest, request);
        Map<String, Object> userAttributes = response.getBody();
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        authorities.add(new OAuth2UserAuthority(userAttributes));
        OAuth2AccessToken token = userRequest.getAccessToken();
        for (String authority : token.getScopes()) {
            authorities.add(new SimpleGrantedAuthority("SCOPE_" + authority));
        }
        return new QqOAuth2User(authorities, userAttributes, openid);
    }

    private ResponseEntity<Map<String, Object>> getResponse(OAuth2UserRequest userRequest, RequestEntity<?> request) {
        try {
            return this.restOperations.exchange(request, PARAMETERIZED_RESPONSE_TYPE);
        } catch (OAuth2AuthorizationException ex) {
            OAuth2Error oauth2Error = ex.getError();
            StringBuilder errorDetails = new StringBuilder();
            errorDetails.append("Error details: [");
            errorDetails.append("UserInfo Uri: ")
                    .append(userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUri());
            errorDetails.append(", Error Code: ").append(oauth2Error.getErrorCode());
            if (oauth2Error.getDescription() != null) {
                errorDetails.append(", Error Description: ").append(oauth2Error.getDescription());
            }
            errorDetails.append("]");
            oauth2Error = new OAuth2Error(INVALID_USER_INFO_RESPONSE_ERROR_CODE,
                    "An error occurred while attempting to retrieve the UserInfo Resource: " + errorDetails.toString(),
                    null);
            throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString(), ex);
        } catch (UnknownContentTypeException ex) {
            String errorMessage = "An error occurred while attempting to retrieve the UserInfo Resource from '"
                    + userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUri()
                    + "': response contains invalid content type '" + ex.getContentType().toString() + "'. "
                    + "The UserInfo Response should return a JSON object (content type 'application/json') "
                    + "that contains a collection of name and value pairs of the claims about the authenticated End-User. "
                    + "Please ensure the UserInfo Uri in UserInfoEndpoint for Client Registration '"
                    + userRequest.getClientRegistration().getRegistrationId() + "' conforms to the UserInfo Endpoint, "
                    + "as defined in OpenID Connect 1.0: 'https://openid.net/specs/openid-connect-core-1_0.html#UserInfo'";
            OAuth2Error oauth2Error = new OAuth2Error(INVALID_USER_INFO_RESPONSE_ERROR_CODE, errorMessage, null);
            throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString(), ex);
        } catch (RestClientException ex) {
            OAuth2Error oauth2Error = new OAuth2Error(INVALID_USER_INFO_RESPONSE_ERROR_CODE,
                    "An error occurred while attempting to retrieve the UserInfo Resource: " + ex.getMessage(), null);
            throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString(), ex);
        }
    }


    @SuppressWarnings("all")
    private static class QqOAuth2UserHttpMessageConverter
            extends AbstractHttpMessageConverter<QqOAuth2User> {
        private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
        private final GenericHttpMessageConverter<Object> jsonMessageConverter = HttpMessageConverters.getJsonMessageConverter();

        public QqOAuth2UserHttpMessageConverter() {
            super(DEFAULT_CHARSET, MediaType.TEXT_PLAIN,
                    MediaType.APPLICATION_JSON,
                    new MediaType("application", "*+json"));
        }

        @Override
        protected boolean supports(Class<?> clazz) {
            return QqOAuth2User.class.isAssignableFrom(clazz);
        }

        @Override
        protected QqOAuth2User readInternal(Class<? extends QqOAuth2User> clazz,
                                            HttpInputMessage inputMessage) throws HttpMessageNotReadableException {
            try {
                // gh-6463: Parse parameter values as Object in order to handle potential JSON
                // Object and then convert values to String
                return (QqOAuth2User) this.jsonMessageConverter
                        .read(OAUTH2_USER_OBJECT.getType(), null, inputMessage);
            } catch (Exception ex) {
                throw new HttpMessageNotReadableException(
                        "An error occurred reading the OAuth 2.0 Access Token Response: " + ex.getMessage(), ex,
                        inputMessage);
            }
        }

        @Override
        protected void writeInternal(QqOAuth2User user, HttpOutputMessage outputMessage)
                throws HttpMessageNotWritableException {
        }

        static class HttpMessageConverters {
            private static final boolean jackson2Present;
            private static final boolean gsonPresent;
            private static final boolean jsonbPresent;

            static {
                ClassLoader classLoader = HttpMessageConverters.class.getClassLoader();
                jackson2Present = ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", classLoader)
                        && ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", classLoader);
                gsonPresent = ClassUtils.isPresent("com.google.gson.Gson", classLoader);
                jsonbPresent = ClassUtils.isPresent("jakarta.json.bind.Jsonb", classLoader);
            }

            private HttpMessageConverters() {
            }

            static GenericHttpMessageConverter<Object> getJsonMessageConverter() {
                if (jackson2Present) {
                    return new MappingJackson2HttpMessageConverter();
                }
                if (gsonPresent) {
                    return new GsonHttpMessageConverter();
                }
                if (jsonbPresent) {
                    return new JsonbHttpMessageConverter();
                }
                return null;
            }

        }
    }


}