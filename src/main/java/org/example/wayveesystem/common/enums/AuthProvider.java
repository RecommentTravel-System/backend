package org.example.wayveesystem.common.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Phương thức xác thực tài khoản: LOCAL (Đăng ký thông thường) hoặc GOOGLE (Đăng nhập bằng Google)")
public enum AuthProvider {
    LOCAL,      // Đăng ký thông thường qua tài khoản / mật khẩu
    REGISTER,   // Đồng nghĩa với LOCAL
    GOOGLE      // Đăng nhập / đăng ký qua Google OAuth / Firebase
}
