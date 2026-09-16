package org.example.wayveesystem.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.example.wayveesystem.common.exception.AppException;
import org.example.wayveesystem.common.exception.ErrorCode;
import org.example.wayveesystem.entity.OtpEmail;
import org.example.wayveesystem.respository.OtpRepository;
import org.example.wayveesystem.service.OtpService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OtpServiceImpl implements OtpService {
    OtpRepository otpRepository;
    PasswordEncoder passwordEncoder;
    SecureRandom secureRandom = new SecureRandom();

    @NonFinal
    @Value("${otp.length}")
    int OTP_LENGTH;

    @NonFinal
    @Value("${otp.expiry.duration}")
    int OTP_EXPIRATION;

    @NonFinal
    @Value("${otp.max.attempts}")
    int MAX_ATTEMPTS;

    @NonFinal
    @Value("${otp.block.duration}")
    int BLOCK_DURATION;

    @NonFinal
    @Value("${otp.resend.cooldown}")
    int RESEND_COOLDOWN;

    @NonFinal
    @Value("${otp.rate.limit}")
    int RATE_LIMIT;

    private long now() {
        return Instant.now().getEpochSecond();
    }

    private String generateOtpCode(int length) {
        int bound = (int) Math.pow(10, length);
        return String.format("%0" + length + "d", secureRandom.nextInt(bound));
    }

    private void checkRateLimit(OtpEmail data) {
        long now = now();
        int count = 0;
        long reset = now + 3600;

        if (data != null) {
            count = data.getRateLimitCount();
            reset = data.getRateLimitResetAt();

            if (now > reset) {
                count = 0;
                reset = now + 3600;
            }
        }
        count++;

        if (count > RATE_LIMIT) {
            throw new AppException(ErrorCode.OTP_RATE_LIMIT);
        }
    }

    @Override
    public String generateOtp(String email) {
        OtpEmail existingData = otpRepository.findById(email).orElse(null);
        checkRateLimit(existingData);
        long now = now();

        if (existingData != null) {
            if (now < existingData.getResendCooldownUntil()) {
                throw new AppException(ErrorCode.OTP_RESEND_COOLDOWN);
            }

            if (now < existingData.getBlockUntil()) {
                throw new AppException(ErrorCode.OTP_MAX_ATTEMPT);
            }
        }

        String otp = generateOtpCode(OTP_LENGTH);
        String hash = passwordEncoder.encode(otp);

        int rateCount = existingData != null && now <= existingData.getRateLimitResetAt()
                ? existingData.getRateLimitCount() + 1 : 1;
        long rateReset = existingData != null && now <= existingData.getRateLimitResetAt()
                ? existingData.getRateLimitResetAt() : now + 3600;

        OtpEmail newData = OtpEmail.builder()
                .email(email)
                .otpHash(hash)
                .attempts(0)
                .expireAt(now + OTP_EXPIRATION)
                .resendCooldownUntil(now + RESEND_COOLDOWN)
                .blockUntil(0)
                .rateLimitCount(rateCount)
                .rateLimitResetAt(rateReset)
                .build();

        otpRepository.save(newData);
        return otp;
    }

    @Override
    public void verifyOtp(String email, String otp) {
        OtpEmail data = otpRepository.findById(email).orElse(null);
        if (data == null || now() > data.getExpireAt()) {
            throw new AppException(ErrorCode.OTP_EXPIRED);
        }

        long now = now();
        if (now < data.getBlockUntil()) {
            throw new AppException(ErrorCode.OTP_MAX_ATTEMPT);
        }

        if (!passwordEncoder.matches(otp, data.getOtpHash())) {
            int attempts = data.getAttempts() + 1;
            data.setAttempts(attempts);

            if (attempts >= MAX_ATTEMPTS) {
                data.setBlockUntil(now + BLOCK_DURATION);
                otpRepository.save(data);
                throw new AppException(ErrorCode.OTP_MAX_ATTEMPT);
            }

            otpRepository.save(data);
            throw new AppException(ErrorCode.OTP_INVALID);
        }

        otpRepository.delete(data);
    }

    @Override
    public String resendOtp(String email) {
        return generateOtp(email);
    }
}
