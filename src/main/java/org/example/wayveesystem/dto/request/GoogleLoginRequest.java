package org.example.wayveesystem.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request payload cho đăng nhập hoặc đăng ký bằng Google")
public class GoogleLoginRequest {

    @Schema(description = "Địa chỉ email của tài khoản Google", example = "user@gmail.com")
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email;

    @Schema(description = "Họ và tên hiển thị từ Google", example = "Nguyễn Văn A")
    String fullName;

    @Schema(description = "Đường dẫn ảnh đại diện từ Google", example = "https://lh3.googleusercontent.com/a/...")
    String avatarUrl;

    @Schema(description = "Google UID hoặc Firebase UID", example = "google-uid-12345")
    String googleId;

    @Schema(description = "Firebase ID Token (tùy chọn)", example = "eyJhbGciOiJSUzI1NiIs...")
    String idToken;
}
