package org.example.wayveesystem.configuration;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.wayveesystem.common.enums.Role;
import org.example.wayveesystem.common.enums.UserStatus;
import org.example.wayveesystem.model.User;
import org.example.wayveesystem.respository.UserRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Configuration
public class ApplicationInitConfig {
    PasswordEncoder passwordEncoder;

    @Bean
    ApplicationRunner applicationRunner(UserRepository userRepository) {
        return application -> {
            if (userRepository.findByEmail("admin@gmail.com").isEmpty()) {
                User user = User.builder()
                        .email("admin@gmail.com")
                        .password(passwordEncoder.encode("Admin@1234"))
                        .fullName("Admin")
                        .phone("0123456789")
                        .role(Role.ADMIN)
                        .status(UserStatus.ACTIVE)
                        .build();
                userRepository.save(user);
                log.warn("Admin has been created");
            }

            if (userRepository.findByEmail("customer@gmail.com").isEmpty()) {
                User user = User.builder()
                        .email("customer@gmail.com")
                        .password(passwordEncoder.encode("Customer@1234"))
                        .fullName("Customer")
                        .phone("0987654321")
                        .role(Role.USER)
                        .status(UserStatus.ACTIVE)
                        .build();
                userRepository.save(user);
                log.warn("Customer has been created");
            }
        };
    }
}