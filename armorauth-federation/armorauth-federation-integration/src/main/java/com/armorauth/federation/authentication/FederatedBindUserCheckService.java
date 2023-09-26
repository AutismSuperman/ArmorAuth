package com.armorauth.federation.authentication;

public interface FederatedBindUserCheckService {


    Boolean requireBindUser(String openId,String clientRegistrationId);

}
