package org.example.wayveesystem.service;

public interface OtpService {
    String generateOtp(String email);
    void verifyOtp(String email, String otp);
    String resendOtp(String email);
}
