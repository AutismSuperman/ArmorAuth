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
package com.armorauth.endpoint;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;

@Controller
public class OAuth2LoginController {
    /**
     * 跳转登录页面
     *
     * @param model              model
     * @param authentication     认证信息
     * @param enableCaptchaLogin 是否开启验证码
     * @param csrfToken          是否开启crsf
     * @return
     */
    @GetMapping("/login")
    public String oauth2LoginPage(Model model,
                                  @CurrentSecurityContext(expression = "authentication") Authentication authentication,
                                  @Value("${spring.security.oauth2.server.login.captcha.enabled:true}") boolean enableCaptchaLogin,
                                  @RequestAttribute(name = "org.springframework.security.web.csrf.CsrfToken", required = false) CsrfToken csrfToken
    ) {
        if (!(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/";
        }
        if (csrfToken != null) {
            model.addAttribute("_csrfToken", csrfToken);
        }
        model.addAttribute("enableCaptchaLogin", enableCaptchaLogin);
        return "oauth2_login";
    }


    /**
     * oauth2中间页.
     *
     * @param model          the model
     * @param authentication the authentication
     * @param csrfToken      the csrf token
     * @return the string
     */
    @GetMapping("/")
    public String oauth2IndexPage(Model model,
                                  @CurrentSecurityContext(expression = "authentication") Authentication authentication,
                                  @RequestAttribute(name = "org.springframework.security.web.csrf.CsrfToken", required = false) CsrfToken csrfToken) {

        if (csrfToken != null) {
            model.addAttribute("_csrfToken", csrfToken);
        }
        model.addAttribute("principal", authentication.getName());
        return "default/oauth2_index";
    }


}
