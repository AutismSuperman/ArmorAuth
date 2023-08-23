package com.armorauth.federat.wechat;


/**
 * Standard and custom (non-standard) parameter names defined in the Wechat OAuth Parameters
 * Registry and used by the authorization endpoint, token endpoint and token revocation
 * endpoint.
 *
 * @author AutismSuperman
 * @see <a target="_blank" href="https://developers.weixin.qq.com/doc/oplatform/Website_App/WeChat_Login/Wechat_Login.html">Wechat
 * OAuth Document</a>
 */
public final class WechatParameterNames {

    /**
     * {@code appid} - used in Authorization Request and Access Token Request.
     */
    public static final String APP_ID = "appid";
    /**
     * {@code secret} - used in Access Token Request.
     */
    public static final String SECRET = "secret";
    /**
     * {@code wechat_redirect} - wechat used in Authorization Request need the fragment
     */
    public static final String WECHAT_REDIRECT = "wechat_redirect";
}
