package org.example.wayveesystem.service;

public interface EmailService {
    void sendVerifyEmailOtp(String toEmail, String otp);
    void sendForgotPasswordOtp(String toEmail, String otp);
}
