@echo off
setlocal enabledelayedexpansion
title hilight-studio-plusplus [v a1.1.0] - Universal Pixel 11 Pro ADB Control

:: 1. Check standard SDK locations and local folder only
set "ADB_PATH="
if exist "%~dp0platform-tools\adb.exe" set "ADB_PATH=%~dp0platform-tools\adb.exe"
if "%ADB_PATH%"=="" if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" set "ADB_PATH=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
if "%ADB_PATH%"=="" if defined ANDROID_HOME if exist "%ANDROID_HOME%\platform-tools\adb.exe" set "ADB_PATH=%ANDROID_HOME%\platform-tools\adb.exe"
if "%ADB_PATH%"=="" if defined ANDROID_SDK_ROOT if exist "%ANDROID_SDK_ROOT%\platform-tools\adb.exe" set "ADB_PATH=%ANDROID_SDK_ROOT%\platform-tools\adb.exe"

if "%ADB_PATH%"=="" (
    where adb >nul 2>nul
    if !errorlevel! equ 0 set "ADB_PATH=adb"
)

if "%ADB_PATH%"=="" (
    echo ============================================================
    echo   ADB (Android Debug Bridge) Configuration
    echo ============================================================
    echo [NOTICE] ADB was not found in your standard Android SDK or PATH.
    echo.
    echo Would you like to download Google's official Android platform-tools (~5 MB) directly
    echo from dl.google.com into this folder?
    echo.
    set /p "USER_CONSENT=[Y] Yes, download from Google  /  [N] No, cancel: "
    if /i "!USER_CONSENT!"=="Y" (
        echo.
        echo [INFO] Downloading official Google Platform-Tools from dl.google.com...
        powershell -NoProfile -ExecutionPolicy Bypass -Command ^
            "$ProgressPreference = 'SilentlyContinue'; " ^
            "$zip = Join-Path $env:TEMP 'platform-tools.zip'; " ^
            "Invoke-WebRequest -Uri 'https://dl.google.com/android/repository/platform-tools-latest-windows.zip' -OutFile $zip; " ^
            "Expand-Archive -Path $zip -DestinationPath '%~dp0' -Force; " ^
            "Remove-Item $zip -Force"
        if exist "%~dp0platform-tools\adb.exe" (
            set "ADB_PATH=%~dp0platform-tools\adb.exe"
            echo [SUCCESS] Google Platform-Tools downloaded successfully!
            echo.
        ) else (
            echo [ERROR] Download failed.
            pause
            exit /b 1
        )
    ) else (
        echo.
        echo [CANCELLED] Please install Android platform-tools or add adb.exe to your PATH.
        pause
        exit /b 1
    )
)

:MENU
cls
echo ============================================================
echo   hilight-studio-plusplus [v a1.1.0] - Universal Control Launcher
echo   Supports: Pixel 11 Pro / Pixel 11 Pro XL / Pixel 11 Pro Fold
echo ============================================================
echo.
echo Connected Devices:
"%ADB_PATH%" devices
echo.
echo ------------------------------------------------------------
echo  Select an option:
echo ------------------------------------------------------------
echo  [1] Start HiLight (Restart 8-LED renderer after reboot)
echo  [2] Stop / Kill ADB Session (Terminate renderer)
echo  [3] Full Easy Install & Start (Re-flash APK + Start)
echo  [4] Exit
echo ------------------------------------------------------------
echo.

set "CHOICE="
set /p "CHOICE=Enter choice [1-4]: "

if "%CHOICE%"=="1" goto START_RENDERER
if "%CHOICE%"=="2" goto KILL_SESSION
if "%CHOICE%"=="3" goto FULL_INSTALL
if "%CHOICE%"=="4" goto EXIT_TOOL
goto MENU

:START_RENDERER
cls
echo [1/2] Resetting existing renderers...
"%ADB_PATH%" shell "pkill -f 'com.hilight.(core.AdbHelper|studio:hilight)'" >nul 2>nul

echo [2/2] Starting HiLight AdbHelper renderer on Pixel...
"%ADB_PATH%" shell "CLASSPATH=$(pm path com.hilight.studio | head -1 | cut -d: -f2) nohup app_process / com.hilight.core.AdbHelper > /data/local/tmp/hilight.log 2>&1 &"
timeout /t 2 >nul

echo.
echo --- Renderer Status ---
"%ADB_PATH%" shell "cat /data/local/tmp/hilight.log" 2>nul
echo -----------------------
echo.
echo [SUCCESS] Renderer daemon is running!
echo.
echo Press any key to return to the menu...
pause >nul
goto MENU

:KILL_SESSION
cls
echo Stopping HiLight ADB session...
"%ADB_PATH%" shell "pkill -f 'com.hilight.(core.AdbHelper|studio:hilight)'" >nul 2>nul
echo.
echo [SUCCESS] All HiLight ADB sessions stopped and hardware released.
echo.
echo Press any key to return to the menu...
pause >nul
goto MENU

:FULL_INSTALL
cls
echo [1/3] Waiting for connected Pixel device...
"%ADB_PATH%" wait-for-device

set "APK_PATH=%~dp0app\build\outputs\apk\debug\app-debug.apk"
if not exist "%APK_PATH%" if exist "%~dp0app-debug.apk" set "APK_PATH=%~dp0app-debug.apk"

if not exist "%APK_PATH%" (
    echo Building APK with Gradle...
    call "%~dp0gradlew.bat" assembleDebug
)
echo [2/3] Installing APK to device...
"%ADB_PATH%" install -r "%APK_PATH%"

echo [3/3] Launching app and starting renderer...
"%ADB_PATH%" shell am start -n com.hilight.studio/.MainActivity >nul 2>nul
timeout /t 2 >nul
"%ADB_PATH%" shell "pkill -f 'com.hilight.(core.AdbHelper|studio:hilight)'" >nul 2>nul
"%ADB_PATH%" shell "CLASSPATH=$(pm path com.hilight.studio | head -1 | cut -d: -f2) nohup app_process / com.hilight.core.AdbHelper > /data/local/tmp/hilight.log 2>&1 &"
timeout /t 2 >nul

echo --- Renderer Status ---
"%ADB_PATH%" shell "cat /data/local/tmp/hilight.log" 2>nul
echo -----------------------
echo.
echo [SUCCESS] Installed & running!
echo.
echo Press any key to return to the menu...
pause >nul
goto MENU

:EXIT_TOOL
exit /b 0
