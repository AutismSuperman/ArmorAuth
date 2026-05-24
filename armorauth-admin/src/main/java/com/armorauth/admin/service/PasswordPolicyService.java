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
package com.armorauth.admin.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 密码策略服务
 *
 * @author fulin
 * @since 2026-05-23
 */
@Service
public class PasswordPolicyService {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 128;
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]");

    /**
     * 验证密码是否符合策略
     *
     * @param password 密码
     * @return 错误列表，空表示通过
     */
    public List<String> validate(String password) {
        List<String> errors = new ArrayList<>();

        if (password == null || password.isEmpty()) {
            errors.add("密码不能为空");
            return errors;
        }

        if (password.length() < MIN_LENGTH) {
            errors.add("密码长度不能少于" + MIN_LENGTH + "个字符");
        }

        if (password.length() > MAX_LENGTH) {
            errors.add("密码长度不能超过" + MAX_LENGTH + "个字符");
        }

        if (!UPPERCASE_PATTERN.matcher(password).find()) {
            errors.add("密码必须包含至少一个大写字母");
        }

        if (!LOWERCASE_PATTERN.matcher(password).find()) {
            errors.add("密码必须包含至少一个小写字母");
        }

        if (!DIGIT_PATTERN.matcher(password).find()) {
            errors.add("密码必须包含至少一个数字");
        }

        if (!SPECIAL_PATTERN.matcher(password).find()) {
            errors.add("密码必须包含至少一个特殊字符");
        }

        // 检查是否包含连续重复字符
        if (hasConsecutiveRepeatingChars(password, 3)) {
            errors.add("密码不能包含3个或以上连续重复字符");
        }

        return errors;
    }

    /**
     * 检查密码是否有效
     *
     * @param password 密码
     * @return true 如果密码符合策略
     */
    public boolean isValid(String password) {
        return validate(password).isEmpty();
    }

    /**
     * 检查是否包含连续重复字符
     */
    private boolean hasConsecutiveRepeatingChars(String password, int maxConsecutive) {
        if (password.length() < maxConsecutive) {
            return false;
        }
        for (int i = 0; i <= password.length() - maxConsecutive; i++) {
            boolean allSame = true;
            char first = password.charAt(i);
            for (int j = 1; j < maxConsecutive; j++) {
                if (password.charAt(i + j) != first) {
                    allSame = false;
                    break;
                }
            }
            if (allSame) {
                return true;
            }
        }
        return false;
    }
}
