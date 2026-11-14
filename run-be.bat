@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo =======================================================
echo   Đang khởi động Backend WayveeSystem (Spring Boot)
echo =======================================================

echo [1/2] Đang nạp các biến môi trường từ .env...
if exist ".env" (
    for /f "usebackq eol=# tokens=1* delims==" %%A in (".env") do (
        set "%%A=%%B"
    )
    echo       Nạp .env thành công!
) else (
    echo       [Cảnh báo] Không tìm thấy file .env!
)

echo [2/2] Đang chạy Spring Boot trên cổng 8080 (http://localhost:8080/wayvee)...
call mvnw.cmd spring-boot:run
pause

