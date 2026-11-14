package org.example.wayveesystem.service;

import org.example.wayveesystem.common.enums.AuthProvider;
import org.example.wayveesystem.common.enums.Role;
import org.example.wayveesystem.common.enums.UserStatus;
import org.example.wayveesystem.dto.request.GoogleLoginRequest;
import org.example.wayveesystem.dto.response.AuthenticationResponse;
import org.example.wayveesystem.mapper.UserMapper;
import org.example.wayveesystem.model.User;
import org.example.wayveesystem.repository.InvalidatedTokenRepository;
import org.example.wayveesystem.repository.UserRepository;
import org.example.wayveesystem.service.impl.AuthenticationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthenticationServiceGoogleLoginTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Spy
    private UserMapper userMapper = new UserMapper();

    @Mock
    private InvalidatedTokenRepository invalidatedTokenRepository;

    @Mock
    private OtpService otpService;

    @Mock
    private EmailService emailService;

    @Mock
    private CloudinaryImageService cloudinaryImageService;

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                authenticationService,
                "SIGNER_KEY",
                "1234567890123456789012345678901234567890123456789012345678901234"
        );
    }

    @Test
    void testLoginWithGoogle_NewUser_CreatesAndSavesUser() {
        GoogleLoginRequest request = GoogleLoginRequest.builder()
                .email("newgoogleuser@gmail.com")
                .fullName("Google User")
                .avatarUrl("https://photo.url")
                .googleId("google-uid-123")
                .build();

        when(userRepository.findByEmail("newgoogleuser@gmail.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setUserId(99L);
            return u;
        });

        AuthenticationResponse response = authenticationService.loginWithGoogle(request);

        assertNotNull(response);
        assertTrue(response.isAuthenticated());
        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("newgoogleuser@gmail.com", response.getUserResponse().getEmail());
        assertEquals(AuthProvider.GOOGLE, response.getUserResponse().getAuthProvider());
        assertEquals(UserStatus.ACTIVE, response.getUserResponse().getStatus());

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testLoginWithGoogle_ExistingUser_UpdatesAndSaves() {
        GoogleLoginRequest request = GoogleLoginRequest.builder()
                .email("existing@gmail.com")
                .fullName("Existing User")
                .avatarUrl("https://newphoto.url")
                .build();

        User existing = User.builder()
                .userId(50L)
                .email("existing@gmail.com")
                .fullName("Old Name")
                .status(UserStatus.PENDING)
                .role(Role.USER)
                .authProvider(null)
                .build();

        when(userRepository.findByEmail("existing@gmail.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthenticationResponse response = authenticationService.loginWithGoogle(request);

        assertNotNull(response);
        assertTrue(response.isAuthenticated());
        assertEquals(UserStatus.ACTIVE, existing.getStatus());
        assertEquals(AuthProvider.GOOGLE, existing.getAuthProvider());
        verify(userRepository, times(1)).save(existing);
    }
}
