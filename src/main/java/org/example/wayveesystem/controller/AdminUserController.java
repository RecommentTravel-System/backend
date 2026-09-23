package org.example.wayveesystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.wayveesystem.common.response.ApiResponse;
import org.example.wayveesystem.dto.request.AdminUserUpdateRequest;
import org.example.wayveesystem.dto.request.UserCreationRequest;
import org.example.wayveesystem.dto.response.UserResponse;
import org.example.wayveesystem.service.AdminUserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin User API", description = "Endpoints quản trị người dùng dành cho Admin")
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@CrossOrigin(origins = "*")
public class AdminUserController {

    AdminUserService adminUserService;

    @Operation(summary = "Lấy danh sách tất cả người dùng", description = "Trả về toàn bộ người dùng trong hệ thống (chỉ Admin)")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        List<UserResponse> responses = adminUserService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", responses));
    }

    @Operation(summary = "Lấy thông tin người dùng theo ID", description = "Lấy chi tiết người dùng theo userId")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", adminUserService.getUserById(id)));
    }

    @Operation(summary = "Tạo tài khoản người dùng mới từ Admin", description = "Tạo người dùng với trạng thái ACTIVE")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody UserCreationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User created successfully", adminUserService.createUser(request)));
    }

    @Operation(summary = "Cập nhật thông tin người dùng", description = "Cập nhật tên, sđt, trạng thái, quyền hoặc mật khẩu")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable("id") Long id,
            @RequestBody AdminUserUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", adminUserService.updateUser(id, request)));
    }

    @Operation(summary = "Xóa người dùng", description = "Xóa tài khoản người dùng theo userId")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable("id") Long id) {
        adminUserService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully"));
    }
}
