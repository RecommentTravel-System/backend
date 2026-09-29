package org.example.wayveesystem.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.wayveesystem.common.enums.Role;
import org.example.wayveesystem.common.enums.UserStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminUserUpdateRequest {
    String fullName;
    String phone;
    UserStatus status;
    Role role;
    String password;
}
