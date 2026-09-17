package org.example.wayveesystem.service.impl;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.example.wayveesystem.common.enums.Role;
import org.example.wayveesystem.common.enums.UserStatus;
import org.example.wayveesystem.common.exception.AppException;
import org.example.wayveesystem.common.exception.ErrorCode;
import org.example.wayveesystem.dto.request.*;
import org.example.wayveesystem.dto.response.AuthenticationResponse;
import org.example.wayveesystem.dto.response.IntrospectResponse;
import org.example.wayveesystem.dto.response.ResetOtpResponse;
import org.example.wayveesystem.dto.response.UserResponse;
import org.example.wayveesystem.model.InvalidatedToken;
import org.example.wayveesystem.model.User;
import org.example.wayveesystem.mapper.UserMapper;
import org.example.wayveesystem.respository.InvalidatedTokenRepository;
import org.example.wayveesystem.respository.UserRepository;
import org.example.wayveesystem.service.AuthenticationService;
import org.example.wayveesystem.service.EmailService;
import org.example.wayveesystem.service.OtpService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationServiceImpl implements AuthenticationService {

    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    UserMapper userMapper;
    InvalidatedTokenRepository invalidatedTokenRepository;
    OtpService otpService;
    EmailService emailService;

    @NonFinal
    @Value("${jwt.signerKey}")
    protected String SIGNER_KEY;

    private void validateUserCreation(UserCreationRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_EXISTED);
        }
    }

    private void validateUserStatus(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            if (user.getStatus() == UserStatus.PENDING) {
                throw new AppException(ErrorCode.USER_NOT_VERIFIED);
            } else if (user.getStatus() == UserStatus.INACTIVE) {
                throw new AppException(ErrorCode.USER_INACTIVE);
            } else if (user.getStatus() == UserStatus.SUSPENDED) {
                throw new AppException(ErrorCode.USER_SUSPENDED);
            } else if (user.getStatus() == UserStatus.DELETED) {
                throw new AppException(ErrorCode.USER_DELETED);
            }
        }
    }

    @Override
    public UserResponse createUserAccount(UserCreationRequest request) {
        validateUserCreation(request);
        User user = userMapper.toUser(request);
        user.setStatus(UserStatus.PENDING);
        user.setRole(Role.USER);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        User savedUser = userRepository.save(user);

        String otp = otpService.generateOtp(savedUser.getEmail());
        emailService.sendVerifyEmailOtp(savedUser.getEmail(), otp);

        return userMapper.toUserResponse(savedUser);
    }

    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        validateUserStatus(user);
        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPassword());

        if (!authenticated) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        var accessToken = generateAccessToken(user);
        var refreshToken = generateRefreshToken(user);
        return AuthenticationResponse.builder()
                .authenticated(true)
                .userResponse(userMapper.toUserResponse(user))
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public void verifyEmail(String email, String otp) {
        otpService.verifyOtp(email, otp);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_VERIFIED);
        }

        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
    }

    @Override
    public void resendOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_VERIFIED);
        }

        String otp = otpService.resendOtp(email);
        emailService.sendVerifyEmailOtp(email, otp);
    }

    @Override
    public void forgotPassword(OtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        String otp = otpService.generateOtp(user.getEmail());
        emailService.sendForgotPasswordOtp(user.getEmail(), otp);
    }

    @Override
    public ResetOtpResponse verifyResetOtp(VerifyEmailRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        otpService.verifyOtp(request.getEmail(), request.getOtp());
        String resetToken = generateResetPasswordToken(user);
        return ResetOtpResponse.builder()
                .resetToken(resetToken)
                .build();
    }

    private void validateNewPassword(String newPassword, String confirmPassword, String currentPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new AppException(ErrorCode.PASSWORD_CONFIRM_NOT_MATCH);
        }

        if (passwordEncoder.matches(newPassword, currentPassword)) {
            throw new AppException(ErrorCode.PASSWORD_SAME_AS_OLD);
        }
    }

    @Override
    public void changePassword(PasswordUpdateRequest request) {
        String userIdStr = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findById(Long.valueOf(userIdStr))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.PASSWORD_INCORRECT);
        }

        validateNewPassword(request.getNewPassword(), request.getConfirmNewPassword(), user.getPassword());
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) throws ParseException, JOSEException {
        SignedJWT signedJWT = verifyResetPasswordToken(request.getResetToken());
        String userIdStr;

        try {
            userIdStr = signedJWT.getJWTClaimsSet().getSubject();
        } catch (ParseException e) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }

        User user = userRepository.findById(Long.valueOf(userIdStr))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        validateNewPassword(request.getNewPassword(), request.getConfirmNewPassword(), user.getPassword());
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        String jwtTokenId = signedJWT.getJWTClaimsSet().getJWTID();
        Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
        InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                .id(jwtTokenId)
                .expiryTime(expirationTime)
                .build();

        invalidatedTokenRepository.save(invalidatedToken);
    }

    @Override
    public void logout(LogoutRequest request) throws ParseException, JOSEException {
        try {
            var signedToken = verifyToken(request.getToken());

            String jwtTokenId = signedToken.getJWTClaimsSet().getJWTID();
            Date expirationTime = signedToken.getJWTClaimsSet().getExpirationTime();

            InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                    .id(jwtTokenId)
                    .expiryTime(expirationTime)
                    .build();

            invalidatedTokenRepository.save(invalidatedToken);
        } catch (AppException e) {
            log.info("Token already expired or invalid");
        }
    }

    @Override
    public IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException {
        var token = request.getToken();
        boolean isValid = true;

        try {
            verifyToken(token);
        } catch (AppException e) {
            isValid = false;
        }

        return IntrospectResponse.builder().valid(isValid).build();
    }

    @Override
    public AuthenticationResponse refreshToken(RefreshTokenRequest request) throws ParseException, JOSEException {
        var signedJWT = verifyToken(request.getRefreshToken());
        var jwtId = signedJWT.getJWTClaimsSet().getJWTID();
        var expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();

        InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                .id(jwtId)
                .expiryTime(expirationTime)
                .build();

        invalidatedTokenRepository.save(invalidatedToken);

        var userIdStr = signedJWT.getJWTClaimsSet().getSubject();
        var user = userRepository.findById(Long.valueOf(userIdStr))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        var newAccessToken = generateAccessToken(user);
        var newRefreshToken = generateRefreshToken(user);

        return AuthenticationResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .authenticated(true)
                .userResponse(userMapper.toUserResponse(user))
                .build();
    }

    private String generateAccessToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(String.valueOf(user.getUserId()))
                .issuer("wayveesystem.com")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(60, ChronoUnit.MINUTES).toEpochMilli()
                ))
                .jwtID(UUID.randomUUID().toString())
                .claim("scope", user.getRole() != null ? "ROLE_" + user.getRole().name() : "ROLE_USER")
                .build();

        Payload payload = new Payload(claims.toJSONObject());
        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
        } catch (JOSEException e) {
            log.error("Cannot create access token", e);
            throw new RuntimeException(e);
        }
        return jwsObject.serialize();
    }

    private String generateRefreshToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(String.valueOf(user.getUserId()))
                .issuer("wayveesystem.com")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(7, ChronoUnit.DAYS).toEpochMilli()
                ))
                .jwtID(UUID.randomUUID().toString())
                .claim("scope", "REFRESH_TOKEN")
                .build();

        JWSObject jwsObject = new JWSObject(header, new Payload(claims.toJSONObject()));

        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
        } catch (JOSEException e) {
            log.error("Cannot create refresh token", e);
            throw new RuntimeException(e);
        }
        return jwsObject.serialize();
    }

    private String generateResetPasswordToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(String.valueOf(user.getUserId()))
                .issuer("wayveesystem.com")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(15, ChronoUnit.MINUTES).toEpochMilli()
                ))
                .jwtID(UUID.randomUUID().toString())
                .claim("scope", "RESET_PASSWORD")
                .build();

        JWSObject jwsObject = new JWSObject(header, new Payload(claims.toJSONObject()));

        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
        } catch (JOSEException e) {
            log.error("Cannot create reset password token", e);
            throw new RuntimeException(e);
        }
        return jwsObject.serialize();
    }

    private SignedJWT verifyToken(String token) throws JOSEException, ParseException {
        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());
        SignedJWT signedJWT = SignedJWT.parse(token);

        Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
        var verified = signedJWT.verify(verifier);

        if (!verified || expirationTime.before(new Date())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        if (invalidatedTokenRepository.existsById(signedJWT.getJWTClaimsSet().getJWTID())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        return signedJWT;
    }

    private SignedJWT verifyResetPasswordToken(String token) throws ParseException, JOSEException {
        SignedJWT signedJWT = verifyToken(token);

        try {
            String scope = signedJWT.getJWTClaimsSet().getStringClaim("scope");
            if (!"RESET_PASSWORD".equals(scope)) {
                log.warn("Reset password token invalid scope: {}", scope);
                throw new AppException(ErrorCode.INVALID_TOKEN);
            }

            return signedJWT;

        } catch (ParseException e) {
            log.warn("Invalid token: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }
    }
}
