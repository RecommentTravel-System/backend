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
import org.example.wayveesystem.dto.response.ResetOtpResponse;
import org.example.wayveesystem.dto.response.UserResponse;
import org.example.wayveesystem.service.AuthenticationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.text.ParseException;

@Tag(name = "Authentication API", description = "Endpoints cho Đăng ký, Đăng nhập, OTP Email, Refresh Token, Introspect và Logout")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {

    AuthenticationService authenticationService;

    @Operation(summary = "Đăng ký tài khoản", description = "Tạo tài khoản mới và gửi mã OTP qua Email")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody UserCreationRequest request) {
        authenticationService.createUserAccount(request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("OTP verification has been sent to your email"));
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

    @Operation(summary = "Xác thực Email bằng OTP", description = "Xác thực mã OTP gửi về email sau khi đăng ký")
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authenticationService.verifyEmail(request.getEmail(), request.getOtp());
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Email verified successfully"));
    }

    @Operation(summary = "Gửi lại mã OTP", description = "Gửi lại mã OTP kích hoạt tài khoản")
    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<Void>> resendOtp(@Valid @RequestBody OtpRequest request) {
        authenticationService.resendOtp(request.getEmail());
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("OTP has been resent to your email"));
    }

    @Operation(summary = "Thay đổi mật khẩu", description = "Thay đổi mật khẩu tài khoản đang đăng nhập")
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody PasswordUpdateRequest request) {
        authenticationService.changePassword(request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Password changed successfully"));
    }

    @Operation(summary = "Quên mật khẩu", description = "Gửi mã OTP khôi phục mật khẩu qua Email")
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody OtpRequest request) {
        authenticationService.forgotPassword(request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Otp forgot password has been sent to your email"));
    }

    @Operation(summary = "Xác thực OTP đặt lại mật khẩu", description = "Xác thực OTP quên mật khẩu và nhận token đặt lại mật khẩu")
    @PostMapping("/verify-reset-otp")
    public ResponseEntity<ApiResponse<ResetOtpResponse>> verifyResetOtp(@Valid @RequestBody VerifyEmailRequest request) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Otp verify successfully", authenticationService.verifyResetOtp(request)));
    }

    @Operation(summary = "Đặt lại mật khẩu", description = "Đặt lại mật khẩu mới bằng token sau khi xác thực OTP thành công")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) throws ParseException, JOSEException {
        authenticationService.resetPassword(request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Password reset successfully"));
    }
}
