package org.example.wayveesystem.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccessLogResponse {
    Long id;
    String ipAddress;
    String path;
    String pageTitle;
    String method;
    String deviceType;
    String browser;
    String operatingSystem;
    String userAgent;
    Long userId;
    String userEmail;
    String userFullName;
    Integer statusCode;
    LocalDateTime createdAt;
}
