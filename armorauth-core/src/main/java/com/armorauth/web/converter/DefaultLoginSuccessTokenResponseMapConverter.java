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
package com.armorauth.web.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.armorauth.web.endpoint.LoginSuccessResponse;
import lombok.Data;
import org.springframework.core.convert.converter.Converter;

import java.util.Map;

@Data
public class DefaultLoginSuccessTokenResponseMapConverter
        implements Converter<Map<String, Object>, LoginSuccessResponse> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public LoginSuccessResponse convert(Map<String, Object> source) {
        return objectMapper.convertValue(source, new TypeReference<>() {
        });
    }


}
