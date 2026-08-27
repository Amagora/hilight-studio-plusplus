@echo off
setlocal enabledelayedexpansion
title Hilight-Studio-PlusPlusV3.5 [v a1.2.5] - Universal Pixel 11 Pro ADB Control

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

:: 2. If not found in standard paths, ask user for explicit consent before downloading
if "%ADB_PATH%"=="" (
    echo ======================================================================
    echo   ADB (Android Debug Bridge) Configuration
    echo ======================================================================
    echo [NOTICE] ADB was not found in your standard Android SDK or PATH.
    echo Hilight-Studio-PlusPlusV3 requires ADB to communicate with your Pixel 11 Pro device.
    echo.
    echo Would you like to download Google's official Android platform-tools (~5 MB) directly
    echo from dl.google.com into this folder?
    echo.
    set /p "USER_CONSENT=[Y] Yes, download from Google  /  [N] No, cancel and set path manually: "
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
            echo [ERROR] Download failed. Please install Android platform-tools manually.
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
echo ======================================================================
echo    Hilight-Studio-PlusPlusV3.5 [v a1.2.5] - Universal Control Manager
echo    Supports: Pixel 11 Pro / Pixel 11 Pro XL / Pixel 11 Pro Fold
echo ======================================================================
echo.
echo Connected Devices:
"%ADB_PATH%" devices
echo.
echo ----------------------------------------------------------------------
echo  Select an option:
echo ----------------------------------------------------------------------
echo  [1] Full Easy Install & Start  (Build/Flash APK, Launch, Start 8-LEDs)
echo  [2] Start Hi-Light             (Fast restart of renderer after reboot)
echo  [3] Stop / Kill ADB Session    (Terminate renderer & release lights HAL)
echo  [4] Exit
echo ----------------------------------------------------------------------
echo.

set "CHOICE="
set /p "CHOICE=Enter choice [1-4]: "

if "%CHOICE%"=="1" goto FULL_INSTALL
if "%CHOICE%"=="2" goto START_RENDERER
if "%CHOICE%"=="3" goto KILL_SESSION
if "%CHOICE%"=="4" goto EXIT_TOOL
goto MENU

:FULL_INSTALL
cls
echo ======================================================================
echo  [1/4] Checking Connected Device & APK
echo ======================================================================
"%ADB_PATH%" wait-for-device

set "APK_PATH=%~dp0app\build\outputs\apk\debug\app-debug.apk"
if not exist "%APK_PATH%" if exist "%~dp0app-debug.apk" set "APK_PATH=%~dp0app-debug.apk"

if not exist "%APK_PATH%" (
    echo Building APK with Gradle...
    call "%~dp0gradlew.bat" assembleDebug
    if !errorlevel! neq 0 (
        echo [ERROR] Build failed.
        pause
        goto MENU
    )
) else (
    echo Found compiled APK: %APK_PATH%
)

echo.
echo ======================================================================
echo  [2/4] Installing APK to Device
echo ======================================================================
"%ADB_PATH%" install -r "%APK_PATH%"
if !errorlevel! neq 0 (
    echo [WARNING] Re-installing with fresh signature...
    "%ADB_PATH%" uninstall com.hilight.studio >nul 2>nul
    "%ADB_PATH%" install -r "%APK_PATH%"
)

echo.
echo ======================================================================
echo  [3/4] Launching HiLight Studio on Device
echo ======================================================================
"%ADB_PATH%" shell am start -n com.hilight.studio/.MainActivity >nul 2>nul
timeout /t 2 >nul

echo.
echo ======================================================================
echo  [4/4] Starting 8-LED Hardware Renderer Daemon
echo ======================================================================
"%ADB_PATH%" shell "pkill -f 'com.hilight.(core.AdbHelper|studio:hilight)'" >nul 2>nul
"%ADB_PATH%" shell "CLASSPATH=$(pm path com.hilight.studio | head -1 | cut -d: -f2) nohup app_process / com.hilight.core.AdbHelper > /data/local/tmp/hilight.log 2>&1 &"
timeout /t 2 >nul

echo --- Renderer Log Output ---
"%ADB_PATH%" shell "cat /data/local/tmp/hilight.log" 2>nul
echo ---------------------------
echo.
echo [SUCCESS] HiLight Studio is running with 8-LED hardware control!
echo.
echo Press any key to return to the menu...
pause >nul
goto MENU

:START_RENDERER
cls
echo ======================================================================
echo  Starting HiLight 8-LED Renderer (Post-Reboot)
echo ======================================================================
echo Resetting any existing renderers...
"%ADB_PATH%" shell "pkill -f 'com.hilight.(core.AdbHelper|studio:hilight)'" >nul 2>nul

echo Starting background AdbHelper daemon...
"%ADB_PATH%" shell "CLASSPATH=$(pm path com.hilight.studio | head -1 | cut -d: -f2) nohup app_process / com.hilight.core.AdbHelper > /data/local/tmp/hilight.log 2>&1 &"
timeout /t 2 >nul

echo.
echo --- Renderer Log Output ---
"%ADB_PATH%" shell "cat /data/local/tmp/hilight.log" 2>nul
echo ---------------------------
echo.
echo [SUCCESS] Renderer daemon started!
echo.
echo Press any key to return to the menu...
pause >nul
goto MENU

:KILL_SESSION
cls
echo ======================================================================
echo  Stopping / Killing Active ADB Session
echo ======================================================================
echo Terminating all HiLight AdbHelper processes...
"%ADB_PATH%" shell "pkill -f 'com.hilight.(core.AdbHelper|studio:hilight)'" >nul 2>nul
echo.
echo [SUCCESS] All HiLight ADB sessions stopped and hardware released.
echo.
echo Press any key to return to the menu...
pause >nul
goto MENU

:EXIT_TOOL
exit /b 0
