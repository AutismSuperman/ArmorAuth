/*
 * Copyright (c) 2023-present ArmorAuth. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
