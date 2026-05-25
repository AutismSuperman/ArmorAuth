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

import com.armorauth.admin.dto.UserDTO;
import com.armorauth.admin.service.UserService;
import com.armorauth.common.response.ApiResponse;
import com.armorauth.common.response.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理API
 *
 * @author fulin
 * @since 2026-05-23
 */
@RestController
@RequestMapping("/api/admin/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<PageResponse<UserDTO.Response>> list(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sort", defaultValue = "createTime") String sort,
            @RequestParam(name = "direction", defaultValue = "DESC") String direction,
            @RequestParam(name = "keyword", required = false) String keyword) {
        Sort sortObj = Sort.by(Sort.Direction.fromString(direction), sort);
        Page<UserDTO.Response> result = userService.listUsers(PageRequest.of(page, size, sortObj), keyword);
        PageResponse<UserDTO.Response> pageResponse = new PageResponse<>(
                result.getContent(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize()
        );
        return ApiResponse.ok(pageResponse);
    }

    @GetMapping("/{id}")
    public ApiResponse<UserDTO.Response> get(@PathVariable(name = "id") String id) {
        return ApiResponse.ok(userService.getUser(id));
    }

    @PostMapping
    public ApiResponse<UserDTO.Response> create(@Valid @RequestBody UserDTO.CreateRequest request) {
        return ApiResponse.ok(userService.createUser(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserDTO.Response> update(
            @PathVariable(name = "id") String id,
            @Valid @RequestBody UserDTO.UpdateRequest request) {
        return ApiResponse.ok(userService.updateUser(id, request));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(
            @PathVariable(name = "id") String id,
            @Valid @RequestBody UserDTO.StatusRequest request) {
        userService.updateUserStatus(id, request.status());
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/lock")
    public ApiResponse<Void> lock(
            @PathVariable(name = "id") String id,
            @Valid @RequestBody UserDTO.LockRequest request) {
        userService.lockUser(id, request.durationMinutes());
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/unlock")
    public ApiResponse<Void> unlock(@PathVariable(name = "id") String id) {
        userService.unlockUser(id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/password:reset")
    public ApiResponse<Void> resetPassword(
            @PathVariable(name = "id") String id,
            @Valid @RequestBody UserDTO.ResetPasswordRequest request) {
        userService.resetPassword(id, request.newPassword());
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable(name = "id") String id) {
        userService.deleteUser(id);
        return ApiResponse.ok();
    }
}
