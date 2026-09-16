package org.example.wayveesystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PasswordUpdateRequest {
    @NotBlank(message = "PASSWORD_REQUIRED")
    @Size(min = 8, message = "Password must be at least 8 characters")
    String oldPassword;

    @NotBlank(message = "PASSWORD_REQUIRED")
    @Size(min = 8, message = "Password must be at least 8 characters")
    String newPassword;

    @NotBlank(message = "PASSWORD_REQUIRED")
    @Size(min = 8, message = "Password must be at least 8 characters")
    String confirmNewPassword;
}
