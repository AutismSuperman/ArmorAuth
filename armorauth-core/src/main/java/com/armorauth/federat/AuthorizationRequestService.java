package com.armorauth.federat;


import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;


public interface AuthorizationRequestService {


    void convert(OAuth2AuthorizationRequest.Builder builder);


    boolean supports(String registrationId);


}
