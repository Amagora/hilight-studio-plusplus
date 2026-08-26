# hilight-studio-plusplus (Build `a1.1.0`) - Changelog & Modifications Log

This document provides a comprehensive record of all custom modifications, architectural enhancements, user directives, and privacy protections implemented in **hilight-studio-plusplus** (version `a1.1.0`, forked from [DhananjayBhosale/hilight-studio](https://github.com/DhananjayBhosale/hilight-studio) v1.0.8-experimental).

---

## Table of Contents
1. [User Requests & Directives Log](#1-user-requests--directives-log)
2. [Privacy & Security Guarantees](#2-privacy--security-guarantees)
3. [Feature & Technical Modifications](#3-feature--technical-modifications)
4. [Desktop Tools & Automation](#4-desktop-tools--automation)
5. [Licensing & Open-Source Attribution](#5-licensing--open-source-attribution)

---

## 1. User Requests & Directives Log

Below is the chronological log of user directives, problems identified, and the technical solutions engineered in response:

| # | User Request / Directive | Problem / Context | Implemented Solution |
|---|---|---|---|
| **1** | *"Where is ADB located? Can we run rootless without Shizuku each time?"* | Android's `nohup app_process` ADB helper daemon was required to drive the hardware lights without needing Shizuku active constantly. | Located platform-tools in user SDK, identified rootless `AdbHelper` daemon command line, verified on device `66291FDDJ001HW` (Pixel 11 Pro Fold on Android 17 / API 37). |
| **2** | *"Are there any other improvements that can be seen here we can add to our fork/build? Let's do all of the above."* | Upstream lacked theming personalization, foldable layout utilization, fill light capabilities, and battery gauge indicators. | Engineered: 1) Material 3 Theming + AMOLED Pitch Black, 2) Dual-Pane Foldable Layout, 3) Tabletop Video Fill Light & Strobe, 4) 8-LED Battery Gauge, 5) Curated Signature Presets. |
| **3** | *"Can we make this a one click command?"* | Running multi-step ADB shell commands in terminal after reboots was tedious. | Created pre-packaged 1-click launch scripts (`Install-And-Start.bat`, `Start-HiLight.bat`, `.ps1` variants) for instantaneous execution. |
| **4** | *"The UI looks off"* | Scaffolding background was unpainted in certain sections; cards stretched excessively on wider foldable displays. | Added explicit root `Surface(color = MaterialTheme.colorScheme.background)`, restored proper Scaffold padding insets, and constrained cards with `widthIn(max = 720.dp)` for centered aesthetics. |
| **5** | *"The notifications and icons in the status bar are not visible in dark or AMOLED mode"* | Android system status bar text was rendering dark gray/black over dark and AMOLED pitch black backgrounds. | Implemented dynamic status bar insets controller: `WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark` ensuring crisp white status bar icons in Dark/AMOLED modes. |
| **6** | *"How does the battery level indicator option work?"* | User requested clarification and confirmation of battery level feature behavior. | Integrated `BatteryReceiver` measuring battery percentage (1–8 LEDs proportionally lit) and pulsing green animation when connected to AC/USB power. |
| **7** | *"Let's build a GUI to push commands, One for a full easy install, one for the previous ADB session kill, and one to start Hi-Light after a reboot."* | Needed a native Windows desktop GUI (`.exe`) to perform actions without touching CLI. | Created **`HiLight-Control.exe`** (.NET 9 WinForms application) featuring live device detection badge, 3 action cards, and real-time scrolling console log. |
| **8** | *"How do we package this up to share with original dev or someone else? Make sure MIT license is visible in both the app and the .exe."* | Compliance and easy sharing of the fork with upstream and community. | 1) Added interactive "About & License" dialog in Android app with full MIT license text. 2) Added "📜 MIT License" dialog in Windows `.exe`. 3) Packaged standalone `HiLight-Studio-Pixel-Fold-Release.zip`. |
| **9** | *"We should make sure the automatic download is searching the user's PC is optionally selectable. That could be considered malicious or an invasion of someone's data."* | Auto-scanning user directories or downloading files without explicit consent could compromise privacy and trust. | **Privacy-First Overhaul**: Restricted search solely to standard SDK environment variables and local app directory. Added mandatory explicit opt-in modal dialogs in the `.exe` and interactive `[Y/N]` prompts in scripts before downloading official Google platform-tools from `dl.google.com`. Added manual `Set ADB` / `Browse...` file picker. |
| **10** | *"We also need to make sure the disclosure for the use of A.I. is in the app on install/launch the user is only prompted to read it once and after they can find it in the Set Up section as it's own readable section called A.I. Disclosure."* | Transparency regarding AI-assisted development and ensuring it is accessible permanently without repeated prompts. | **A.I. Disclosure System**: 1) Implemented one-time first-launch dialog (`AiDisclosureDialog`) on initial open with an "I Understand" acknowledgement saved to persistent preferences (`aiDisclosureAcknowledged`). 2) Created dedicated **"A.I. Disclosure"** card in the **Setup** tab for permanent on-demand reading. |
| **11** | *"The .exe UI is broken in terms of spacing and layout. This also isn't an 11 Pro Fold build it is an overall build. I think we should call this Hi-Light Studio V2. Let's change this build to a custom build number a1.1.0."* | Layout alignment issues on Windows desktop; need universal branding across all Pixel 11 Pro series devices and custom versioning `a1.1.0`. | **Hi-Light Studio V2 & UI Overhaul**: 1) Rebranded to **Hi-Light Studio V2** across Android (`app_name`), Desktop manager, scripts, and docs. 2) Bumped version to **`a1.1.0`** (`versionCode 110`). 3) Complete WinForms UI rebuild with responsive layout panels, FlowLayoutPanel header buttons, and auto-filling console. 4) Enhanced scripts and GUI to stay persistently open after each action. |
| **12** | *"In the .exe as well as the app let's add a source reference to the original developer and their github page. Let's also add it to the .exe."* | Giving full credit, visibility, and 1-tap links to upstream creator Dhananjay Bhosale. | **Original Developer Attribution & Upstream Links**: 1) Added dedicated **"Original Creator & GitHub Source"** card in Android **Setup** tab with 1-tap buttons to [Dhananjay Bhosale's GitHub](https://github.com/DhananjayBhosale) and the [upstream repository](https://github.com/DhananjayBhosale/hilight-studio). 2) Added **`⭐ Dhananjay's GitHub`** button in the desktop GUI header, MIT License dialog, and A.I. Disclosure dialog. |
| **13** | *"Portions of the UI are still cut off. We should add an information icon next to the 'Change ADB' icon that tells the user in a separate popup window what the button is for."* | DPI text truncation on Windows buttons and user clarification on the optional nature of manual ADB selection. | **DPI Auto-Sizing & ADB Info Modal**: 1) Converted all Windows GUI buttons to `AutoSize = true` with `Padding = (12, 6, 12, 6)` and expanded client bounds (`1080x780`) to completely eliminate text clipping. 2) Added **`ℹ️ What's this?`** button opening a dedicated modal explaining ADB, confirming automatic detection, and explaining when custom paths are used. |
| **14** | *"The UI Elements the buttons that need to be clicked to change the ADB location, the 'What's this' button, the 'Refresh Connection' button, the 'Clear' button, the 'Universal Pixel' text... all of it is still hidden, partially cut off, and invisible."* | Fixed row pixel heights in TableLayoutPanel were overriding High-DPI scaling and clamping rendered rows to unscaled bounds, vertically crushing header text, buttons, and action cards. | **Root-Cause Resolution with SizeType.AutoSize Architecture**: Replaced all hardcoded row heights with `RowStyle(SizeType.AutoSize)` and `AutoSizeMode.GrowAndShrink` across all parent and child panels. Controls now dynamically query font metrics and DPI scale factors at runtime, guaranteeing that every title, subtitle, button shape, and icon renders with 100% full bounds and zero clipping. |
| **15** | *"So the sub menus also have broken buttons lets make the windows adjustable in size plus they need to be able to open in notepad or a text editor of the user's choice."* | Sub-menu dialogs had fixed borders and hardcoded pixel positioning, preventing resizing and causing DPI truncation. Users also needed external text editor access for license, logs, and disclosures. | **Resizable Dialog Windows & Text Editor Integration**: 1) Converted all modal dialogs to `FormBorderStyle.Sizable` with dynamic `TableLayoutPanel` content docking. 2) Added **`📝 Open in Text Editor`** and **`📄 Open in Notepad`** buttons to all dialogs, enabling instant viewing in the user's preferred text editor or default Notepad. 3) Added **`📋 Copy`** buttons for 1-click clipboard capture. |

