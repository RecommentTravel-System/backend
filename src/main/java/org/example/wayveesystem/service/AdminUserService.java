package org.example.wayveesystem.service;

import org.example.wayveesystem.dto.request.AdminUserUpdateRequest;
import org.example.wayveesystem.dto.request.UserCreationRequest;
import org.example.wayveesystem.dto.response.UserResponse;

import java.util.List;

public interface AdminUserService {
    List<UserResponse> getAllUsers();
    UserResponse getUserById(Long userId);
    UserResponse createUser(UserCreationRequest request);
    UserResponse updateUser(Long userId, AdminUserUpdateRequest request);
    void deleteUser(Long userId);
}
