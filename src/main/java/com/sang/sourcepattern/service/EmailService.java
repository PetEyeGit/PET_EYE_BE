package com.sang.sourcepattern.service;

public interface EmailService {
    void sendVerificationEmail(String toEmail, String fullName, String token);
    void sendPasswordResetEmail(String toEmail, String fullName, String token);
    void sendShopApprovedEmail(String toEmail, String shopName);
    void sendShopRejectedEmail(String toEmail, String shopName, String reason);
}
