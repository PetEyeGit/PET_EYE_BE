package com.sang.sourcepattern.service.impl;

import com.sang.sourcepattern.service.EmailService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import org.springframework.scheduling.annotation.Async;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class EmailServiceImpl implements EmailService {

    JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    @NonFinal
    String fromEmail;

    @Override
    public void sendVerificationEmail(String toEmail, String fullName, String otp) {
        String subject = "[PET EYE] Mã xác thực OTP của bạn";
        sendHtmlEmail(toEmail, subject, buildOtpEmailHtml(fullName, otp, "xác thực email", 10));
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String fullName, String otp) {
        String subject = "[PET EYE] Mã OTP đặt lại mật khẩu";
        sendHtmlEmail(toEmail, subject, buildOtpEmailHtml(fullName, otp, "đặt lại mật khẩu", 15));
    }

    @Override
    public void sendShopApprovedEmail(String toEmail, String shopName) {
        String subject = "[PET EYE] Cửa hàng của bạn đã được duyệt";
        String html = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 24px; border: 1px solid #e0e0e0; border-radius: 8px;">
                    <h2 style="color: #4CAF50;">🎉 Chúc mừng! Cửa hàng đã được duyệt</h2>
                    <p>Xin chào <strong>%s</strong>,</p>
                    <p>Cửa hàng <strong>%s</strong> của bạn đã được Admin <strong>phê duyệt</strong> thành công.</p>
                    <p>Bạn có thể đăng nhập và bắt đầu sử dụng hệ thống ngay bây giờ.</p>
                    <hr style="border: none; border-top: 1px solid #eee; margin: 24px 0;">
                    <p style="color: #aaa; font-size: 12px;">PET EYE — Nền tảng chăm sóc thú cưng</p>
                </div>
                """.formatted(shopName, shopName);
        sendHtmlEmail(toEmail, subject, html);
    }

    @Override
    public void sendShopRejectedEmail(String toEmail, String shopName, String reason) {
        String subject = "[PET EYE] Đơn đăng ký cửa hàng bị từ chối";
        String reasonHtml = (reason != null && !reason.isBlank())
                ? "<p><strong>Lý do:</strong> " + reason + "</p>"
                : "";
        String html = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 24px; border: 1px solid #e0e0e0; border-radius: 8px;">
                    <h2 style="color: #e53935;">❌ Đơn đăng ký cửa hàng bị từ chối</h2>
                    <p>Xin chào <strong>%s</strong>,</p>
                    <p>Rất tiếc, đơn đăng ký cửa hàng <strong>%s</strong> của bạn đã bị <strong>từ chối</strong>.</p>
                    %s
                    <p>Nếu bạn có thắc mắc, vui lòng liên hệ đội ngũ hỗ trợ của chúng tôi.</p>
                    <hr style="border: none; border-top: 1px solid #eee; margin: 24px 0;">
                    <p style="color: #aaa; font-size: 12px;">PET EYE — Nền tảng chăm sóc thú cưng</p>
                </div>
                """.formatted(shopName, shopName, reasonHtml);
        sendHtmlEmail(toEmail, subject, html);
    }

    @Async
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    private String buildOtpEmailHtml(String fullName, String otp, String purpose, int minutes) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 24px; border: 1px solid #e0e0e0; border-radius: 8px;">
                    <h2 style="color: #4CAF50;">🐾 PET EYE — Mã OTP %s</h2>
                    <p>Xin chào <strong>%s</strong>,</p>
                    <p>Đây là mã OTP để %s của bạn:</p>
                    <div style="text-align: center; margin: 32px 0;">
                        <span style="display: inline-block; background-color: #f5f5f5; border: 2px dashed #4CAF50; border-radius: 8px; padding: 16px 40px; font-size: 36px; font-weight: bold; letter-spacing: 8px; color: #333;">
                            %s
                        </span>
                    </div>
                    <p style="color: #888; font-size: 13px;">Mã có hiệu lực trong <strong>%d phút</strong>. Nếu bạn không thực hiện yêu cầu này, hãy bỏ qua email.</p>
                    <hr style="border: none; border-top: 1px solid #eee; margin: 24px 0;">
                    <p style="color: #aaa; font-size: 12px;">PET EYE — Nền tảng chăm sóc thú cưng</p>
                </div>
                """.formatted(purpose, fullName != null ? fullName : "bạn", purpose, otp, minutes);
    }
}
