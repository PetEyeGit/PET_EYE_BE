package com.sang.sourcepattern.service.impl;

import com.sang.sourcepattern.dto.request.ForgotPasswordRequest;
import com.sang.sourcepattern.dto.request.PasswordChangeRequest;
import com.sang.sourcepattern.dto.request.ResetPasswordRequest;
import com.sang.sourcepattern.dto.request.UserCreationRequest;
import com.sang.sourcepattern.dto.request.UserUpdateRequest;
import com.sang.sourcepattern.dto.response.UserResponse;
import com.sang.sourcepattern.entity.UserToken;
import com.sang.sourcepattern.entity.Role;
import com.sang.sourcepattern.entity.User;
import com.sang.sourcepattern.exception.AppException;
import com.sang.sourcepattern.exception.ErrorCode;
import com.sang.sourcepattern.mapper.UserMapper;
import com.sang.sourcepattern.repository.UserTokenRepository;
import com.sang.sourcepattern.repository.RoleRepository;
import com.sang.sourcepattern.repository.UserRepository;
import com.sang.sourcepattern.service.EmailService;
import com.sang.sourcepattern.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserServiceImpl implements UserService {

    UserRepository userRepository;
    RoleRepository roleRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;
    EmailService emailService;
    UserTokenRepository userTokenRepository;
    com.sang.sourcepattern.repository.UserVoucherRepository userVoucherRepository;
    com.sang.sourcepattern.service.TierUpgradeService tierUpgradeService;

    @Override
    @Transactional
    public UserResponse createUser(UserCreationRequest request) {
        log.info("createUser: {}", request.getEmail());
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setActive(true);         // active ngay, nhưng chưa dùng được đầy đủ cho đến khi verify email
        user.setEmailVerified(false);

        HashSet<Role> roles = new HashSet<>();
        roles.add(roleRepository.findByName("USER").orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND)));
        user.setRoles(roles);

        User savedUser = userRepository.save(user);

        sendVerificationToken(savedUser);

        return userMapper.toUserResponse(savedUser);
    }

    @Override
    public UserResponse updateUser(Integer userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        userMapper.updateUser(user, request);

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        return userMapper.toUserResponse(userRepository.save(user));
    }

    @Override
    public UserResponse getUserById(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        return mapUserWithVouchers(user);
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapUserWithVouchers)
                .toList();
    }

    @Override
    public com.sang.sourcepattern.dto.response.PageResponse<UserResponse> getAllUsersPaged(int page) {
        org.springframework.data.domain.Page<User> pageResult =
                userRepository.findAll(org.springframework.data.domain.PageRequest.of(page, 10,
                        org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")));
        return com.sang.sourcepattern.dto.response.PageResponse.<UserResponse>builder()
                .content(pageResult.getContent().stream().map(this::mapUserWithVouchers).toList())
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }

    private UserResponse mapUserWithVouchers(User user) {
        UserResponse response = userMapper.toUserResponse(user);
        List<com.sang.sourcepattern.dto.response.VoucherResponse> vouchers = userVoucherRepository.findByUserId(user.getId()).stream()
                .map(uv -> com.sang.sourcepattern.dto.response.VoucherResponse.builder()
                        .id((long) uv.getVoucher().getId())
                        .code(uv.getVoucher().getCode())
                        .targetTierName(uv.getVoucher().getTargetTier() != null ? uv.getVoucher().getTargetTier().getName() : null)
                        .requiredSpending(uv.getVoucher().getTargetTier() != null ? uv.getVoucher().getTargetTier().getRequiredSpending() : null)
                        .discountType(uv.getVoucher().getDiscountType())
                        .discountValue(uv.getVoucher().getDiscountValue())
                        .validDays(uv.getVoucher().getValidDays())
                        .isUsed(uv.isUsed())
                        .build())
                .toList();
        response.setVouchers(vouchers);
        return response;
    }

    @Override
    public void deleteUser(Integer userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        userRepository.deleteById(userId);
    }

    @Override
    public void deactivateUser(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        user.setActive(false);
        userRepository.save(user);
    }

    @Override
    public void activateUser(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        user.setActive(true);
        userRepository.save(user);
    }

    @Override
    public void deleteAvatar(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        user.setAvatar(null);
        userRepository.save(user);
    }

    @Override
    public void changePassword(Integer userId, PasswordChangeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.OLD_PASSWORD_INVALID);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void verifyEmail(String email, String otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (user.isEmailVerified()) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_VERIFIED);
        }

        UserToken verificationToken = userTokenRepository.findByTokenAndType(otp, "VERIFY_EMAIL")
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_VERIFICATION_TOKEN));

        if (verificationToken.getUser().getId() != user.getId()) {
            throw new AppException(ErrorCode.INVALID_VERIFICATION_TOKEN);
        }

        if (verificationToken.isUsed() || verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.INVALID_VERIFICATION_TOKEN);
        }

        user.setEmailVerified(true);
        user.setActive(true);         // kích hoạt tài khoản sau khi xác thực email
        userRepository.save(user);

        verificationToken.setUsed(true);
        userTokenRepository.save(verificationToken);

        log.info("Email verified for user: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (user.isEmailVerified()) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_VERIFIED);
        }

        userTokenRepository.deleteByUserIdAndType(user.getId(), "VERIFY_EMAIL");
        sendVerificationToken(user);
        log.info("Resent verification email to: {}", email);
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        userTokenRepository.deleteByUserIdAndType(user.getId(), "RESET_PASSWORD");

        String otp = String.format("%06d", new java.util.Random().nextInt(1_000_000));
        userTokenRepository.saveAndFlush(UserToken.builder()
                .token(otp)
                .type("RESET_PASSWORD")
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build());

        emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), otp);
        log.info("Password reset OTP sent to: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        UserToken resetToken = userTokenRepository.findByTokenAndType(request.getOtp(), "RESET_PASSWORD")
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_RESET_TOKEN));

        if (resetToken.getUser().getId() != user.getId()) {
            throw new AppException(ErrorCode.INVALID_RESET_TOKEN);
        }

        if (resetToken.isUsed()) {
            throw new AppException(ErrorCode.RESET_TOKEN_USED);
        }

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.INVALID_RESET_TOKEN);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        userTokenRepository.save(resetToken);

        log.info("Password reset successfully for user: {}", user.getEmail());
    }

    
    private void sendVerificationToken(User user) {
        String otp = String.format("%06d", new java.util.Random().nextInt(1_000_000));
        userTokenRepository.saveAndFlush(UserToken.builder()
                .token(otp)
                .type("VERIFY_EMAIL")
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build());
       
        emailService.sendVerificationEmail(user.getEmail(), user.getFullName(), otp);
    }
}
