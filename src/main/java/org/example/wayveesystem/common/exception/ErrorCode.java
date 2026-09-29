package org.example.wayveesystem.common.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpStatus;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Uncategorized error key", HttpStatus.BAD_REQUEST),
    USER_EXISTED(1002, "User already exists", HttpStatus.BAD_REQUEST),
    USERNAME_INVALID(1003, "Username must be at least 3 characters", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1004, "Password must be at least 8 characters", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1005, "User not found", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1006, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1007, "You do not have permission", HttpStatus.FORBIDDEN),
    EMAIL_EXISTED(1008, "Email already exists", HttpStatus.BAD_REQUEST),
    EMAIL_REQUIRED(1009, "Email is required", HttpStatus.BAD_REQUEST),
    INVALID_EMAIL(1010, "Email is invalid", HttpStatus.BAD_REQUEST),
    INVALID_TOKEN(1011, "Invalid token", HttpStatus.UNAUTHORIZED),
    USER_INACTIVE(1012, "User is inactive", HttpStatus.BAD_REQUEST),
    USER_SUSPENDED(1013, "User is suspended", HttpStatus.BAD_REQUEST),
    USER_DELETED(1014, "User is deleted", HttpStatus.BAD_REQUEST),
    PASSWORD_INCORRECT(1015, "Password incorrect", HttpStatus.BAD_REQUEST),
    USER_NOT_VERIFIED(2001, "Please verify your email before logging in", HttpStatus.FORBIDDEN),
    OTP_EXPIRED(3001, "OTP has expired", HttpStatus.BAD_REQUEST),
    OTP_INVALID(3002, "OTP is invalid", HttpStatus.BAD_REQUEST),
    OTP_MAX_ATTEMPT(3003, "Too many attempts. Try again later", HttpStatus.BAD_REQUEST),
    OTP_RESEND_COOLDOWN(3004, "Please wait before requesting another OTP", HttpStatus.BAD_REQUEST),
    OTP_RATE_LIMIT(3005, "Too many OTP requests", HttpStatus.BAD_REQUEST),
    EMAIL_SEND_FAILED(3006, "Failed to send email", HttpStatus.INTERNAL_SERVER_ERROR),
    EMAIL_ALREADY_VERIFIED(3007, "Email has already verified", HttpStatus.BAD_REQUEST),
    OTP_REQUIRED(3008, "Otp is required", HttpStatus.BAD_REQUEST),
    PASSWORD_CONFIRM_NOT_MATCH(3010, "Password confirmation does not match", HttpStatus.BAD_REQUEST),
    PASSWORD_SAME_AS_OLD(3011, "New password must be different from old password", HttpStatus.BAD_REQUEST),
    PASSWORD_REQUIRED(3012, "Password is required", HttpStatus.BAD_REQUEST),

    EXTERNAL_MAP_SERVICE_UNAVAILABLE(4001, "External map service is unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    EXTERNAL_MAP_TIMEOUT(4002, "External map service timeout", HttpStatus.GATEWAY_TIMEOUT),
    LOCATION_NOT_FOUND(4003, "Location not found", HttpStatus.NOT_FOUND),
    IMAGE_REQUIRED(4011, "Image file is required", HttpStatus.BAD_REQUEST),
    INVALID_IMAGE_TYPE(4012, "Only image files are supported", HttpStatus.BAD_REQUEST),
    IMAGE_TOO_LARGE(4013, "Image size must not exceed 10 MB", HttpStatus.PAYLOAD_TOO_LARGE),
    IMAGE_UPLOAD_FAILED(4014, "Failed to upload image", HttpStatus.INTERNAL_SERVER_ERROR),

    CATEGORY_NOT_FOUND(4004, "Category not found", HttpStatus.NOT_FOUND),
    CATEGORY_CODE_EXISTED(4005, "Category code already exists", HttpStatus.BAD_REQUEST),
    FAVORITE_NOT_FOUND(4006, "Favorite item not found", HttpStatus.NOT_FOUND),
    FAVORITE_ALREADY_EXISTS(4007, "Location is already favorited", HttpStatus.BAD_REQUEST),
    REVIEW_NOT_FOUND(4008, "Review not found", HttpStatus.NOT_FOUND),
    SUBSCRIPTION_NOT_FOUND(4009, "Subscription not found", HttpStatus.NOT_FOUND),
    UNAUTHORIZED_ACTION(4010, "You do not have permission to perform this action", HttpStatus.FORBIDDEN),

    TRIP_NAME_REQUIRED(5001, "Tên chuyến đi không được để trống", HttpStatus.BAD_REQUEST),
    TRIP_DATES_REQUIRED(5002, "Ngày đi - về không được để trống", HttpStatus.BAD_REQUEST),
    DESTINATION_REQUIRED(5003, "Điểm đến không được để trống", HttpStatus.BAD_REQUEST),
    COMPANIONS_REQUIRED(5004, "Thông tin người đi cùng không được để trống", HttpStatus.BAD_REQUEST),
    PASSENGER_COUNT_REQUIRED(5005, "Số lượng người không được để trống", HttpStatus.BAD_REQUEST),
    TRAVEL_STYLE_REQUIRED(5006, "Phong cách chuyến đi không được để trống", HttpStatus.BAD_REQUEST),
    TRIP_NOT_FOUND(5007, "Trip not found", HttpStatus.NOT_FOUND),
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
