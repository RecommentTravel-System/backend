package org.example.wayveesystem.controller;

import com.nimbusds.jose.JOSEException;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.wayveesystem.common.response.ApiResponse;
import org.example.wayveesystem.dto.request.*;
import org.example.wayveesystem.dto.response.AuthenticationResponse;
import org.example.wayveesystem.dto.response.IntrospectResponse;
import org.example.wayveesystem.dto.response.UserResponse;
import org.example.wayveesystem.service.AuthenticationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.text.ParseException;

@Tag(name = "Authentication API", description = "Endpoints cho Đăng ký, Đăng nhập, Refresh Token, Introspect và Logout")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {

    AuthenticationService authenticationService;

    @Operation(summary = "Đăng ký tài khoản", description = "Tạo tài khoản mới cho người dùng")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserResponse>> signup(@Valid @RequestBody UserCreationRequest request) {
        UserResponse response = authenticationService.createUserAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account created successfully", response));
    }

    @Operation(summary = "Đăng nhập", description = "Xác thực tài khoản và trả về JWT Access Token")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> authenticate(@Valid @RequestBody AuthenticationRequest request) {
        AuthenticationResponse response = authenticationService.authenticate(request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Login successful", response));
    }

    @Operation(summary = "Đăng xuất", description = "Vô hiệu hóa Token hiện tại (Blacklist JWT)")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody LogoutRequest request) throws ParseException, JOSEException {
        authenticationService.logout(request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Logout successful"));
    }

    @Operation(summary = "Làm mới Token (Refresh Token)", description = "Cấp mới Access Token bằng Refresh Token")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> refreshToken(@RequestBody RefreshTokenRequest request)
            throws ParseException, JOSEException {
        AuthenticationResponse response = authenticationService.refreshToken(request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Token refreshed successfully", response));
    }

    @Operation(summary = "Kiểm tra Token (Introspect)", description = "Kiểm tra tính hợp lệ của Token")
    @PostMapping("/introspect")
    public ResponseEntity<ApiResponse<IntrospectResponse>> introspect(@RequestBody IntrospectRequest request)
            throws ParseException, JOSEException {
        IntrospectResponse response = authenticationService.introspect(request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Token introspection result", response));
    }
}
