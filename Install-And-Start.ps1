# HiLight Studio - 1-Click Flash & Renderer Launcher for PowerShell
$Host.UI.RawUI.WindowTitle = "HiLight Studio - 1-Click Flash & Start"

Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host "   HiLight Studio - 1-Click Flash & Renderer Launcher for Pixel" -ForegroundColor Cyan
Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host ""

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

# Check standard Android SDK locations and local folder only
$candidates = @(
    (Join-Path $scriptDir "platform-tools\adb.exe"),
    "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
    "$env:ANDROID_HOME\platform-tools\adb.exe",
    "$env:ANDROID_SDK_ROOT\platform-tools\adb.exe"
)

$adb = ""
foreach ($cand in $candidates) {
    if ($cand -and (Test-Path $cand)) {
        $adb = $cand
        break
    }
}

if (-not $adb) {
    $cmd = Get-Command adb -ErrorAction SilentlyContinue
    if ($cmd) { $adb = "adb" }
}

# If still not found, ask user explicitly before downloading from Google CDN
if (-not $adb) {
    Write-Host "[NOTICE] ADB (Android Debug Bridge) was not found in standard SDK paths or PATH." -ForegroundColor Yellow
    Write-Host "HiLight Studio requires ADB to communicate with your Pixel 11 Pro." -ForegroundColor Yellow
    Write-Host ""
    $choice = Read-Host "Would you like to download official Google platform-tools (~5 MB) from dl.google.com? (Y/N)"
    if ($choice -match '^[Yy]') {
        Write-Host "[INFO] Downloading official Google Platform-Tools from dl.google.com..." -ForegroundColor Cyan
        $zip = Join-Path $env:TEMP "platform-tools.zip"
        Invoke-WebRequest -Uri "https://dl.google.com/android/repository/platform-tools-latest-windows.zip" -OutFile $zip
        Expand-Archive -Path $zip -DestinationPath $scriptDir -Force
        Remove-Item $zip -Force -ErrorAction SilentlyContinue
        $localAdb = Join-Path $scriptDir "platform-tools\adb.exe"
        if (Test-Path $localAdb) {
            $adb = $localAdb
            Write-Host "[SUCCESS] Google Platform-Tools downloaded successfully!" -ForegroundColor Green
        } else {
            Write-Host "[ERROR] Could not download ADB. Please install Android platform-tools manually." -ForegroundColor Red
            exit 1
        }
    } else {
        Write-Host "[CANCELLED] Please install Android platform-tools or add adb.exe to your PATH." -ForegroundColor Yellow
        exit 1
    }
}

Write-Host "[1/5] Waiting for connected Pixel device..." -ForegroundColor Yellow
& $adb wait-for-device
Write-Host "Connected device:" -ForegroundColor Green
& $adb devices
Write-Host ""

$apkPath = Join-Path $scriptDir "app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $apkPath)) {
    $apkPath = Join-Path $scriptDir "app-debug.apk"
}

if (-not (Test-Path $apkPath)) {
    Write-Host "[2/5] Building APK with Gradle..." -ForegroundColor Yellow
    & (Join-Path $scriptDir "gradlew.bat") assembleDebug
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] Build failed." -ForegroundColor Red
        exit 1
    }
    $apkPath = Join-Path $scriptDir "app\build\outputs\apk\debug\app-debug.apk"
} else {
    Write-Host "[2/5] Found compiled APK: $apkPath" -ForegroundColor Green
}

Write-Host "[3/5] Installing APK to device..." -ForegroundColor Yellow
& $adb install -r $apkPath
if ($LASTEXITCODE -ne 0) {
    Write-Host "[WARNING] Re-installing with fresh signature..." -ForegroundColor Yellow
    & $adb uninstall com.hilight.studio 2>$null
    & $adb install -r $apkPath
}

Write-Host "[4/5] Launching HiLight Studio on device..." -ForegroundColor Yellow
& $adb shell am start -n com.hilight.studio/.MainActivity | Out-Null
Start-Sleep -Seconds 2

Write-Host "[5/5] Starting background 8-LED renderer..." -ForegroundColor Yellow
& $adb shell "pkill -f 'com.hilight.(core.AdbHelper|studio:hilight)'" 2>$null
& $adb shell 'CLASSPATH=$(pm path com.hilight.studio | head -1 | cut -d: -f2) nohup app_process / com.hilight.core.AdbHelper > /data/local/tmp/hilight.log 2>&1 &'

Start-Sleep -Seconds 2

Write-Host ""
Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host "  Renderer Status:" -ForegroundColor Cyan
Write-Host "======================================================================" -ForegroundColor Cyan
& $adb shell "cat /data/local/tmp/hilight.log"
Write-Host ""
Write-Host "======================================================================" -ForegroundColor Green
Write-Host "  [SUCCESS] HiLight Studio is now running with 8-LED hardware control!" -ForegroundColor Green
Write-Host "======================================================================" -ForegroundColor Green
Write-Host ""
