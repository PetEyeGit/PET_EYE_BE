package com.sang.sourcepattern.service;

import com.sang.sourcepattern.dto.request.ForgotPasswordRequest;
import com.sang.sourcepattern.dto.request.PasswordChangeRequest;
import com.sang.sourcepattern.dto.request.ResetPasswordRequest;
import com.sang.sourcepattern.dto.request.UserCreationRequest;
import com.sang.sourcepattern.dto.request.UserUpdateRequest;
import com.sang.sourcepattern.dto.response.PageResponse;
import com.sang.sourcepattern.dto.response.UserResponse;

import java.util.List;

public interface UserService {

    UserResponse createUser(UserCreationRequest request);

    UserResponse updateUser(Integer userId, UserUpdateRequest request);

    UserResponse getUserById(Integer userId);

    List<UserResponse> getAllUsers();

    PageResponse<UserResponse> getAllUsersPaged(int page);

    void deleteUser(Integer userId);

    void deactivateUser(Integer userId);

    void activateUser(Integer userId);

    void deleteAvatar(Integer userId);

    void changePassword(Integer userId, PasswordChangeRequest request);

    void verifyEmail(String email, String otp);

    void resendVerificationEmail(String email);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    void deleteExpiredUnverifiedUsers();
}