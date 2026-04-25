package com.sang.sourcepattern.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.sang.sourcepattern.dto.request.UserUpdateRequest;
import com.sang.sourcepattern.dto.request.UserCreationRequest;
import com.sang.sourcepattern.dto.request.PasswordChangeRequest;
import com.sang.sourcepattern.dto.response.ApiResponse;
import com.sang.sourcepattern.dto.response.UserResponse;
import com.sang.sourcepattern.service.UserService;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserController {
    UserService userService;

    @PostMapping("/register")
    ApiResponse<UserResponse> createUser(@RequestBody @Valid UserCreationRequest request) {
        log.info("Create User Controller! ");
        ApiResponse<UserResponse> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userService.createUser(request));
        return apiResponse;
    }

    @GetMapping("/{id}")
    ApiResponse<UserResponse> getUser(@PathVariable Integer id) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getUserById(id))
                .build();
    }

    @PutMapping("/{id}")
    ApiResponse<UserResponse> updateUser(@PathVariable Integer id, @RequestBody @Valid UserUpdateRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.updateUser(id, request))
                .build();
    }

    @DeleteMapping("/{id}/avatar")
    ApiResponse<Void> deleteAvatar(@PathVariable Integer id) {
        userService.deleteAvatar(id);
        return ApiResponse.<Void>builder()
                .message("Avatar deleted successfully")
                .build();
    }

    @PostMapping("/{id}/change-password")
    ApiResponse<Void> changePassword(@PathVariable Integer id, @RequestBody @Valid PasswordChangeRequest request) {
        userService.changePassword(id, request);
        return ApiResponse.<Void>builder()
                .message("Password changed successfully")
                .build();
    }
}