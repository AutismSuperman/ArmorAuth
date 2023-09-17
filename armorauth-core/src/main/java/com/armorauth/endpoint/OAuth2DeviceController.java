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

import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class OAuth2DeviceController {

    private final AuthorizationServerSettings authorizationServerSettings;

    public OAuth2DeviceController(AuthorizationServerSettings authorizationServerSettings) {
        this.authorizationServerSettings = authorizationServerSettings;
    }

    @GetMapping("/activate")
    public String activate(Model model, @RequestParam(value = "user_code", required = false) String userCode) {
        if (userCode != null) {
            return "redirect:/oauth2/device_verification?user_code=" + userCode;
        }
        model.addAttribute("model", "device");
        model.addAttribute("deviceVerificationEndpoint",
                authorizationServerSettings.getDeviceVerificationEndpoint());
        return "index";
    }

    @GetMapping("/activated")
    public String activated() {
        return "index";
    }


}