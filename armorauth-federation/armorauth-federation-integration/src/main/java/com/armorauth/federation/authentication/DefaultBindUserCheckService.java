package com.armorauth.federation.authentication;

public class DefaultBindUserCheckService implements BindUserCheckService{
    @Override
    public Boolean requireBindUser(String openId, String clientRegistrationId) {
        // default return false
        return false;
    }
}
