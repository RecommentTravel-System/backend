package org.example.wayveesystem.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccessLogRecordRequest {
    String path;
    String pageTitle;
    String userAgent;
    String deviceType;
    String browser;
    String operatingSystem;
    Long userId;
    String userEmail;
    String userFullName;
}
