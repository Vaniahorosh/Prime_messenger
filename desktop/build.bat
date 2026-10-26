@echo off
echo ==============================================
echo   Building Prime Desktop (Windows x64)
echo ==============================================

where dotnet >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] .NET 8 SDK not found.
    echo Please install .NET 8 SDK from: https://dotnet.microsoft.com/download/dotnet/8.0
    pause
    exit /b 1
)

cd /d "%~dp0\PrimeDesktop"
echo Restoring packages and building Release...
dotnet publish -c Release -r win-x64 --self-contained false -o ..\bin\Release

if %ERRORLEVEL% equ 0 (
    echo.
    echo [SUCCESS] Build completed!
    echo Output directory: %~dp0bin\Release\PrimeDesktop.exe
) else (
    echo [ERROR] Build failed.
)

pause
