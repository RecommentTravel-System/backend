package org.example.wayveesystem.service;

import com.nimbusds.jose.JOSEException;
import org.example.wayveesystem.dto.request.*;
import org.example.wayveesystem.dto.response.AuthenticationResponse;
import org.example.wayveesystem.dto.response.IntrospectResponse;
import org.example.wayveesystem.dto.response.UserResponse;

import java.text.ParseException;

public interface AuthenticationService {
    UserResponse createUserAccount(UserCreationRequest request);
    AuthenticationResponse authenticate(AuthenticationRequest request);
    void logout(LogoutRequest request) throws ParseException, JOSEException;
    IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException;
    AuthenticationResponse refreshToken(RefreshTokenRequest request) throws ParseException, JOSEException;
}
