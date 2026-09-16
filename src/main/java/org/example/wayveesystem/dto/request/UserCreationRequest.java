package org.example.wayveesystem.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreationRequest {

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    String email;

    @Size(min = 6, message = "INVALID_PASSWORD")
    @NotBlank(message = "Password is required")
    String password;

    String fullName;

    String phone;
}
