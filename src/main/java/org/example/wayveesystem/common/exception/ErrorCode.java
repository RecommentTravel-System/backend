package org.example.wayveesystem.common.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatusCode;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Uncategorized error key", org.springframework.http.HttpStatus.BAD_REQUEST),
    USER_EXISTED(1002, "User already exists", org.springframework.http.HttpStatus.BAD_REQUEST),
    USERNAME_INVALID(1003, "Username must be at least 3 characters", org.springframework.http.HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1004, "Password must be at least 8 characters", org.springframework.http.HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1005, "User not found", org.springframework.http.HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1006, "Unauthenticated", org.springframework.http.HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1007, "You do not have permission", org.springframework.http.HttpStatus.FORBIDDEN),
    EMAIL_EXISTED(1008, "Email already exists", org.springframework.http.HttpStatus.BAD_REQUEST),
    EMAIL_REQUIRED(1009, "Email is required", org.springframework.http.HttpStatus.BAD_REQUEST),
    INVALID_EMAIL(1010, "Email is invalid", org.springframework.http.HttpStatus.BAD_REQUEST),
    INVALID_TOKEN(1011, "Invalid token", org.springframework.http.HttpStatus.UNAUTHORIZED),
    USER_INACTIVE(1012, "User is inactive", org.springframework.http.HttpStatus.BAD_REQUEST),
    USER_SUSPENDED(1013, "User is suspended", org.springframework.http.HttpStatus.BAD_REQUEST),
    USER_DELETED(1014, "User is deleted", org.springframework.http.HttpStatus.BAD_REQUEST),
    PASSWORD_INCORRECT(1015, "Password incorrect", org.springframework.http.HttpStatus.BAD_REQUEST),
    USER_NOT_VERIFIED(2001, "Please verify your email before logging in", org.springframework.http.HttpStatus.FORBIDDEN),
    OTP_EXPIRED(3001, "OTP has expired", org.springframework.http.HttpStatus.BAD_REQUEST),
    OTP_INVALID(3002, "OTP is invalid", org.springframework.http.HttpStatus.BAD_REQUEST),
    OTP_MAX_ATTEMPT(3003, "Too many attempts. Try again later", org.springframework.http.HttpStatus.BAD_REQUEST),
    OTP_RESEND_COOLDOWN(3004, "Please wait before requesting another OTP", org.springframework.http.HttpStatus.BAD_REQUEST),
    OTP_RATE_LIMIT(3005, "Too many OTP requests", org.springframework.http.HttpStatus.BAD_REQUEST),
    EMAIL_SEND_FAILED(3006, "Failed to send email", org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR),
    EMAIL_ALREADY_VERIFIED(3007, "Email has already verified", org.springframework.http.HttpStatus.BAD_REQUEST),
    OTP_REQUIRED(3008, "Otp is required", org.springframework.http.HttpStatus.BAD_REQUEST),
    PASSWORD_CONFIRM_NOT_MATCH(3010, "Password confirmation does not match", org.springframework.http.HttpStatus.BAD_REQUEST),
    PASSWORD_SAME_AS_OLD(3011, "New password must be different from old password", org.springframework.http.HttpStatus.BAD_REQUEST),
    PASSWORD_REQUIRED(3012, "Password is required", org.springframework.http.HttpStatus.BAD_REQUEST),
    ;
    int code;
    String message;
    HttpStatusCode httpStatusCode;

    ErrorCode(int code, String message, HttpStatusCode httpStatusCode) {
        this.code = code;
        this.message = message;
        this.httpStatusCode = httpStatusCode;
    }
}
