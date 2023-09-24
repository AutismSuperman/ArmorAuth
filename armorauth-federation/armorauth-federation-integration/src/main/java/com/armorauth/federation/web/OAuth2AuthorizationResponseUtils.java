package com.armorauth.federation.web;

import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.Map;

final class OAuth2AuthorizationResponseUtils {

    private OAuth2AuthorizationResponseUtils() {
    }

    static MultiValueMap<String, String> toMultiMap(Map<String, String[]> map) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>(map.size());
        map.forEach((key, values) -> {
            for (String value : values) {
                params.add(key, value);
            }
        });
        return params;
    }

    static boolean isAuthorizationResponse(MultiValueMap<String, String> request) {
        return isAuthorizationResponseSuccess(request) || isAuthorizationResponseError(request);
    }

    static boolean isAuthorizationResponseSuccess(MultiValueMap<String, String> request) {
        return StringUtils.hasText(request.getFirst(OAuth2ParameterNames.CODE))
                && StringUtils.hasText(request.getFirst(OAuth2ParameterNames.STATE));
    }

    static boolean isAuthorizationResponseError(MultiValueMap<String, String> request) {
        return StringUtils.hasText(request.getFirst(OAuth2ParameterNames.ERROR))
                && StringUtils.hasText(request.getFirst(OAuth2ParameterNames.STATE));
    }

    static OAuth2AuthorizationResponse convert(MultiValueMap<String, String> request, String redirectUri) {
        String code = request.getFirst(OAuth2ParameterNames.CODE);
        String errorCode = request.getFirst(OAuth2ParameterNames.ERROR);
        String state = request.getFirst(OAuth2ParameterNames.STATE);
        if (StringUtils.hasText(code)) {
            return OAuth2AuthorizationResponse.success(code).redirectUri(redirectUri).state(state).build();
        }
        String errorDescription = request.getFirst(OAuth2ParameterNames.ERROR_DESCRIPTION);
        String errorUri = request.getFirst(OAuth2ParameterNames.ERROR_URI);
        // @formatter:off
		return OAuth2AuthorizationResponse.error(errorCode)
				.redirectUri(redirectUri)
				.errorDescription(errorDescription)
				.errorUri(errorUri)
				.state(state)
				.build();
		// @formatter:on
    }

}