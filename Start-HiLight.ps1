# HiLight Studio - 1-Click PowerShell Launcher for AdbHelper
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  HiLight Studio - 1-Click ADB Renderer Launcher" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adb)) {
    $adbCmd = Get-Command adb -ErrorAction SilentlyContinue
    if ($adbCmd) {
        $adb = "adb"
    } else {
        Write-Host "[ERROR] adb.exe was not found in Android SDK or PATH." -ForegroundColor Red
        exit 1
    }
}

Write-Host "[1/3] Checking connected devices..." -ForegroundColor Yellow
& $adb devices
Write-Host ""

Write-Host "[2/3] Resetting any existing renderers..." -ForegroundColor Yellow
& $adb shell "pkill -f 'com.hilight.(core.AdbHelper|studio:hilight)'" 2>$null

Write-Host "[3/3] Starting HiLight AdbHelper renderer on Pixel 11 Pro..." -ForegroundColor Yellow
& $adb shell 'instance=adb-$(cat /proc/sys/kernel/random/uuid); CLASSPATH=$(pm path com.hilight.studio | head -1 | cut -d: -f2) nohup app_process / com.hilight.core.AdbHelper --owner adb --instance "$instance" --exclusive > /data/local/tmp/hilight.log 2>&1 &'

Start-Sleep -Seconds 2

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  HiLight AdbHelper Status:" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
& $adb shell "cat /data/local/tmp/hilight.log"
Write-Host ""
Write-Host "[DONE] Renderer started! You can now use HiLight Studio on your phone." -ForegroundColor Green
Write-Host ""
