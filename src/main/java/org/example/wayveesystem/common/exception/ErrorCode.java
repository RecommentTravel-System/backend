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
    USERNAME_EXISTED(1009, "Username already exists", org.springframework.http.HttpStatus.BAD_REQUEST),
    INVALID_TOKEN(1010, "Invalid token", org.springframework.http.HttpStatus.UNAUTHORIZED),
    USER_INACTIVE(1011, "User is inactive", org.springframework.http.HttpStatus.BAD_REQUEST),
    USER_SUSPENDED(1012, "User is suspended", org.springframework.http.HttpStatus.BAD_REQUEST),
    USER_DELETED(1013, "User is deleted", org.springframework.http.HttpStatus.BAD_REQUEST),
    PASSWORD_INCORRECT(1014, "Password incorrect", org.springframework.http.HttpStatus.BAD_REQUEST),
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
