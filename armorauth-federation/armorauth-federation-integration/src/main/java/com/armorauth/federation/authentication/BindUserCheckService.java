package com.armorauth.federation.authentication;

public interface BindUserCheckService {


    Boolean requireBindUser(String openId,String clientRegistrationId);

}
