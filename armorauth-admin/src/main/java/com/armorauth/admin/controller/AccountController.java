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
package com.armorauth.admin.controller;

import com.armorauth.admin.dto.AccountDTO;
import com.armorauth.admin.service.AccountService;
import com.armorauth.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 账户自助API
 *
 * @author fulin
 * @since 2026-05-23
 */
@RestController
@RequestMapping("/api/account/v1")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public ApiResponse<AccountDTO.ProfileResponse> me(Authentication authentication) {
        return ApiResponse.ok(accountService.getProfile(authentication.getName()));
    }

    /**
     * 更新当前用户信息
     */
    @PutMapping("/me")
    public ApiResponse<AccountDTO.ProfileResponse> updateMe(
            Authentication authentication,
            @Valid @RequestBody AccountDTO.UpdateProfileRequest request) {
        return ApiResponse.ok(accountService.updateProfile(authentication.getName(), request));
    }

    /**
     * 修改密码
     */
    @PostMapping("/password:change")
    public ApiResponse<Void> changePassword(
            Authentication authentication,
            @Valid @RequestBody AccountDTO.ChangePasswordRequest request) {
        accountService.changePassword(authentication.getName(), request);
        return ApiResponse.ok();
    }

    /**
     * 获取当前用户的 MFA 因子列表
     */
    @GetMapping("/factors")
    public ApiResponse<List<AccountDTO.FactorResponse>> listFactors(Authentication authentication) {
        return ApiResponse.ok(accountService.listFactors(authentication.getName()));
    }

    /**
     * 初始化 TOTP 绑定（返回 secret 和 URI，用户需扫码后验证）
     */
    @PostMapping("/factors/totp")
    public ApiResponse<AccountDTO.TotpSetupResponse> setupTotp(Authentication authentication) {
        return ApiResponse.ok(accountService.setupTotp(authentication.getName()));
    }

    /**
     * 初始化 Passkey/WebAuthn 注册（返回 challenge 和公钥凭据创建参数）
     */
    @PostMapping("/factors/passkey:begin-registration")
    public ApiResponse<AccountDTO.PasskeyBeginRegistrationResponse> beginPasskeyRegistration(
            Authentication authentication,
            @RequestBody(required = false) AccountDTO.PasskeyBeginRegistrationRequest request) {
        return ApiResponse.ok(accountService.beginPasskeyRegistration(authentication.getName(), request));
    }

    /**
     * 保存 Passkey/WebAuthn 凭据元数据
     */
    @PostMapping("/factors/passkey/{id}:finish-registration")
    public ApiResponse<AccountDTO.FactorResponse> finishPasskeyRegistration(
            Authentication authentication,
            @PathVariable(name = "id") String factorId,
            @Valid @RequestBody AccountDTO.PasskeyFinishRegistrationRequest request) {
        return ApiResponse.ok(accountService.finishPasskeyRegistration(authentication.getName(), factorId, request));
    }

    /**
     * 验证并激活 TOTP 因子
     */
    @PostMapping("/factors/{id}:verify")
    public ApiResponse<Void> verifyFactor(
            Authentication authentication,
            @PathVariable(name = "id") String factorId,
            @Valid @RequestBody AccountDTO.VerifyFactorRequest request) {
        accountService.verifyFactor(authentication.getName(), factorId, request.code());
        return ApiResponse.ok();
    }

    /**
     * 删除 MFA 因子
     */
    @DeleteMapping("/factors/{id}")
    public ApiResponse<Void> deleteFactor(
            Authentication authentication,
            @PathVariable(name = "id") String factorId) {
        accountService.deleteFactor(authentication.getName(), factorId);
        return ApiResponse.ok();
    }
}
