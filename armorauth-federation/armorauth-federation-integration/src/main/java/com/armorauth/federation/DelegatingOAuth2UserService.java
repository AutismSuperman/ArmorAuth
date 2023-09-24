package com.armorauth.federation;

import com.armorauth.federation.gitee.user.GiteeOAuth2UserService;
import com.armorauth.federation.qq.user.QqOAuth2UserService;
import com.armorauth.federation.wechat.user.WechatOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.HashMap;
import java.util.Map;

public class DelegatingOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> defaultOAuth2UserService = new DefaultOAuth2UserService();

    private final Map<String, OAuth2UserService<OAuth2UserRequest, OAuth2User>> userServices;

    public DelegatingOAuth2UserService(Map<String, OAuth2UserService<OAuth2UserRequest, OAuth2User>> userServices) {
        this.userServices = userServices;
    }


    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService = userServices.get(registrationId.toLowerCase());
        if (oAuth2UserService == null) {
            oAuth2UserService = defaultOAuth2UserService;
        }
        return oAuth2UserService.loadUser(userRequest);
    }

    public void addOAuth2UserService(String registrationId, OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService) {
        this.userServices.put(registrationId.toUpperCase(), oAuth2UserService);
    }

}