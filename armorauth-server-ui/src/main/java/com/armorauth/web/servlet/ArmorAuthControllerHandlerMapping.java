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
package com.armorauth.web.servlet;

import com.armorauth.util.PathUtils;
import com.armorauth.web.ArmorAuthController;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

import java.lang.reflect.Method;
import java.util.Set;

public class ArmorAuthControllerHandlerMapping extends RequestMappingHandlerMapping {

    private final String armorauthContextPath;

    public ArmorAuthControllerHandlerMapping() {
        this("");
    }

    public ArmorAuthControllerHandlerMapping(String armorauthContextPath) {
        this.armorauthContextPath = armorauthContextPath;
    }

    @Override
    protected boolean isHandler(Class<?> beanType) {
        return AnnotatedElementUtils.hasAnnotation(beanType, ArmorAuthController.class);
    }

    @Override
    protected void registerHandlerMethod(Object handler, Method method, RequestMappingInfo mapping) {
        super.registerHandlerMethod(handler, method, withPrefix(mapping));
    }

    private RequestMappingInfo withPrefix(RequestMappingInfo mapping) {
        if (!StringUtils.hasText(this.armorauthContextPath)) {
            return mapping;
        }
        RequestMappingInfo.Builder mutate = mapping.mutate();
        return mutate.paths(withNewPatterns(mapping.getPathPatternsCondition().getPatterns())).build();
    }

    private String[] withNewPatterns(Set<PathPattern> patterns) {
        return patterns.stream()
                .map((pattern) -> PathUtils.normalizePath(this.armorauthContextPath + pattern.getPatternString()))
                .toArray(String[]::new);
    }

}
