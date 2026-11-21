package org.example.wayveesystem.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "access_logs", indexes = {
        @Index(name = "idx_access_created_at", columnList = "created_at"),
        @Index(name = "idx_access_ip", columnList = "ip_address"),
        @Index(name = "idx_access_path", columnList = "path")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @Column(name = "ip_address", length = 64)
    String ipAddress;

    @Column(name = "path", length = 512, nullable = false)
    String path;

    @Column(name = "page_title", length = 256)
    String pageTitle;

    @Column(name = "method", length = 16)
    @Builder.Default
    String method = "GET";

    @Column(name = "device_type", length = 32)
    @Builder.Default
    String deviceType = "DESKTOP";

    @Column(name = "browser", length = 64)
    String browser;

    @Column(name = "operating_system", length = 64)
    String operatingSystem;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    String userAgent;

    @Column(name = "user_id")
    Long userId;

    @Column(name = "user_email", length = 128)
    String userEmail;

    @Column(name = "user_full_name", length = 128)
    String userFullName;

    @Column(name = "status_code")
    @Builder.Default
    Integer statusCode = 200;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;
}
