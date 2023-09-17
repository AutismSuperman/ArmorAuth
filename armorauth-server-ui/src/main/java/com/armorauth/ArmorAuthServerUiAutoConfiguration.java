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
package com.armorauth;

import com.armorauth.properties.ArmorAuthUiProperties;
import com.armorauth.web.servlet.ArmorAuthControllerHandlerMapping;
import com.armorauth.web.servlet.HomepageForwardingFilter;
import com.armorauth.util.PathUtils;
import com.armorauth.web.UiController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static java.util.Arrays.asList;

@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({
        ArmorAuthUiProperties.class
})
public class ArmorAuthServerUiAutoConfiguration {


    /**
     * path patterns that will be forwarded to the homepage (webapp)
     */
    private static final List<String> DEFAULT_UI_ROUTES =
            asList("/activate/**","/activated/**");


    private final ApplicationContext applicationContext;


    private final ArmorAuthUiProperties armorAuthUiProperties;

    public ArmorAuthServerUiAutoConfiguration(ApplicationContext applicationContext, ArmorAuthUiProperties armorAuthUiProperties) {
        this.applicationContext = applicationContext;
        this.armorAuthUiProperties = armorAuthUiProperties;
    }

    @Bean
    @ConditionalOnMissingBean
    public UiController homeUiController() {
        return new UiController();
    }


    @Bean
    public SpringResourceTemplateResolver armorauthTemplateResolver() {
        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setApplicationContext(this.applicationContext);
        resolver.setPrefix(armorAuthUiProperties.getTemplateLocation());
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setCacheable(true);
        resolver.setOrder(10);
        resolver.setCheckExistence(true);
        return resolver;
    }

    static String normalizeHomepageUrl(String homepage) {
        if (!"/".equals(homepage)) {
            homepage = PathUtils.normalizePath(homepage);
        }
        return homepage;
    }


    @Bean
    public RequestMappingHandlerMapping adminHandlerMapping(ContentNegotiationManager contentNegotiationManager) {
        RequestMappingHandlerMapping mapping = new ArmorAuthControllerHandlerMapping();
        mapping.setOrder(0);
        mapping.setContentNegotiationManager(contentNegotiationManager);
        return mapping;
    }


    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public static class ServletUiConfiguration {


        @Configuration(proxyBeanMethods = false)
        public static class AdminUiWebMvcConfig implements WebMvcConfigurer {

            private final ArmorAuthUiProperties armorAuthUiProperties;

            public AdminUiWebMvcConfig(ArmorAuthUiProperties armorAuthUiProperties) {
                this.armorAuthUiProperties = armorAuthUiProperties;
            }

            @Override
            @SuppressWarnings("deprecation")
            public void configurePathMatch(PathMatchConfigurer configurer) {
                configurer.setUseTrailingSlashMatch(true);
            }

            @Bean
            public HomepageForwardingFilterConfig homepageForwardingFilterConfig() {
                String homepage = normalizeHomepageUrl("/");
                return new HomepageForwardingFilterConfig(homepage, DEFAULT_UI_ROUTES);
            }

            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                registry.addResourceHandler("/**")
                        .addResourceLocations(this.armorAuthUiProperties.getResourceLocations())
                        .setCacheControl(CacheControl.noStore());
            }

            @Bean
            @ConditionalOnMissingBean
            public HomepageForwardingFilter homepageForwardFilter(HomepageForwardingFilterConfig homepageForwardingFilterConfig) throws IOException {
                return new HomepageForwardingFilter(homepageForwardingFilterConfig);
            }

        }

    }

}