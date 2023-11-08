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
package com.armorauth.federation.web;

import jakarta.servlet.Filter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

public final class HttpSecurityFilterOrderRegistrationUtils {

    public static void putIntendedFilterAfter(HttpSecurity http, Filter filter, Class<? extends Filter> afterFilter) {
        Class<?> filterOrderRegistrationClass;
        try {
            filterOrderRegistrationClass = Class.forName("org.springframework.security.config.annotation.web.builders.FilterOrderRegistration");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        Method[] declaredMethods = ReflectionUtils.getDeclaredMethods(filterOrderRegistrationClass);
        Arrays.stream(declaredMethods).filter(method -> method.getName().equals("put")).findFirst().ifPresent(put -> {
            extracted(http, filter, afterFilter, put, filterOrderRegistrationClass, 1);
        });
    }

    public static void putIntendedFilterBefore(HttpSecurity http, Filter filter, Class<? extends Filter> beforeFilter) {

        Class<?> filterOrderRegistrationClass;
        try {
            filterOrderRegistrationClass = Class.forName("org.springframework.security.config.annotation.web.builders.FilterOrderRegistration");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        Method[] declaredMethods = ReflectionUtils.getDeclaredMethods(filterOrderRegistrationClass);
        Arrays.stream(declaredMethods).filter(method -> method.getName().equals("put")).findFirst().ifPresent(put -> {
            extracted(http, filter, beforeFilter, put, filterOrderRegistrationClass, -1);
        });
    }

    private static void extracted(HttpSecurity http, Filter filter, Class<? extends Filter> beforeFilter, Method put, Class<?> filterOrderRegistrationClass, int offset) {
        ReflectionUtils.makeAccessible(put);
        Method getOrder = ReflectionUtils.findMethod(filterOrderRegistrationClass, "getOrder", Class.class);
        assert getOrder != null;
        ReflectionUtils.makeAccessible(getOrder);
        Field filterOrders = ReflectionUtils.findField(http.getClass(), "filterOrders");
        assert filterOrders != null;
        ReflectionUtils.makeAccessible(filterOrders);
        Object filterOrderRegistration = ReflectionUtils.getField(filterOrders, http);
        Integer order = (Integer) ReflectionUtils.invokeMethod(getOrder, filterOrderRegistration, beforeFilter);
        assert order != null;
        ReflectionUtils.invokeMethod(put, filterOrderRegistration, filter.getClass(), Integer.sum(order, offset));
    }
}