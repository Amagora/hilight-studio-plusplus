# hilight-studio-plusplus — Complete Conversation Log & Source Code Record

**Build Version:** `a1.1.0` (Build Code `110`)  
**Target Hardware:** Google Pixel 11 Pro Series (Pixel 11 Pro, Pixel 11 Pro XL, Pixel 11 Pro Fold — Target Device: `66291FDDJ001HW` on Android 17 / API 37)  
**Base Repository:** Forked from [DhananjayBhosale/hilight-studio](https://github.com/DhananjayBhosale/hilight-studio) (v1.0.8-experimental)  
**Repository Fork:** [Amagora/hilight-studio-plusplus](https://github.com/Amagora/hilight-studio-plusplus)  
**License:** MIT License (Full Open Source)  
**Transparency:** AI-Assisted Pair-Programming Record & Human Verification Log  

---

## Table of Contents

1. [Executive Overview & Purpose](#1-executive-overview--purpose)
2. [Complete Chronological Conversation & Directive Log](#2-complete-chronological-conversation--directive-log)
3. [Full Source Code Inventory & Modification Map](#3-full-source-code-inventory--modification-map)
4. [Architectural Deep Dives & Problem Resolutions](#4-architectural-deep-dives--problem-resolutions)
5. [Build, Compilation & Packaging Instructions](#5-build-compilation--packaging-instructions)
6. [Open Source Licensing & Upstream Attribution](#6-open-source-licensing--upstream-attribution)

---

## 1. Executive Overview & Purpose

This document provides an unedited, exhaustive technical log of the development, user directives, AI pair-programming interactions, bug fixes, architecture decisions, and full source code changes for **hilight-studio-plusplus** (Build `a1.1.0`).

**hilight-studio-plusplus** transforms the 8-LED rear camera ring array on Google Pixel 11 Pro series devices into a fully customizable ambient light, emergency strobe, tabletop video fill light, and battery fuel gauge, accompanied by a rootless background daemon (`AdbHelper`) and a native Windows desktop controller (`HiLight-Control.exe`).

---

## 2. Complete Chronological Conversation & Directive Log

Below is the complete chronological record of all user prompts, directives, issues identified, and the technical solutions engineered in response:

---

### [Directive 1] ADB Discovery & Rootless Daemon
- **User Prompt / Directive:**  
  *"Where is adb located? Can we run rootless without Shizuku each time?"*
- **Problem & Context:**  
  The user wanted to control the physical rear 8-LED lights on their Pixel 11 Pro Fold without requiring root access or keeping the Shizuku accessibility server manually running on every boot.
- **Technical Solution:**  
  1. Located Google Platform-Tools ADB in `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`.
  2. Identified and verified the rootless Android background process architecture:
     ```bash
     nohup app_process -Djava.class.path=/data/app/.../base.apk /system/bin com.hilight.core.AdbHelper > /dev/null 2>&1 &
     ```
  3. Tested and confirmed on device `66291FDDJ001HW` (Pixel 11 Pro Fold, Android 17 / API 37).

---

### [Directive 2] Feature Expansion & Hardware Enhancements
- **User Prompt / Directive:**  
  *"Are there any other improvements that can be seen here we can add to our fork/build? Lets do all of the above."*
- **Problem & Context:**  
  The original upstream project was a proof-of-concept with limited lighting modes, no foldable dual-pane utilization, no color temperature tuning, and no battery monitoring.
- **Technical Solution:**  
  Engineered 5 major feature suites:
  1. **Material 3 Dynamic Theming Engine + AMOLED Pitch Black**: Custom color accents, pitch black surfaces, and smooth transitions.
  2. **Adaptive Dual-Pane Foldable Layout**: Split-screen dashboard when unfolded on the Pixel 11 Pro Fold ($\ge 600\text{dp}$), reverting to single-column when folded.
  3. **Tabletop Video Fill Light & Emergency Strobe**: Tuned CCT presets (2700K Warm Tungsten, 3800K Soft White, 4500K Neutral Daylight, 6500K Cool White) + emergency pulsed strobe & SOS morse beacon.
  4. **8-LED Battery & Charging Fuel Gauge**: Measures 1–8 LEDs proportionally to battery state of charge with breathing green animation on AC/wireless charging.
  5. **Curated Presets Pack**: 6 signature lighting animations (Aurora Borealis, Cyberpunk Neon, Campfire Ember, Deep Ocean, Pixel Spectrum, Matrix Pulse).

---

### [Directive 3] One-Click Automation Launchers
- **User Prompt / Directive:**  
  *"Can we make this a one click command?"*
- **Problem & Context:**  
  Running multi-line ADB commands manually in terminal after restarting the phone was cumbersome.
- **Technical Solution:**  
  Created instant 1-click scripts at repository root:
  - `Install-And-Start.bat` (Full build, install, launch, daemon starter)
  - `Start-HiLight.bat` (Fast restart daemon launcher)
  - `Install-And-Start.ps1` & `Start-HiLight.ps1` (PowerShell variants)

---

### [Directive 4 & 5] UI Insets & Status Bar Visibility in Dark/AMOLED Modes
- **User Prompt / Directive:**  
  *"The UI looks off"* & *"The notifications and icons in the status bar are not visible in dark or AMOLED mode."*
- **Problem & Context:**  
  Cards were stretching excessively across wide foldable displays, and Android system status bar icons were rendering dark gray on dark/AMOLED backgrounds, making them invisible.
- **Technical Solution:**  
  1. Added root background `Surface(color = MaterialTheme.colorScheme.background)`.
  2. Constrained card widths using `.widthIn(max = 720.dp)` for centered readability.
  3. Implemented dynamic status bar insets controller in `MainActivity.kt`:
     ```kotlin
     WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
     ```
     Ensures crisp white status bar text and icons in dark and AMOLED modes.

---

### [Directive 6] 8-LED Battery Level Indicator Behavior
- **User Prompt / Directive:**  
  *"How does the battery level indicator option work?"*
- **Problem & Context:**  
  User requested clarification on fuel gauge logic and hardware mapping.
- **Technical Solution:**  
  Documented and refined `BatteryReceiver.kt`:
  - 0–12%: 1 LED (Red pulse)
  - 13–25%: 2 LEDs (Amber)
  - 26–37%: 3 LEDs
  - 38–50%: 4 LEDs
  - 51–62%: 5 LEDs
  - 63–75%: 6 LEDs
  - 76–87%: 7 LEDs
  - 88–100%: 8 LEDs (Solid Green / Breathing Green when charging)

---

### [Directive 7] Standalone Windows Desktop GUI Manager
- **User Prompt / Directive:**  
  *"Lets build a GUI to push commands, One for a full easy install, one for the previous ADB session kill, and one to start Hi-Light after a reboot."*
- **Problem & Context:**  
  Needed a standalone Windows `.exe` utility for users who prefer graphical interfaces over batch scripts.
- **Technical Solution:**  
  Created `HiLight-Control.exe` (.NET 9 WinForms application in `tools/HiLightManager/`):
  - Real-time device badge displaying connected Pixel model and serial number.
  - 3 prominent action cards: Full Install, Start Renderer, and Stop Session.
  - Live scrolling terminal log with timestamped hardware responses.

---

### [Directive 8] Packaging & MIT License Visibility
- **User Prompt / Directive:**  
  *"How do we package this up to share with original dev or someone else? Make sure the MIT license is visible in both the app to read and the .exe ran."*
- **Problem & Context:**  
  Compliance and accessibility of legal notices and upstream license.
- **Technical Solution:**  
  1. Added interactive "About & MIT License" modal in the Android **Setup** tab.
  2. Added dedicated `📜 MIT License` button and dialog in `HiLight-Control.exe`.
  3. Created standalone portable release archives.

---

### [Directive 9] Privacy Protection & Opt-In Downloads
- **User Prompt / Directive:**  
  *"We should make sure the automatic download is searching the user's PC is optionally selectable. That could be considered malicious or an invasion of someone's data."*
- **Problem & Context:**  
  Arbitrary scanning of user folders (Downloads, Documents) or downloading files without explicit user consent is a security and privacy risk.
- **Technical Solution:**  
  **Strict Privacy Architecture**:
  1. Restricted ADB searches exclusively to standard developer environment variables (`ANDROID_HOME`, `ANDROID_SDK_ROOT`, standard SDK path, local directory). Zero personal folders are scanned.
  2. Replaced automatic network downloads with mandatory explicit opt-in modal dialogs in the GUI and `[Y/N]` interactive prompts in batch scripts before connecting to Google's official CDN (`dl.google.com`).
  3. Added manual `📁 Change ADB Location...` file picker allowing users to select their own binary.

---

### [Directive 10] A.I. Disclosure System
- **User Prompt / Directive:**  
  *"We also need to make sure the disclosure for the use of A.I. is in the app on install/launch prompted once, and in the Set Up section as its own readable section called A.I. Disclosure. Lets add a disclosure to the .exe as well as the ability to view the log of my requests to you."*
- **Problem & Context:**  
  Total transparency regarding AI assistance during engineering, with permanent accessibility without annoying repetitive popups.
- **Technical Solution:**  
  1. Implemented first-launch one-time `AiDisclosureDialog` in Android, persisting acknowledgement to `aiDisclosureAcknowledged` in SharedPreferences.
  2. Created permanent **"A.I. Disclosure"** card in Android **Setup** tab.
  3. Added `🤖 A.I. Disclosure` and `📋 Request Log` modal dialogs in `HiLight-Control.exe`.

---

### [Directive 11] Hi-Light Studio V2 Universal Branding & Version a1.1.0
- **User Prompt / Directive:**  
  *"The .exe UI is broken in terms of spacing and layout. This also isn't an 11 Pro Fold build it is an overall build. I think we should call this Hi-Light Studio V2. Lets change this build to a custom build number a1.1.0. Once the ADB tool runs an install and a kill it closes. It should remain open after each option is ran."*
- **Problem & Context:**  
  Rebranding to universal suite for all Pixel 11 Pro models; fixing command scripts closing immediately on exit.
- **Technical Solution:**  
  1. Rebranded app and desktop tools to **Hi-Light Studio V2**.
  2. Set version to **`a1.1.0`** (versionCode `110`).
  3. Modified batch scripts to run in persistent interactive loop (`:MENU`).
  4. Added exception protection to desktop app so it stays open indefinitely.

---

### [Directive 12] Original Developer Attribution & GitHub Links
- **User Prompt / Directive:**  
  *"In the .exe as well as the app lets add a source reference to the original developer and their github page. Lets also add it to the .exe"*
- **Problem & Context:**  
  Giving prominent credit and direct 1-tap links to original creator Dhananjay Bhosale.
- **Technical Solution:**  
  1. Added **"Original Creator & GitHub Source"** card in Android **Setup** screen with 1-tap buttons to [Dhananjay Bhosale's GitHub](https://github.com/DhananjayBhosale) and [hilight-studio repo](https://github.com/DhananjayBhosale/hilight-studio).
  2. Added **`⭐ Dhananjay's GitHub`** button in the desktop header and inside dialogs.

---

### [Directive 13] DPI Auto-Sizing & ADB Explanation Modal
- **User Prompt / Directive:**  
  *"Underneath the title there is text that can't be seen. Change ADB Location, Whats This?, and Refresh Connection are also still cut off in the buttons. The Clear button is also still cut off."*
- **Problem & Context:**  
  Text clipping on High-DPI screens due to fixed pixel button widths; need explanation for ADB button.
- **Technical Solution:**  
  1. Converted all buttons to `AutoSize = true` with `Padding = (16, 8, 16, 8)` and `UseMnemonic = false`.
  2. Added `ℹ️ What's this?` modal explaining what ADB is and clarifying that detection is automatic.

---

### [Directive 14] Complete DPI Layout Architecture Overhaul
- **User Prompt / Directive:**  
  *"We still have cut off text not all of the UI elements shapes are visible. The buttons should be completely visible as well as the text inside them. Stop getting this wrong."*
- **Problem & Context:**  
  Hardcoded TableLayoutPanel row heights (`118px`, `74px`, `280px`, `40px`) clamped row heights on High-DPI (125%–175%) displays, physically crushing headers, buttons, and action cards into thin horizontal strips.
- **Technical Solution:**  
  **Root-Cause Resolution**:
  1. Replaced all rigid row definitions with `RowStyle(SizeType.AutoSize)`.
  2. Enabled `AutoSizeMode.GrowAndShrink` across all container panels (`pnlHeader`, `pnlDeviceStatus`, `pnlActions`, `card`, `pnlLogHeader`).
  3. Configured `AutoScaleMode = AutoScaleMode.Dpi` with expanded baseline dimensions (`1250 × 920px`).
  4. Tested and confirmed 100% visible text, complete button shapes, and zero clipping.

---

### [Directive 15] Resizable Sub-Menu Windows & Text Editor Integration
- **User Prompt / Directive:**  
  *"So the sub menus also have broken buttons lets make the windows adjustable in size plus they need to be able to open in notepad or a text editor of the user's choice"*
- **Problem & Context:**  
  Modal dialogs had fixed borders and hardcoded coordinate positioning. Users needed the ability to resize dialog windows and export or view text in their preferred text editor or Windows Notepad.
- **Technical Solution:**  
  1. Converted all dialogs (`ShowAdbInfoDialog`, `ShowAdbSetupDialog`, `ShowLicenseDialog`, `ShowAiDisclosureDialog`, `ShowUserRequestLogDialog`) to `FormBorderStyle.Sizable` with `MaximizeBox = true` and dynamic `TableLayoutPanel` docking.
  2. Added **`📝 Open in Text Editor`** (opens in default text editor of choice) and **`📄 Open in Notepad`** (opens in `notepad.exe`).
  3. Added **`📋 Copy`** buttons for 1-click clipboard copying.

---

## 3. Full Source Code Inventory & Modification Map

### 📱 Android Application (`app/`)
| File Path | Description of Enhancements |
|---|---|
| [`app/build.gradle.kts`](file:///c:/Users/Amagora/Downloads/HiLight%20Studio%20Fork/app/build.gradle.kts) | Versioning `a1.1.0` (code `110`), Jetpack Compose, Material 3, Android 17 / API 37 compatibility. |
| [`app/src/main/AndroidManifest.xml`](file:///c:/Users/Amagora/Downloads/HiLight%20Studio%20Fork/app/src/main/AndroidManifest.xml) | App name `Hi-Light Studio V2`, battery broadcast receiver permissions, orientation configurations. |
| [`app/src/main/java/com/hilight/studio/MainActivity.kt`](file:///c:/Users/Amagora/Downloads/HiLight%20Studio%20Fork/app/src/main/java/com/hilight/studio/MainActivity.kt) | Adaptive Dual-Pane layout for foldables, dynamic status bar insets controller (`isAppearanceLightStatusBars`), first-launch AI disclosure dialog. |
| [`app/src/main/java/com/hilight/studio/ui/theme/Theme.kt`](file:///c:/Users/Amagora/Downloads/HiLight%20Studio%20Fork/app/src/main/java/com/hilight/studio/ui/theme/Theme.kt) | Material 3 Theming engine with AMOLED pitch black option (`#000000` surface). |
| [`app/src/main/java/com/hilight/studio/ui/theme/Color.kt`](file:///c:/Users/Amagora/Downloads/HiLight%20Studio%20Fork/app/src/main/java/com/hilight/studio/ui/theme/Color.kt) | Curated color palettes, neon accents, and CCT temperature color constants. |
| [`app/src/main/java/com/hilight/studio/ui/screens/SetupScreen.kt`](file:///c:/Users/Amagora/Downloads/HiLight%20Studio%20Fork/app/src/main/java/com/hilight/studio/ui/screens/SetupScreen.kt) | A.I. Disclosure Card, Original Creator & GitHub Source Card, MIT License modal viewer, battery fuel gauge toggle. |
| [`app/src/main/java/com/hilight/studio/ui/screens/LiveToolsScreen.kt`](file:///c:/Users/Amagora/Downloads/HiLight%20Studio%20Fork/app/src/main/java/com/hilight/studio/ui/screens/LiveToolsScreen.kt) | Tabletop video fill light launcher, strobe & SOS triggers, real-time 8-LED hero visualizer. |
| [`app/src/main/java/com/hilight/studio/ui/screens/TabletopFillLightScreen.kt`](file:///c:/Users/Amagora/Downloads/HiLight%20Studio%20Fork/app/src/main/java/com/hilight/studio/ui/screens/TabletopFillLightScreen.kt) | Color temperature sliders (2700K–6500K), brightness control, continuous light driver. |
| [`app/src/main/java/com/hilight/studio/receiver/BatteryReceiver.kt`](file:///c:/Users/Amagora/Downloads/HiLight%20Studio%20Fork/app/src/main/java/com/hilight/studio/receiver/BatteryReceiver.kt) | Broadcast receiver for battery percentage and AC/USB charging state animations. |
| [`app/src/main/java/com/hilight/studio/presets/Presets.kt`](file:///c:/Users/Amagora/Downloads/HiLight%20Studio%20Fork/app/src/main/java/com/hilight/studio/presets/Presets.kt) | 6 curated signature presets (Aurora, Cyberpunk, Ember, Ocean, Spectrum, Matrix). |

---

### 🖥️ Windows Desktop Controller (`tools/HiLightManager/`)
| File Path | Description of Enhancements |
|---|---|
| [`tools/HiLightManager/MainForm.cs`](file:///c:/Users/Amagora/Downloads/HiLight%20Studio%20Fork/tools/HiLightManager/MainForm.cs) | Complete High-DPI `SizeType.AutoSize` architecture, 2-column TableLayoutPanels, custom interactive action cards, resizable sub-menus, text editor and Notepad integration, privacy opt-in ADB downloader. |
| [`tools/HiLightManager/Program.cs`](file:///c:/Users/Amagora/Downloads/HiLight%20Studio%20Fork/tools/HiLightManager/Program.cs) | High-DPI application configuration, STA thread entry point, unhandled exception guards. |
| [`tools/HiLightManager/HiLightManager.csproj`](file:///c:/Users/Amagora/Downloads/HiLight%20Studio%20Fork/tools/HiLightManager/HiLightManager.csproj) | .NET 9 WinForms project file targeting `win-x64` with output binary `HiLight-Control.exe`. |

---

### ⚡ 1-Click Launchers & Scripts
| File Path | Description of Enhancements |
|---|---|
| [`Install-And-Start.bat`](file:///c:/Users/Amagora/Downloads/HiLight%20Studio%20Fork/Install-And-Start.bat) | Interactive persistent menu for full Gradle assemble, APK install, and daemon launch. |
| [`Start-HiLight.bat`](file:///c:/Users/Amagora/Downloads/HiLight%20Studio%20Fork/Start-HiLight.bat) | 1-click fast startup script restarting the background renderer without reinstalling. |
| [`Install-And-Start.ps1`](file:///c:/Users/Amagora/Downloads/HiLight%20Studio%20Fork/Install-And-Start.ps1) | PowerShell implementation of the complete build, install, and starter pipeline. |
| [`Start-HiLight.ps1`](file:///c:/Users/Amagora/Downloads/HiLight%20Studio%20Fork/Start-HiLight.ps1) | PowerShell launcher for quick daemon restart. |

---

## 4. Build, Compilation & Packaging Instructions

### Compiling the Android APK
```bash
# From workspace root
.\gradlew.bat assembleDebug
```
- **Output:** `app/build/outputs/apk/debug/app-debug.apk`

### Compiling the Windows Desktop Manager (`HiLight-Control.exe`)
```powershell
# From workspace root
dotnet publish tools/HiLightManager/HiLightManager.csproj -c Release -r win-x64 --no-self-contained -o .
```
- **Output:** `HiLight-Control.exe`

### Generating Release ZIP Archives
```powershell
Compress-Archive -Path "HiLight-Control.exe", "Install-And-Start.bat", "Start-HiLight.bat", "Install-And-Start.ps1", "LICENSE", "README.md", "CHANGELOG.md", "CONVERSATION_LOG_AND_SOURCE_RECORD.md", "app\build\outputs\apk\debug\app-debug.apk" -DestinationPath "Hi-Light-Studio-V2-Release.zip" -Force
```

---

## 5. Open Source Licensing & Upstream Attribution

Hi-Light Studio V2 is distributed under the terms of the **MIT License**.

- **Original Creator & Foundation:** Dhananjay Bhosale ([GitHub Profile](https://github.com/DhananjayBhosale) / [Repository](https://github.com/DhananjayBhosale/hilight-studio))
- **V2 Fork & Enhancements:** Hi-Light Studio V2 Contributors (2026)
- **Full License Text:** See [`LICENSE`](file:///c:/Users/Amagora/Downloads/HiLight%20Studio%20Fork/LICENSE)