---

## 2. Privacy & Security Guarantees

In accordance with user directives regarding privacy and data integrity:

1. **Zero Invasive File Scanning**:
   - The application and scripts **NEVER** scan arbitrary personal directories (such as `%USERPROFILE%\Downloads`, `Documents`, or `C:\` root).
   - Only standard, well-defined developer paths are checked:
     - `.\platform-tools\adb.exe` (Local folder next to the app)
     - `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe` (Standard Android Studio path)
     - `%ANDROID_HOME%\platform-tools\adb.exe` & `%ANDROID_SDK_ROOT%`
     - System `PATH`
2. **Explicit Opt-In User Consent for Network Downloads**:
   - If ADB is not found in standard developer paths, **no network activity or file download will ever occur silently in the background**.
   - In `HiLight-Control.exe`: An explicit modal dialog is presented explaining that ADB is required, showing the exact official source (`dl.google.com`), and giving three selectable options:
     - `[📥 Download Google ADB (~5 MB)]`
     - `[📂 Browse for adb.exe...]`
     - `[Cancel]`
   - In `.bat` and `.ps1` scripts: An interactive prompt requires the user to type `Y` to confirm downloading from Google's official CDN, or `N` to cancel cleanly without touching the filesystem.
3. **Manual Override Always Accessible**:
   - The desktop manager provides a `📁 Set ADB` button directly in the device status bar so users can inspect or reconfigure their selected ADB binary at any time.

---

## 3. Feature & Technical Modifications

### A. Material 3 Theming Engine & AMOLED Pitch Black
- **Files Modified**: `Theme.kt`, `Store.kt`, `SetupScreen.kt`, `strings_setup.xml`, `values-ja/strings_setup.xml`.
- **Modes**:
  - `Follow System`: Adheres to device day/night cycle.
  - `Light Mode`: Crisp Material 3 light container surface.
  - `Dark Mode`: Standard dark gray surface (`#121216`).
  - `AMOLED Pitch Black`: Pure `#000000` surface and backgrounds, maximizing battery savings on OLED and foldable displays.
- **Curated Color Palettes**:
  - `Dynamic Material You` (wallpaper-derived), `Pixel Indigo`, `Ocean Blue`, `Emerald Green`, `Coral Peach`, `Amber Gold`, `Berry Rose`, and `Monochrome`.

### B. Foldable Adaptive Dual-Pane Layout
- **Files Modified**: `MainActivity.kt`.
- **Implementation**:
  - Detects unfolded inner display width (`>= 600dp`) on Pixel 11 Pro Fold.
  - Renders a persistent side `NavigationRail` and split-view pane with live real-time `DeviceHero` 8-LED hardware visualization on the left and active control cards on the right.
  - Retains compact single-column navigation bar when closed/folded (`< 600dp`).

### C. Tabletop Video Fill Light & Emergency Strobe Beacon
- **Files Modified**: `LiveScreen.kt`, `strings_live.xml`, `values-ja/strings_live.xml`.
- **Features**:
  - **Calibrated Color Temperatures**: Warm (2700K `#FFA94D`), Soft (3800K `#FFD199`), Neutral (4500K `#FFF0DD`), and Cool (6500K `#EDF4FF`).
  - **Continuous Fill Lighting**: Turns the rear 8-LED array into a soft, high-CRI key light for video conferencing, selfie recording, and tabletop macro photography when the Pixel Fold is propped open.
  - **Emergency Strobe & Beacon**: Fast strobe (10 Hz) and SOS emergency flashing modes for roadside visibility or outdoor distress signaling.

### D. 8-LED Battery & Charging Fuel Gauge
- **Files Modified**: `Store.kt`, `SetupScreen.kt`, `LiveScreen.kt`, `strings_setup.xml`, `values-ja/strings_setup.xml`.
- **Features**:
  - Proportional LED fuel gauge lighting 1 to 8 LEDs based on real-time battery percentage (0%–100%).
  - Soft breathing green animation when the phone is connected to AC or USB-C power.

### E. Curated Signature Presets Pack
- **Files Modified**: `AmbientScreen.kt`.
- **Presets Added**:
  - *Aurora Borealis* (Green/Cyan polar wave)
  - *Cyberpunk Neon* (Hot Magenta & Cyan chase)
  - *Campfire Ember* (Warm orange-red glowing breathe)
  - *Deep Ocean* (Calm azure blue wave)
  - *Pixel Spectrum* (Full RGB rainbow cycle)
  - *Matrix Pulse* (Phosphor green digital pulse)

### F. Status Bar & UI Visual Polish
- **Files Modified**: `MainActivity.kt`, `Theme.kt`, `SetupScreen.kt`.
- **Fixes**:
  - Implemented dynamic status bar appearance switching (`isAppearanceLightStatusBars = !isDark`), ensuring notification and battery icons on the phone remain visible across Light, Dark, and AMOLED modes.
  - Added root background `Surface` and responsive card centering (`widthIn(max = 720.dp)`).

---

## 4. Desktop Tools & Automation

### A. Windows Desktop GUI Control Manager (`HiLight-Control.exe`)
- **Project Path**: `tools/HiLightManager/` $\rightarrow$ Published as single-file executable to workspace root.
- **Framework**: .NET 9 WinForms, compiled for `win-x64`.
- **Capabilities**:
  1. **Live Device Status Header**: Polls connected Pixel devices, displays product model name (e.g. `Pixel 11 Pro Fold`), serial number, and connection health.
  2. **1. Full Easy Install & Start**: Verifies/builds APK with Gradle, streams `adb install -r`, launches main activity, and starts the 8-LED renderer.
  3. **2. Start HiLight (Post-Reboot)**: Restarts the `AdbHelper` background daemon in 1 click without reinstalling.
  4. **3. Stop / Kill ADB Session**: Terminates existing `com.hilight.core.AdbHelper` processes and releases the hardware lights HAL.
  5. **Real-Time Console Log**: Integrated dark terminal box with timestamped command execution logs.
  6. **MIT License Viewer**: Dedicated header button displaying full open-source licensing terms.
  7. **User-Consented ADB Setup**: Modal dialog with options to download official Google platform-tools or browse for local `adb.exe`.

### B. 1-Click Script Launchers
- **`Install-And-Start.bat` & `Install-And-Start.ps1`**: Full build, install, launch, and daemon start script.
- **`Start-HiLight.bat` & `Start-HiLight.ps1`**: Quick daemon restart script.

---

## 5. Licensing & Open-Source Attribution

- **License**: MIT License.
- **Upstream Authors**: Copyright (c) 2026 HiLight Studio contributors ([DhananjayBhosale/hilight-studio](https://github.com/DhananjayBhosale/hilight-studio)).
- **Fork Authors**: Copyright (c) 2026 HiLight Studio Fork contributors.
- **In-App Visibility**:
  - Accessible in mobile app under **Setup $\rightarrow$ About & License $\rightarrow$ View MIT License**.
  - Accessible in Windows desktop app via the **"📜 MIT License"** header button.
