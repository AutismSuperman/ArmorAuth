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
package com.armorauth;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;

import java.util.Base64;

public class StateTest {


    @Test
    public void test() {
        Base64StringKeyGenerator base64StringKeyGenerator = new Base64StringKeyGenerator(Base64.getUrlEncoder());
        String str = base64StringKeyGenerator.generateKey();
        System.out.println(str);
        Base64.Decoder urlDecoder = Base64.getUrlDecoder();
        byte[] decode = urlDecoder.decode(str);
        System.out.println(new String(decode));
    }
}
