package org.example.wayveesystem.service;

import com.nimbusds.jose.JOSEException;
import org.example.wayveesystem.dto.request.*;
import org.example.wayveesystem.dto.response.AuthenticationResponse;
import org.example.wayveesystem.dto.response.IntrospectResponse;
import org.example.wayveesystem.dto.response.ResetOtpResponse;
import org.example.wayveesystem.dto.response.UserResponse;

import java.text.ParseException;

public interface AuthenticationService {
    UserResponse createUserAccount(UserCreationRequest request);
    AuthenticationResponse authenticate(AuthenticationRequest request);
    void logout(LogoutRequest request) throws ParseException, JOSEException;
    IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException;
    AuthenticationResponse refreshToken(RefreshTokenRequest request) throws ParseException, JOSEException;
    void verifyEmail(String email, String otp);
    void resendOtp(String email);
    void changePassword(PasswordUpdateRequest request);
    void forgotPassword(OtpRequest request);
    ResetOtpResponse verifyResetOtp(VerifyEmailRequest request);
    void resetPassword(ResetPasswordRequest request) throws ParseException, JOSEException;
}
