# ==============================================================================
# Script khởi chạy Backend Wayvee (Spring Boot)
# Tự động nạp biến môi trường từ file .env và chạy Spring Boot
# ==============================================================================

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

Write-Host ">>> Đang nạp cấu hình từ .env..." -ForegroundColor Cyan

if (Test-Path ".env") {
    Get-Content ".env" | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
            $parts = $line.Split("=", 2)
            $name = $parts[0].Trim()
            $value = $parts[1].Trim().Trim('"').Trim("'")
            [System.Environment]::SetEnvironmentVariable($name, $value, "Process")
        }
    }
    Write-Host ">>> Đã nạp biến môi trường thành công!" -ForegroundColor Green
} else {
    Write-Host ">>> Cảnh báo: Không tìm thấy file .env!" -ForegroundColor Yellow
}

Write-Host ">>> Đang khởi động Backend WayveeSystem trên cổng 8080..." -ForegroundColor Cyan
if (Test-Path "mvnw.cmd") {
    .\mvnw.cmd spring-boot:run
} else {
    mvn spring-boot:run
}

