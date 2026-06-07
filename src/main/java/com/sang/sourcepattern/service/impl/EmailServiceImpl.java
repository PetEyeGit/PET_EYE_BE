package com.sang.sourcepattern.service.impl;

import com.sang.sourcepattern.entity.Booking;
import com.sang.sourcepattern.service.EmailService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class EmailServiceImpl implements EmailService {

    JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    @NonFinal
    String fromEmail;

    // ─── @Async trên từng method interface để Spring proxy intercept đúng ─────

    @Async
    @Override
    public void sendVerificationEmail(String toEmail, String fullName, String otp) {
        String subject = "[PET EYE] Mã xác thực OTP của bạn";
        doSend(toEmail, subject, buildOtpEmailHtml(fullName, otp, "xác thực email", 10));
    }

    @Async
    @Override
    public void sendPasswordResetEmail(String toEmail, String fullName, String otp) {
        String subject = "[PET EYE] Mã OTP đặt lại mật khẩu";
        doSend(toEmail, subject, buildOtpEmailHtml(fullName, otp, "đặt lại mật khẩu", 15));
    }

    @Async
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
        doSend(toEmail, subject, html);
    }

    @Async
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
        doSend(toEmail, subject, html);
    }

    @Async
    @Override
    public void sendBookingInvoiceEmail(String toEmail, Booking booking,
                                        BigDecimal paidAmount,
                                        String paymentMethod,
                                        String paymentStatus) {
        String subject = "[PET EYE] Hóa đơn dịch vụ #" + booking.getId();
        String html = buildInvoiceEmailHtml(booking, paidAmount, paymentMethod, paymentStatus);
        doSend(toEmail, subject, html);
    }

    private String buildInvoiceEmailHtml(Booking booking, BigDecimal paidAmount,
                                          String paymentMethod, String paymentStatus) {
        NumberFormat vndFmt = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));
        DateTimeFormatter dtFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        // ── Thông tin shop ────────────────────────────────────────────────────
        var shop = booking.getShop();
        String shopName    = shop != null && shop.getShopName() != null ? shop.getShopName() : "PET EYE";
        String shopAddress = shop != null && shop.getAddress() != null  ? shop.getAddress()  : "—";
        String shopPhone   = shop != null && shop.getPhone() != null    ? shop.getPhone()    : "—";
        String shopEmail   = shop != null && shop.getEmail() != null    ? shop.getEmail()    : "—";

        // ── Thông tin khách hàng ──────────────────────────────────────────────
        var user = booking.getUser();
        String customerName  = user != null && user.getFullName() != null ? user.getFullName() : "Khách hàng";
        String customerEmail = user != null && user.getEmail() != null    ? user.getEmail()    : "—";
        String customerPhone = user != null && user.getPhone() != null    ? user.getPhone()    : "—";

        // ── Bảng dịch vụ ─────────────────────────────────────────────────────
        StringBuilder serviceRows = new StringBuilder();
        BigDecimal totalServicePrice = BigDecimal.ZERO;
        if (booking.getServices() != null) {
            for (com.sang.sourcepattern.entity.Service s : booking.getServices()) {
                BigDecimal price = s.getPrice() != null ? s.getPrice() : BigDecimal.ZERO;
                totalServicePrice = totalServicePrice.add(price);
                serviceRows.append("""
                        <tr>
                          <td style="padding:9px 12px;border-bottom:1px solid #f0f0f0;">%s</td>
                          <td style="padding:9px 12px;border-bottom:1px solid #f0f0f0;text-align:right;font-weight:600;color:#2e7d32;">%s ₫</td>
                        </tr>
                        """.formatted(
                        s.getServiceName() != null ? s.getServiceName() : "Dịch vụ",
                        vndFmt.format(price)
                ));
            }
        }

        // ── Dòng giảm giá (nếu có) ────────────────────────────────────────────
        String discountRow = "";
        if (booking.getDiscountAmount() != null && booking.getDiscountAmount() > 0) {
            discountRow = """
                    <tr>
                      <td style="padding:9px 12px;border-bottom:1px solid #f0f0f0;color:#e53935;">Giảm giá (voucher)</td>
                      <td style="padding:9px 12px;border-bottom:1px solid #f0f0f0;text-align:right;color:#e53935;">-%s ₫</td>
                    </tr>
                    """.formatted(vndFmt.format(booking.getDiscountAmount()));
        }

        // ── Nhãn phương thức thanh toán ───────────────────────────────────────
        String methodLabel = switch (paymentMethod) {
            case "PAYOS"        -> "Chuyển khoản PayOS";
            case "CASH_DEPOSIT" -> "Tiền mặt (đặt cọc qua PayOS)";
            case "CASH"         -> "Tiền mặt";
            case "MOCK"         -> "PayOS (Demo)";
            default             -> paymentMethod;
        };

        String appointmentStr = booking.getAppointmentDatetime() != null
                ? booking.getAppointmentDatetime().format(dtFmt) : "—";
        String petName = booking.getPet() != null ? booking.getPet().getName() : "—";

        return """
                <div style="font-family:Arial,sans-serif;max-width:660px;margin:auto;border:1px solid #e0e0e0;border-radius:10px;overflow:hidden;">

                  <!-- Header -->
                  <div style="background:#4CAF50;padding:22px 32px;">
                    <h2 style="color:#fff;margin:0;font-size:21px;">🐾 PET EYE — Hóa đơn dịch vụ</h2>
                    <p style="color:#c8e6c9;margin:5px 0 0;font-size:13px;">Mã đơn: <strong>#%d</strong> &nbsp;|&nbsp; %s</p>
                  </div>

                  <div style="padding:28px 32px;">

                    <!-- Thông tin shop & khách hàng (2 cột) -->
                    <table style="width:100%%;border-collapse:collapse;margin-bottom:24px;font-size:13px;">
                      <tr>
                        <td style="width:50%%;vertical-align:top;padding-right:16px;">
                          <p style="margin:0 0 6px;font-weight:700;font-size:14px;color:#333;">Thông tin cửa hàng</p>
                          <p style="margin:3px 0;color:#555;"><strong>%s</strong></p>
                          <p style="margin:3px 0;color:#777;">📍 %s</p>
                          <p style="margin:3px 0;color:#777;">📞 %s</p>
                          <p style="margin:3px 0;color:#777;">✉️ %s</p>
                        </td>
                        <td style="width:50%%;vertical-align:top;padding-left:16px;border-left:1px solid #f0f0f0;">
                          <p style="margin:0 0 6px;font-weight:700;font-size:14px;color:#333;">Thông tin khách hàng</p>
                          <p style="margin:3px 0;color:#555;"><strong>%s</strong></p>
                          <p style="margin:3px 0;color:#777;">✉️ %s</p>
                          <p style="margin:3px 0;color:#777;">📞 %s</p>
                          <p style="margin:3px 0;color:#777;">🐾 Thú cưng: <strong>%s</strong></p>
                          <p style="margin:3px 0;color:#777;">📅 Ngày hẹn: %s</p>
                        </td>
                      </tr>
                    </table>

                    <!-- Bảng dịch vụ -->
                    <p style="font-weight:700;font-size:14px;margin:0 0 8px;">Chi tiết dịch vụ</p>
                    <table style="width:100%%;border-collapse:collapse;font-size:14px;margin-bottom:20px;">
                      <thead>
                        <tr style="background:#f5f5f5;">
                          <th style="padding:10px 12px;text-align:left;font-weight:600;color:#333;">Tên dịch vụ</th>
                          <th style="padding:10px 12px;text-align:right;font-weight:600;color:#333;">Giá</th>
                        </tr>
                      </thead>
                      <tbody>
                        %s
                        %s
                        <tr style="background:#f1f8e9;">
                          <td style="padding:11px 12px;font-weight:700;font-size:15px;">Tổng cộng</td>
                          <td style="padding:11px 12px;text-align:right;font-weight:700;font-size:15px;color:#2e7d32;">%s ₫</td>
                        </tr>
                      </tbody>
                    </table>

                    <!-- Thông tin thanh toán -->
                    <div style="background:#f9f9f9;border:1px solid #e8e8e8;border-radius:8px;padding:16px 20px;font-size:14px;">
                      <table style="width:100%%;border-collapse:collapse;">
                        <tr>
                          <td style="padding:6px 0;color:#666;">Số tiền đã thanh toán</td>
                          <td style="padding:6px 0;text-align:right;font-weight:700;font-size:16px;color:#2e7d32;">%s ₫</td>
                        </tr>
                        <tr>
                          <td style="padding:6px 0;color:#666;">Phương thức thanh toán</td>
                          <td style="padding:6px 0;text-align:right;color:#333;">%s</td>
                        </tr>
                        <tr>
                          <td style="padding:6px 0;color:#666;">Trạng thái</td>
                          <td style="padding:6px 0;text-align:right;">
                            <span style="background:#e8f5e9;color:#2e7d32;padding:4px 12px;border-radius:12px;font-weight:700;font-size:13px;">✓ %s</span>
                          </td>
                        </tr>
                      </table>
                    </div>
                  </div>

                  <!-- Footer -->
                  <div style="border-top:1px solid #eee;padding:14px 32px;background:#fafafa;">
                    <p style="color:#aaa;font-size:12px;margin:0;">Cảm ơn bạn đã tin dùng PET EYE 🐾 — Nếu có thắc mắc, vui lòng liên hệ cửa hàng.</p>
                  </div>
                </div>
                """.formatted(
                // header
                booking.getId(), appointmentStr,
                // shop
                shopName, shopAddress, shopPhone, shopEmail,
                // customer
                customerName, customerEmail, customerPhone, petName, appointmentStr,
                // services
                serviceRows, discountRow, vndFmt.format(totalServicePrice),
                // payment
                vndFmt.format(paidAmount), methodLabel, paymentStatus
        );
    }

    @Async
    @Override
    public void sendAppointmentReminderEmail(String toEmail, Booking booking) {
        String subject = "[PET EYE] ⏰ Nhắc nhở: Bạn có lịch hẹn sắp tới!";
        DateTimeFormatter dtFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String appointmentStr = booking.getAppointmentDatetime() != null
                ? booking.getAppointmentDatetime().format(dtFmt) : "—";
        var shop = booking.getShop();
        String shopName = shop != null && shop.getShopName() != null ? shop.getShopName() : "PET EYE";
        String shopAddress = shop != null && shop.getAddress() != null ? shop.getAddress() : "—";
        var user = booking.getUser();
        String customerName = user != null && user.getFullName() != null ? user.getFullName() : "Khách hàng";
        String petName = booking.getPet() != null ? booking.getPet().getName() : "—";

        String html = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 24px; border: 1px solid #e0e0e0; border-radius: 8px;">
                    <h2 style="color: #4CAF50;">⏰ Nhắc nhở lịch hẹn sắp tới</h2>
                    <p>Xin chào <strong>%s</strong>,</p>
                    <p>PET EYE xin nhắc nhở bạn có một lịch hẹn chăm sóc cho bé <strong>%s</strong> sẽ diễn ra trong vòng 24 giờ tới.</p>
                    
                    <div style="background-color: #f9f9f9; padding: 16px; border-radius: 8px; margin: 16px 0;">
                        <p style="margin: 4px 0;"><strong>Thời gian:</strong> <span style="color: #e53935; font-size: 16px; font-weight: bold;">%s</span></p>
                        <p style="margin: 4px 0;"><strong>Cửa hàng:</strong> %s</p>
                        <p style="margin: 4px 0;"><strong>Địa chỉ:</strong> %s</p>
                    </div>

                    <p>Vui lòng sắp xếp thời gian đến đúng giờ để bé được phục vụ tốt nhất.</p>

                    <div style="background-color: #fff3e0; border-left: 4px solid #ff9800; padding: 12px; margin-top: 24px;">
                        <h4 style="margin: 0 0 8px; color: #e65100;">⚠️ LƯU Ý VỀ CHÍNH SÁCH HỦY LỊCH:</h4>
                        <ul style="margin: 0; padding-left: 20px; color: #555; font-size: 13px; line-height: 1.5;">
                            <li>Nếu hủy lịch <strong>trước 5 tiếng</strong>: Bạn sẽ được hoàn lại tiền dịch vụ, trừ đi tiền cọc.</li>
                            <li>Nếu hủy lịch <strong>sau 5 tiếng</strong> (sát giờ hẹn): Bạn cần phải trả thêm <strong>50%% tiền thiệt hại</strong> cho shop và mất thêm cọc.</li>
                        </ul>
                    </div>

                    <hr style="border: none; border-top: 1px solid #eee; margin: 24px 0;">
                    <p style="color: #aaa; font-size: 12px;">PET EYE — Nền tảng chăm sóc thú cưng</p>
                </div>
                """.formatted(customerName, petName, appointmentStr, shopName, shopAddress);

        doSend(toEmail, subject, html);
    }

    // ─── Internal helper — không @Async, gọi nội bộ an toàn ─────────────────

    private void doSend(String to, String subject, String htmlContent) {        try {
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

    @Async
    @Override
    public void sendBookingCompletedEmail(String toEmail, Booking booking) {
        String subject = "[PET EYE] Dịch vụ đã hoàn thành - Đơn hàng #" + booking.getId();
        doSend(toEmail, subject, buildBookingCompletedHtml(booking));
    }

    private String buildBookingCompletedHtml(Booking booking) {
        DateTimeFormatter dtFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        NumberFormat vnFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        String shopName = booking.getShop() != null ? booking.getShop().getShopName() : "PET EYE Shop";
        String userName = booking.getUser() != null ? booking.getUser().getFullName() : "Quý khách";
        String petName = booking.getPet() != null ? booking.getPet().getName() : "—";
        String appointmentStr = booking.getAppointmentDatetime() != null
                ? booking.getAppointmentDatetime().format(dtFmt) : "—";

        // Parse completion times
        java.util.Map<Integer, String> completionTimes = new java.util.HashMap<>();
        if (booking.getCompletedServiceTimes() != null && !booking.getCompletedServiceTimes().isBlank()) {
            for (String part : booking.getCompletedServiceTimes().split(",")) {
                String[] kv = part.split(":", 2);
                if (kv.length == 2) {
                    try {
                        Integer sId = Integer.parseInt(kv[0].trim());
                        java.time.LocalDateTime dt = java.time.LocalDateTime.parse(kv[1].trim());
                        completionTimes.put(sId, dt.format(dtFmt));
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
        }

        StringBuilder servicesRows = new StringBuilder();
        BigDecimal total = BigDecimal.ZERO;
        
        if (booking.getServices() != null) {
            for (com.sang.sourcepattern.entity.Service s : booking.getServices()) {
                BigDecimal price = s.getPrice() != null ? s.getPrice() : BigDecimal.ZERO;
                total = total.add(price);
                String completedAt = completionTimes.getOrDefault(s.getId(), "Chưa ghi nhận");
                
                servicesRows.append("<tr>")
                            .append("<td style=\"padding:10px 12px;border-bottom:1px solid #eee;color:#555;\">").append(s.getServiceName()).append("</td>")
                            .append("<td style=\"padding:10px 12px;border-bottom:1px solid #eee;color:#555;\">").append(completedAt).append("</td>")
                            .append("<td style=\"padding:10px 12px;text-align:right;border-bottom:1px solid #eee;color:#555;\">").append(vnFormat.format(price)).append("</td>")
                            .append("</tr>");
            }
        }

        return """
                <div style="font-family:Arial,sans-serif;max-width:660px;margin:auto;border:1px solid #e0e0e0;border-radius:10px;overflow:hidden;">
                  <div style="background:#4CAF50;padding:22px 32px;text-align:center;">
                    <h2 style="color:#fff;margin:0;font-size:24px;">🎉 Hoàn Thành Dịch Vụ!</h2>
                    <p style="color:#c8e6c9;margin:5px 0 0;font-size:14px;">Mã đơn: <strong>#%d</strong></p>
                  </div>
                  <div style="padding:28px 32px;">
                    <p style="font-size:15px;color:#333;">Chào <strong>%s</strong>,</p>
                    <p style="font-size:15px;color:#555;line-height:1.5;">
                      Cửa hàng <strong>%s</strong> đã hoàn thành các dịch vụ chăm sóc cho thú cưng <strong>%s</strong> của bạn (Ngày hẹn: %s).
                    </p>
                    
                    <p style="font-weight:700;font-size:15px;margin:24px 0 8px;color:#333;">Chi tiết dịch vụ</p>
                    <table style="width:100%%;border-collapse:collapse;font-size:14px;margin-bottom:24px;">
                      <thead>
                        <tr style="background:#f5f5f5;">
                          <th style="padding:10px 12px;text-align:left;font-weight:600;color:#333;">Dịch vụ</th>
                          <th style="padding:10px 12px;text-align:left;font-weight:600;color:#333;">Thời gian xong</th>
                          <th style="padding:10px 12px;text-align:right;font-weight:600;color:#333;">Giá</th>
                        </tr>
                      </thead>
                      <tbody>
                        %s
                      </tbody>
                      <tfoot>
                        <tr>
                          <td colspan="2" style="padding:12px;text-align:right;font-weight:700;color:#333;border-top:2px solid #eee;">Tổng tiền dịch vụ:</td>
                          <td style="padding:12px;text-align:right;font-weight:700;color:#e53935;border-top:2px solid #eee;font-size:16px;">%s</td>
                        </tr>
                      </tfoot>
                    </table>

                    <div style="background:#f9f9f9;padding:16px;border-radius:8px;text-align:center;margin-top:24px;border:1px dashed #ccc;">
                      <p style="margin:0 0 8px;font-size:15px;color:#333;">🙏 <strong>Cảm ơn bạn đã tin tưởng PET EYE!</strong></p>
                      <p style="margin:0 0 16px;color:#666;font-size:14px;line-height:1.5;">
                        Chúng tôi hy vọng bạn và thú cưng đã có một trải nghiệm tuyệt vời.<br>
                        Mong rằng sẽ được phục vụ các bạn trong những lần tiếp theo!
                      </p>
                      <p style="margin:0;color:#555;font-size:14px;">
                        Đừng quên đánh giá dịch vụ trên website để chúng tôi cải thiện tốt hơn nhé!
                      </p>
                    </div>

                    <hr style="border:none;border-top:1px solid #eee;margin:32px 0 24px;">
                    <p style="color:#aaa;font-size:12px;text-align:center;margin:0;">PET EYE — Nền tảng chăm sóc thú cưng hàng đầu</p>
                  </div>
                </div>
                """.formatted(booking.getId(), userName, shopName, petName, appointmentStr, servicesRows.toString(), vnFormat.format(total));
    }
}
