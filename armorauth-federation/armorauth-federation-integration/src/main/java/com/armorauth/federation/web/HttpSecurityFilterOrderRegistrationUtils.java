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