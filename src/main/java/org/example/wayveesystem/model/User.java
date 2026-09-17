package org.example.wayveesystem.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.wayveesystem.common.enums.Role;
import org.example.wayveesystem.common.enums.UserStatus;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    Long userId;

    @Column(name = "email", unique = true, nullable = false)
    String email;

    @Column(name = "password", nullable = false)
    String password;

    @Column(name = "full_name")
    String fullName;

    @Column(name = "phone")
    String phone;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    UserStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    Role role;
}
