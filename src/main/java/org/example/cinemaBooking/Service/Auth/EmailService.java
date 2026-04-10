package org.example.cinemaBooking.Service.Auth;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cinemaBooking.Entity.Booking;
import org.example.cinemaBooking.Repository.BookingRepository;
import org.example.cinemaBooking.Service.Ticket.TicketService;
import org.example.cinemaBooking.Shared.utils.EmailTemplateUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    private final TicketService ticketService;
    private final BookingRepository bookingRepository;
    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Gửi email reset password
     * Dùng @Async để không block request — gửi email chạy nền
     */
    @Async
    public void sendPasswordResetEmail(String toEmail, String token, long expiryMinutes) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("[Cinema Booking] Đặt lại mật khẩu");

            String resetLink = frontendUrl + "/reset-password?token=" + token;
            String htmlContent = buildEmailContent(resetLink, expiryMinutes);

            helper.setText(htmlContent, true); // true = HTML

            mailSender.send(message);
            log.info("Password reset email sent to: {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send password reset email to: {}", toEmail, e);
            // Không throw exception — tránh lộ thông tin email có tồn tại hay không
        }
    }

    private String buildEmailContent(String resetLink, long expiryMinutes) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto;">
                    <h2 style="color: #e50914;">🎬 Cinema Booking</h2>
                    <p>Bạn vừa yêu cầu đặt lại mật khẩu.</p>
                    <p>Nhấn vào nút bên dưới để đặt lại mật khẩu:</p>
                    <a href="%s"
                       style="display: inline-block; padding: 12px 24px;
                              background-color: #e50914; color: white;
                              text-decoration: none; border-radius: 4px;
                              font-weight: bold;">
                        Đặt lại mật khẩu
                    </a>
                    <p style="color: #888; margin-top: 16px;">
                        Link này sẽ hết hạn sau <strong>%d phút</strong>.
                    </p>
                    <p style="color: #888;">
                        Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này.
                    </p>
                    <hr style="border: none; border-top: 1px solid #eee;" />
                    <p style="color: #aaa; font-size: 12px;">
                        © 2025 Cinema Booking System
                    </p>
                </div>
                """.formatted(resetLink, expiryMinutes);
    }

    @Async
    @Transactional(readOnly = true)
    public void sendBookingSuccessEmail(String bookingCode) {
        // Query 1: lấy booking đầy đủ tickets + showtime + user
        Booking booking = bookingRepository.findWithTicketsByBookingCode(bookingCode)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingCode));

        // Query 2: merge bookingProducts vào — Hibernate tự merge vào persistent context
        bookingRepository.findWithProductsByBookingCode(bookingCode)
                .ifPresent(b -> booking.getBookingProducts().addAll(b.getBookingProducts()));

        String qrBase64 = ticketService.getBookingQR(bookingCode);
        String to       = booking.getUser().getEmail();
        String subject  = "🎬 Đặt vé thành công - " + bookingCode;
        String html     = EmailTemplateUtil.buildBookingSuccessEmail(booking);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            byte[] imageBytes = Base64.getDecoder().decode(qrBase64);
            helper.addInline("qrImage", new ByteArrayResource(imageBytes), "image/png");

            mailSender.send(message);
            log.info("Booking success email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send booking success email to {}: {}", to, e.getMessage());
        }
    }

    @Async
    public void sendCancelledEmail(Booking booking) {
        String to      = booking.getUser().getEmail();
        String subject = "Thông báo huỷ vé - " + booking.getBookingCode();
        String html    = EmailTemplateUtil.buildCancelledEmail(booking);
        sendHtmlEmail(to, subject, html);
    }

    private void sendHtmlEmail(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);   // true = isHtml
            mailSender.send(message);
            log.info("Email sent to {}: {}", to, subject);
        } catch (MessagingException e) {
            // Không throw — email fail không được làm hỏng flow chính
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}