package com.armorauth.web.servlet;

import com.armorauth.HomepageForwardingFilterConfig;
import com.armorauth.HomepageForwardingMatcher;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.util.UrlPathHelper;

import java.io.IOException;
import java.util.List;

/**
 * A servlet filter that forwards matching HTML requests to the home page. Further routing
 * will is done in the browser by VueRouter using history mode.
 *
 * @see <a href="https://router.vuejs.org/guide/essentials/history-mode.html">VueRouter
 * history mode</a>
 */
public class HomepageForwardingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(HomepageForwardingFilter.class);

    private final String homepage;

    private final HomepageForwardingMatcher<HttpServletRequest> matcher;

    public HomepageForwardingFilter(String homepage, List<String> routes) {
        this.homepage = homepage;
        UrlPathHelper urlPathHelper = new UrlPathHelper();
        this.matcher = new HomepageForwardingMatcher<>(routes, HttpServletRequest::getMethod,
                urlPathHelper::getPathWithinApplication,
                (r) -> MediaType.parseMediaTypes(r.getHeader(HttpHeaders.ACCEPT)));
    }

    public HomepageForwardingFilter(HomepageForwardingFilterConfig filterConfig) {
        this(filterConfig.getHomepage(), filterConfig.getRoutesIncludes());
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpRequest) {
            if (this.matcher.test(httpRequest)) {
                log.trace("Forwarding request with URL {} to index", httpRequest.getRequestURI());
                request.getRequestDispatcher(this.homepage).forward(request, response);
                return;
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void destroy() {
    }

}