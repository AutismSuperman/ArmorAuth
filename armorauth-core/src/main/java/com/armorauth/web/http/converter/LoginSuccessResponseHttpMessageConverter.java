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
package com.armorauth.web.http.converter;

import com.armorauth.web.converter.DefaultLoginSuccessTokenResponseMapConverter;
import com.armorauth.web.converter.DefaultMapLoginSuccessResponseConverter;
import com.armorauth.web.endpoint.LoginSuccessResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.*;
import org.springframework.util.Assert;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * A {@link HttpMessageConverter} for an {@link LoginSuccessResponse }
 *
 * @author FuLin
 * @see AbstractHttpMessageConverter
 * @see LoginSuccessResponse
 * @since 1.0.0
 */
public class LoginSuccessResponseHttpMessageConverter extends AbstractHttpMessageConverter<LoginSuccessResponse> {

    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;


    private static final ParameterizedTypeReference<Map<String, Object>> STRING_OBJECT_MAP = new ParameterizedTypeReference<>() {
    };

    private GenericHttpMessageConverter<Object> jsonMessageConverter = HttpMessageConverters.getJsonMessageConverter();

    private Converter<Map<String, Object>, LoginSuccessResponse> loginSuccessResponseConverter = new DefaultLoginSuccessTokenResponseMapConverter();

    private Converter<LoginSuccessResponse, Map<String, Object>> loginSuccessResponseParametersConverter = new DefaultMapLoginSuccessResponseConverter();


    public LoginSuccessResponseHttpMessageConverter() {
        super(DEFAULT_CHARSET, MediaType.APPLICATION_JSON, new MediaType("application", "*+json"));
    }


    @Override
    protected boolean supports(Class<?> clazz) {
        return LoginSuccessResponse.class.isAssignableFrom(clazz);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected LoginSuccessResponse readInternal(Class<? extends LoginSuccessResponse> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        try {
            Map<String, Object> loginSuccessParameters = (Map<String, Object>) this.jsonMessageConverter
                    .read(STRING_OBJECT_MAP.getType(), null, inputMessage);
            return loginSuccessResponseConverter.convert(loginSuccessParameters);
        } catch (Exception ex) {
            throw new HttpMessageNotReadableException(
                    "An error occurred reading the Login Success Response: " + ex.getMessage(), ex,
                    inputMessage);
        }

    }

    @Override
    protected void writeInternal(LoginSuccessResponse loginSuccessResponse, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        try {
            Map<String, Object> loginSuccessResponseParameters = loginSuccessResponseParametersConverter.convert(loginSuccessResponse);
            Assert.notNull(loginSuccessResponseParameters, "loginSuccessResponseParameters cannot be null");
            this.jsonMessageConverter.write(loginSuccessResponseParameters, STRING_OBJECT_MAP.getType(),
                    MediaType.APPLICATION_JSON, outputMessage);
        } catch (Exception ex) {
            throw new HttpMessageNotWritableException("An error occurred writing the Login Success Response: " + ex.getMessage(), ex);
        }
    }


    public void setLoginSuccessResponseConverter(Converter<Map<String, Object>, LoginSuccessResponse> loginSuccessResponseConverter) {
        Assert.notNull(loginSuccessResponseConverter, "loginSuccessResponseConverter cannot be null");
        this.loginSuccessResponseConverter = loginSuccessResponseConverter;
    }

    public void setLoginSuccessResponseParametersConverter(Converter<LoginSuccessResponse, Map<String, Object>> loginSuccessResponseParametersConverter) {
        Assert.notNull(loginSuccessResponseParametersConverter, "loginSuccessResponseParametersConverter cannot be null");
        this.loginSuccessResponseParametersConverter = loginSuccessResponseParametersConverter;
    }
}
