package com.armorauth;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class HomepageForwardingMatcher<T> implements Predicate<T> {

    private final List<Pattern> includeRoutes;

    private final Function<T, String> methodAccessor;

    private final Function<T, String> pathAccessor;

    private final Function<T, List<MediaType>> acceptsAccessor;

    public HomepageForwardingMatcher(List<String> includeRoutes,
                                     Function<T, String> methodAccessor, Function<T, String> pathAccessor,
                                     Function<T, List<MediaType>> acceptsAccessor) {
        this.includeRoutes = toPatterns(includeRoutes);
        this.methodAccessor = methodAccessor;
        this.pathAccessor = pathAccessor;
        this.acceptsAccessor = acceptsAccessor;
    }

    public boolean test(T request) {
        if (!HttpMethod.GET.matches(this.methodAccessor.apply(request))) {
            return false;
        }

        String path = this.pathAccessor.apply(request);
        boolean isIncludedRoute = this.includeRoutes.stream().anyMatch((p) -> p.matcher(path).matches());
        if (!isIncludedRoute) {
            return false;
        }

        return this.acceptsAccessor.apply(request).stream().anyMatch((t) -> t.includes(MediaType.TEXT_HTML));
    }

    private List<Pattern> toPatterns(List<String> routes) {
        return routes.stream()
                .map((r) -> "^" + r.replaceAll("/[*][*]", "(/.*)?").replaceAll("/[*]/", "/[^/]+/") + "$")
                .map(Pattern::compile)
                .collect(Collectors.toList());
    }

}