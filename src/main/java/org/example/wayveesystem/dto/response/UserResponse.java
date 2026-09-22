package org.example.wayveesystem.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.wayveesystem.common.enums.Role;
import org.example.wayveesystem.common.enums.UserStatus;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {
    Long userId;
    String username;
    String email;
    String fullName;
    String phone;
    LocalDateTime createdAt;
    UserStatus status;
    Role role;
}
