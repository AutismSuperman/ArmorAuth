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
package com.armorauth.authentication;

import com.armorauth.captcha.GraphicCaptchaService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CaptchaAuthenticationProviderTest {

    private final UserDetailsService userDetailsService = username -> User.withUsername(username)
            .password("{noop}ignored")
            .authorities("ROLE_USER")
            .build();

    @Test
    void authenticatesGraphicCaptchaWithCaptchaId() {
        CaptchaIdGraphicCaptchaService captchaService = new CaptchaIdGraphicCaptchaService();
        CaptchaAuthenticationProvider provider =
                new CaptchaAuthenticationProvider(userDetailsService, captchaService);

        Authentication result = provider.authenticate(
                new CaptchaAuthenticationToken("admin", "ABCD", "captcha-1"));

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getName()).isEqualTo("admin");
        assertThat(captchaService.verifyCalled).isTrue();
        assertThat(captchaService.legacyVerifyCalled).isFalse();
    }

    @Test
    void fallsBackToLegacyCaptchaVerificationWithoutCaptchaId() {
        AtomicReference<String> verifiedAccount = new AtomicReference<>();
        AtomicReference<String> verifiedCode = new AtomicReference<>();
        CaptchaVerifyService captchaService = (account, captcha) -> {
            verifiedAccount.set(account);
            verifiedCode.set(captcha);
            return "admin".equals(account) && "1234".equals(captcha);
        };
        CaptchaAuthenticationProvider provider =
                new CaptchaAuthenticationProvider(userDetailsService, captchaService);

        Authentication result = provider.authenticate(new CaptchaAuthenticationToken("admin", "1234"));

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(verifiedAccount.get()).isEqualTo("admin");
        assertThat(verifiedCode.get()).isEqualTo("1234");
    }

    @Test
    void separatesGraphicCaptchaFromSmsOtpVerification() {
        AtomicReference<String> verifiedPhone = new AtomicReference<>();
        CaptchaVerifyService smsOtpService = (account, captcha) -> {
            verifiedPhone.set(account);
            return "13103777777".equals(account) && "654321".equals(captcha);
        };
        CaptchaIdGraphicCaptchaService graphicCaptchaService = new CaptchaIdGraphicCaptchaService();
        CaptchaAuthenticationProvider provider =
                new CaptchaAuthenticationProvider(userDetailsService, smsOtpService, graphicCaptchaService);

        Authentication graphicResult = provider.authenticate(
                new CaptchaAuthenticationToken("admin", "ABCD", "captcha-1"));
        Authentication smsResult = provider.authenticate(
                new CaptchaAuthenticationToken("13103777777", "654321"));

        assertThat(graphicResult.isAuthenticated()).isTrue();
        assertThat(smsResult.isAuthenticated()).isTrue();
        assertThat(graphicCaptchaService.verifyCalled).isTrue();
        assertThat(graphicCaptchaService.legacyVerifyCalled).isFalse();
        assertThat(verifiedPhone.get()).isEqualTo("13103777777");
    }

    private static final class CaptchaIdGraphicCaptchaService extends GraphicCaptchaService {

        private boolean verifyCalled;

        private boolean legacyVerifyCalled;

        @Override
        public boolean verify(String captchaId, String code) {
            verifyCalled = true;
            return "captcha-1".equals(captchaId) && "ABCD".equals(code);
        }

        @Override
        public boolean verifyCaptcha(String account, String captcha) {
            legacyVerifyCalled = true;
            return false;
        }
    }
}
