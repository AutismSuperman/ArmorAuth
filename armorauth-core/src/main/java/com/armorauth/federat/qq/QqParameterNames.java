package com.armorauth.federat.qq;


/**
 * Standard and custom (non-standard) parameter names defined in the QQ OAuth Parameters
 * Registry and used by the authorization endpoint, token endpoint and token revocation
 * endpoint.
 *
 * @author AutismSuperman
 * @see <a target="_blank" href="https://wiki.connect.qq.com/%e7%bd%91%e7%ab%99%e5%ba%94%e7%94%a8%e6%8e%a5%e5%85%a5%e6%b5%81%e7%a8%8b">QQ
 * OAuth Document</a>
 */
public final class QqParameterNames {

    /**
     * QQ get openid url
     */
    public static final String URL_GET_OPENID = "https://graph.qq.com/oauth2.0/me";
    /**
     * QQ get openid need
     */
    public static final String FMT = "fmt";

    /**
     * QQ get openid need
     */
    public static final String FMT_JOSN = "json";

    /**
     * QQ get userinfo need
     */
    public static final String NEED_OPENID = "need_openid";

    /**
     * QQ get userinfo need
     */
    public static final String OPENID = "openid";

    /**
     * QQ get userinfo need
     */
    public static final String OAUTH_CONSUMER_KEY = "oauth_consumer_key";

}
