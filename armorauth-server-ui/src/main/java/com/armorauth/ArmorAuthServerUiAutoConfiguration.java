package com.armorauth;

import com.armorauth.web.servlet.ArmorAuthControllerHandlerMapping;
import com.armorauth.web.servlet.HomepageForwardingFilter;
import com.armorauth.util.CssColorUtils;
import com.armorauth.util.PathUtils;
import com.armorauth.web.UiController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
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

@Configuration(proxyBeanMethods = false)
public class ArmorAuthServerUiAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ArmorAuthServerUiAutoConfiguration.class);

    /**
     * path patterns that will be forwarded to the homepage (webapp)
     */
    private static final List<String> DEFAULT_UI_ROUTES = asList("/about/**", "/user/**");


    private final ApplicationContext applicationContext;

    public ArmorAuthServerUiAutoConfiguration(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Bean
    public CssColorUtils cssColorUtils() {
        return new CssColorUtils();
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
        resolver.setPrefix("classpath:/META-INF/armorauth-server-ui/");
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

            @Override
            @SuppressWarnings("deprecation")
            public void configurePathMatch(PathMatchConfigurer configurer) {
                configurer.setUseTrailingSlashMatch(true);
            }

            @Bean
            public HomepageForwardingFilterConfig homepageForwardingFilterConfig() throws IOException {
                String homepage = normalizeHomepageUrl("/");
                return new HomepageForwardingFilterConfig(homepage, DEFAULT_UI_ROUTES);
            }

            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                registry.addResourceHandler("/**")
                        .addResourceLocations("classpath:/META-INF/armorauth-server-ui/")
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