package com.sang.sourcepattern.controller;

import com.nimbusds.jose.JOSEException;
import com.sang.sourcepattern.dto.request.AuthenticationRequest;
import com.sang.sourcepattern.dto.request.ForgotPasswordRequest;
import com.sang.sourcepattern.dto.request.IntrospectRequest;
import com.sang.sourcepattern.dto.request.LogoutRequest;
import com.sang.sourcepattern.dto.request.RefreshRequest;
import com.sang.sourcepattern.dto.request.ResetPasswordRequest;
import com.sang.sourcepattern.dto.request.SocialLoginRequest;
import com.sang.sourcepattern.dto.response.ApiResponse;
import com.sang.sourcepattern.dto.response.AuthenticationResponse;
import com.sang.sourcepattern.dto.response.IntrospectResponse;
import com.sang.sourcepattern.service.UserService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;
import com.sang.sourcepattern.service.AuthenticationService;

import java.text.ParseException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {

    AuthenticationService authenticationService;
    UserService userService;

    @PostMapping("/login")
    public ApiResponse<AuthenticationResponse> login(@RequestBody @Valid AuthenticationRequest request) {
        return ApiResponse.<AuthenticationResponse>builder()
                .result(authenticationService.authenticated(request))
                .build();
    }

    @PostMapping("/introspect")
    public ApiResponse<IntrospectResponse> introspect(@RequestBody IntrospectRequest request)
            throws ParseException, JOSEException {
        return ApiResponse.<IntrospectResponse>builder()
                .result(authenticationService.introspect(request))
                .build();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody LogoutRequest request)
            throws ParseException, JOSEException {
        authenticationService.logout(request);
        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthenticationResponse> refresh(@RequestBody RefreshRequest request)
            throws ParseException, JOSEException {
        return ApiResponse.<AuthenticationResponse>builder()
                .result(authenticationService.refreshToken(request))
                .build();
    }

    /** Xác thực email bằng OTP */
    @PostMapping("/verify-email")
    public ApiResponse<Void> verifyEmail(@RequestParam String email, @RequestParam String otp) {
        userService.verifyEmail(email, otp);
        return ApiResponse.<Void>builder()
                .message("Email verified successfully")
                .build();
    }

    /** Gửi lại email xác thực */
    @PostMapping("/resend-verification")
    public ApiResponse<Void> resendVerification(@RequestParam String email) {
        userService.resendVerificationEmail(email);
        return ApiResponse.<Void>builder()
                .message("Verification email sent")
                .build();
    }

    /** Quên mật khẩu — gửi OTP về email */
    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        userService.forgotPassword(request);
        return ApiResponse.<Void>builder()
                .message("Password reset email sent")
                .build();
    }

    /** Đặt lại mật khẩu bằng token */
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        userService.resetPassword(request);
        return ApiResponse.<Void>builder()
                .message("Password reset successfully")
                .build();
    }

    @PostMapping("/google")
    public ApiResponse<AuthenticationResponse> googleLogin(@RequestBody SocialLoginRequest request) {
        return ApiResponse.<AuthenticationResponse>builder()
                .result(authenticationService.socialLoginGoogle(request))
                .build();
    }

    @PostMapping("/facebook")
    public ApiResponse<AuthenticationResponse> facebookLogin(@RequestBody SocialLoginRequest request) {
        return ApiResponse.<AuthenticationResponse>builder()
                .result(authenticationService.socialLoginFacebook(request))
                .build();
    }

    @PostMapping("/zalo")
    public ApiResponse<AuthenticationResponse> zaloLogin(@RequestBody SocialLoginRequest request) {
        return ApiResponse.<AuthenticationResponse>builder()
                .result(authenticationService.socialLoginZalo(request))
                .build();
    }
}
