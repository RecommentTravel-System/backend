package org.example.wayveesystem.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "otp_email")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OtpEmail {
    @Id
    @Column(name = "email", nullable = false)
    String email;

    @Column(name = "otp_hash")
    String otpHash;

    @Column(name = "attempts")
    int attempts;

    @Column(name = "expire_at")
    long expireAt;

    @Column(name = "resend_cooldown_until")
    long resendCooldownUntil;

    @Column(name = "block_until")
    long blockUntil;

    @Column(name = "rate_limit_count")
    int rateLimitCount;

    @Column(name = "rate_limit_reset_at")
    long rateLimitResetAt;
}
