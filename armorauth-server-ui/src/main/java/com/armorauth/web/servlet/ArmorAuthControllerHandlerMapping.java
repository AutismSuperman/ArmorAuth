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
