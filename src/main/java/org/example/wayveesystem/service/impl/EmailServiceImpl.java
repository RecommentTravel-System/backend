package org.example.wayveesystem.service.impl;

import org.example.wayveesystem.common.exception.AppException;
import org.example.wayveesystem.common.exception.ErrorCode;
import org.example.wayveesystem.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private void sendHtmlEmail(String toEmail, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send email to {}", toEmail, e);
            throw new AppException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    @Override
    public void sendVerifyEmailOtp(String toEmail, String otp) {
        String subject = "Verify your email - WayveeSystem";
        String htmlContent = "<div style=\"font-family: Arial, sans-serif; padding: 20px;\">"
                + "<h2>Verify Your Email</h2>"
                + "<p>Thank you for registering with WayveeSystem. Your verification OTP code is:</p>"
                + "<h1 style=\"color: #4CAF50; letter-spacing: 5px;\">" + otp + "</h1>"
                + "<p>This code will expire in 5 minutes. Please do not share this code with anyone.</p>"
                + "</div>";
        sendHtmlEmail(toEmail, subject, htmlContent);
    }

    @Override
    public void sendForgotPasswordOtp(String toEmail, String otp) {
        String subject = "Reset your password - WayveeSystem";
        String htmlContent = "<div style=\"font-family: Arial, sans-serif; padding: 20px;\">"
                + "<h2>Reset Your Password</h2>"
                + "<p>You requested a password reset. Your OTP code is:</p>"
                + "<h1 style=\"color: #FF5722; letter-spacing: 5px;\">" + otp + "</h1>"
                + "<p>This code will expire in 5 minutes. If you did not request this, please ignore this email.</p>"
                + "</div>";
        sendHtmlEmail(toEmail, subject, htmlContent);
    }
}
