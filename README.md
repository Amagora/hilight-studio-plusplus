# Hilight-Studio-PlusPlusV3

**Version: `a1.2.0`** — Universal Hardware Lighting Control for Pixel 11 Pro, Pixel 11 Pro XL, and Pixel 11 Pro Fold devices on Android 17 (API 37).

[![Build Version](https://img.shields.io/badge/version-a1.2.0-blue.svg)](CHANGELOG.md)
[![License: MIT](https://img.shields.io/badge/license-MIT-2f81f7.svg)](LICENSE)
[![Changelog](https://img.shields.io/badge/changelog-custom%20modifications-blue.svg)](CHANGELOG.md)

> [!WARNING]
> ### ⚖️ Non-Liability & Experimental A.I. Code Disclaimer
> This software contains experimental hardware lighting modifications and AI-assisted code for Google Pixel 11 Pro series devices. It is provided **"AS-IS"** under the MIT License without warranty of any kind, express or implied.
> - **Use at Your Own Discretion & Risk**: The developers, fork authors, contributors, and upstream creators assume **zero liability or responsibility** for any hardware damage (including LED burnout, thermal stress, or battery wear), data loss, software errors, security risks, or system instability resulting from the use of this application, ADB daemon scripts, or desktop utilities.
> - **User Acceptance**: By downloading, compiling, installing, or executing any part of this project, you acknowledge and agree that you assume all risks associated with its operation.

> [!NOTE]
> See [CHANGELOG.md](CHANGELOG.md) for the detailed log of custom additions, user requests, architectural enhancements, and privacy guarantees.

> [!IMPORTANT]
> **Hilight-Studio-PlusPlusV3** is an open-source enhancement supporting all devices across the **Pixel 11 Pro line** (Pixel 11 Pro, Pixel 11 Pro XL, and Pixel 11 Pro Fold) on Android 17 (API 37).

<p align="center">
  <img src="docs/media/screen-live.png" alt="Live tab controlling the HiLight array on a Pixel 11 Pro Fold" width="380">
</p>

## Features

- **Material 3 Theming Engine**:
  - **Theme Modes**: System Follow, Light, Dark, and AMOLED Pitch Black (pure `#000000` power-saving mode for OLED and foldable screens)
  - **Material 3 Palettes**: Dynamic Material You (wallpaper colors), Pixel Indigo, Ocean Blue, Emerald Green, Coral Peach, Amber Gold, Berry Rose, and Monochrome
- **Foldable Adaptive Dual-Pane Layout**:
  - Automatically switches between single-column navigation and dual-pane side `NavigationRail` with persistent real-time 8-LED `DeviceHero` when unfolded on the Pixel 11 Pro Fold inner screen (`>= 600dp`).
- **Tabletop Video Fill Light & Emergency Strobe Beacon**:
  - Tuned continuous lighting for video calls, selfies, and tabletop shooting with color temperature presets: Warm (2700K), Soft (3800K), Neutral (4500K), and Cool (6500K).
  - High-visibility emergency beacon and SOS strobe modes.
- **8-LED Battery & Charging Fuel Gauge**:
  - Visual battery level indicator that lights LEDs proportionally (1–8) and breathes green when plugged in or charging.
- **Signature Curated Presets Pack**:
  - Built-in one-tap lighting themes: *Aurora Borealis*, *Cyberpunk Neon*, *Campfire Ember*, *Deep Ocean*, *Pixel Spectrum*, and *Matrix Pulse*.
- **Desktop GUI Control Manager (`HiLight-Control.exe`)**:
  - Native Windows desktop application with 3 one-click actions:
    1. *Full Easy Install & Flash*: Automatically installs/updates APK, launches the app, and starts the 8-LED renderer.
    2. *Start HiLight (Post-Reboot)*: Restarts the background ADB daemon in 1 click after phone reboot.
    3. *Stop / Kill ADB Session*: Cleanly terminates the background renderer and frees hardware lights control.
    4. *Live Device Status*: Real-time connection badge (Pixel 11 Pro Fold) and integrated console log output.
- Solid colours and animated patterns across all eight LEDs on Pixel 11 Pro, Pixel 11 Pro XL, and Pixel 11 Pro Fold
- Per-app rules for foreground use and notifications
- Customisable microphone and camera activity rules, with any built-in animation and colour, for any app or one selected app
- Per-contact rules: a colour for one person or one chat, picked from the chats HiLight has seen
- Saved presets with import and export
- Wallpaper-derived colours and a Quick Settings tile
- Quiet hours, Do Not Disturb, Battery Saver, and low-battery controls
- English and Japanese, selectable per app from Android's own language settings
- Automatic root access when available, with Shizuku and ADB as fallbacks
- Manual update checks against GitHub releases

## Screenshots

### 📱 Android Application (Pixel 11 Pro Series / Fold)

<table>
<tr>
<td width="33%"><img src="docs/media/screen-style.png" alt="Style tab with presets, patterns, and colour controls"></td>
<td width="33%"><img src="docs/media/screen-apps.png" alt="Apps tab with per-app rules"></td>
<td width="33%"><img src="docs/media/screen-setup.png" alt="Setup tab with access and safety controls"></td>
</tr>
<tr>
<td align="center"><sub><b>Style &amp; Signature Presets</b></sub></td>
<td align="center"><sub><b>Apps &amp; Notification Rules</b></sub></td>
<td align="center"><sub><b>Setup &amp; Safety Controls</b></sub></td>
</tr>
</table>

### 🖥️ Windows Desktop Control Manager (`HiLight-Control.exe`)

<p align="center">
  <img src="docs/media/screen-desktop-manager.png" alt="hilight-studio-plusplus Windows Desktop Control Manager" width="850">
</p>

## 🖥️ Desktop Companion Suite (`HiLight-Control.exe`)

**`HiLight-Control.exe`** is a native Windows GUI controller designed to give users complete 1-click management of their Google Pixel 11 Pro series hardware lights without touching the command line:

- **🟢 Real-Time Hardware Detection**:
  - Live status badge displaying device connectivity, Pixel model name (Pixel 11 Pro, 11 Pro XL, or 11 Pro Fold), and USB/Wi-Fi hardware connection state.
- **⚡ 3 One-Click Action Cards**:
  1. **`🚀 1. Full Easy Install & Start`**: Instantly locates or compiles the latest APK, flashes/updates it on your connected Pixel, launches the app, and starts the rootless 8-LED renderer.
  2. **`⚡ 2. Start Hi-Light (After Phone Reboot)`**: Re-initializes the background ADB helper daemon in seconds after restarting your phone without needing to reinstall the APK.
  3. **`🛑 3. Stop / Kill ADB Session`**: Cleanly terminates the background process and turns off / releases hardware lights control.
- **🔒 Privacy-First ADB Detection**:
  - **Zero invasive directory scanning**: Automatically searches only standard developer environment variables (`ANDROID_HOME`, `ANDROID_SDK_ROOT`, standard SDK path, or local folder).
  - **Explicit User Consent**: If ADB is not found, provides an interactive opt-in dialog to download official Google platform-tools directly from `dl.google.com`, or select an existing `adb.exe` manually.
- **📐 High-DPI Adaptive Geometry & Resizable Windows**:
  - Built with dynamic `SizeType.AutoSize` architecture that looks crisp and legible on all monitor scaling factors (100%, 125%, 150%, 175%, 4K).
  - All sub-menus (ADB Setup, MIT License, A.I. Disclosure, Request Log) are fully resizable and maximizable.
- **📝 Text Editor & Notepad Integration**:
  - Every informational dialog features **`📝 Open in Text Editor`** (opens in your default system editor) and **`📄 Open in Notepad`** buttons for instant viewing and editing.
- **📜 Live Output Console**:
  - Real-time scrolling terminal output stream with timestamped execution feedback and a 1-click **`🗑️ Clear Console`** action.


## Build & Install on Google Pixel 11 Pro Series

### 1. Build Your Private APK

To compile your own private build:

```bash
# On Windows PowerShell:
.\gradlew.bat assembleDebug

# On Linux/macOS:
./gradlew assembleDebug
```

The APK will be generated at: `app/build/outputs/apk/debug/app-debug.apk`.

### 2. Install / Flash to Pixel 11 Pro, Pixel 11 Pro XL, or Pixel 11 Pro Fold

Connect your **Pixel 11 Pro, Pixel 11 Pro XL, or Pixel 11 Pro Fold** via USB with **USB debugging** enabled in Developer Options:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

If replacing an earlier release signed with a different key:
```bash
adb uninstall com.hilight.studio
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

hilight-studio-plusplus needs privileged access to the Android lights service. Choose one of the privilege methods below:

### Root

If the phone is rooted, open HiLight Studio and turn it on. The app detects root automatically and
uses it instead of Shizuku or ADB. Approve the one-time request from your root manager when it
appears; no other setup is needed.

### Shizuku

1. Install [Shizuku](https://shizuku.rikka.app/).
2. Start it using Wireless debugging.
3. Open HiLight Studio, go to **Setup**, tap **Request access**, and approve the request.

Restart Shizuku after each reboot, then reopen HiLight Studio.

### ADB

1. Enable **Developer options** and **USB debugging** on the phone.
2. Install and open HiLight Studio once so it can create its state files.
3. Run both commands below. The first stops any existing renderer. The second starts a fresh ADB helper.

macOS, Linux, or PowerShell (verified):

```bash
adb shell "pkill -f 'com.hilight.(core.AdbHelper|studio:hilight)'"
adb shell 'CLASSPATH=$(pm path com.hilight.studio | head -1 | cut -d: -f2) nohup app_process / com.hilight.core.AdbHelper > /data/local/tmp/hilight.log 2>&1 &'
```

Windows Command Prompt (Unverified):

```bat
adb shell "pkill -f 'com.hilight.(core.AdbHelper|studio:hilight)'"
adb shell "CLASSPATH=$(pm path com.hilight.studio | head -1 | cut -d: -f2) nohup app_process / com.hilight.core.AdbHelper > /data/local/tmp/hilight.log 2>&1 &"
```

Keep the two commands separate. The `pkill -f` pattern can match the shell that starts the helper if both operations are merged.

Command Prompt passes the pipe, parentheses, redirects, and `$()` through inside the double quotes. The phone resolves the installed app path.

Check the helper log:

```bash
adb shell cat /data/local/tmp/hilight.log
```

A successful start reports `connected: 8 HiLight LEDs`. An empty log usually means the command was quoted for the wrong shell.

### If the app connects but the LEDs stay dark

Only one renderer can drive the array. A leftover renderer can keep sending black while the app still reports a connection.

Count active sessions:

```bash
adb shell dumpsys lights | grep -c "Session token="
```

There should be exactly one. If there are more, run the reset command and then the start command again.

After setup, grant **Notification access** for notification rules and **Usage access** for
foreground-app rules. Privacy activity rules observe Android's active microphone or camera state in
the privileged renderer and do not need either permission. Turn on **Live**, then choose a look in
**Style**. A new installation starts with its always-on style set to **Off**.

## Safety limits

The renderer enforces these limits even if app state is edited:

- Ambient effects stop after 30 seconds by default and can be raised to 5 minutes.
- Notification effects are limited to 1 minute.
- Privacy activity rules run only while the microphone or camera remains active. Their default rhythm
  is 10 seconds on, 10 seconds off, with a 1-minute maximum per continuous use.
- Sustained brightness tapers after 10 seconds of continuous light.
- The array can be active for at most half of any 10-minute window.
- Battery Saver, low-battery, quiet-hours, screen-state, and Do Not Disturb rules can pause output.

Long, continuous use of the HiLight LEDs has not been tested. If you build the project yourself, you can change the timing and safety values in your copy. Custom builds are your responsibility.

See [Technical details](docs/TECHNICAL.md) for the renderer architecture, hardware findings, device verification, and known limits.

## Privacy

HiLight Studio has no analytics, account system, or telemetry. It uses the internet only when you
tap **Check for updates** under Setup, which fetches public release information from GitHub. No app
rules, notification data, or settings are sent. App rules and presets stay on the device.
Notification and usage access are optional and are used locally for the rules you enable. Privacy
activity rules observe only whether Android reports the microphone or camera as active; HiLight never
reads or records audio, video, or their contents.

Per-contact rules read the sender's name from the notification itself, so they need no contacts permission — picking a contact by hand uses the system picker, which hands over only the row you tap. HiLight remembers the names of chats it has seen so the picker needs no typing; that list is stored on the device, is capped, and can be cleared at any time with **Forget remembered chats** under Setup. Message text is never stored, never logged, and never included in anything the notification inspector copies or shares.

## Build from source

Requirements:

- JDK 21
- Android SDK platform 37.0
- Android Studio or a command-line Android SDK installation

```bash
git clone https://github.com/DhananjayBhosale/hilight-studio.git
cd hilight-studio
./gradlew :app:testDebugUnitTest :app:build :app:lint
```

Build an installable developer APK with:

```bash
./gradlew :app:assembleDebug
```

The APK is written under `app/build/outputs/apk/debug/`. You may fork the repository, change the source, and build your own version under the terms of the MIT License.

## Contributing

Issues and pull requests are welcome. Hardware reports should include the Pixel model, Android build, renderer transport, and exact steps to reproduce. Do not include notification contents or other personal data.

Read [Contributing](CONTRIBUTING.md) before opening a pull request. Security issues must follow the private process in [Security policy](SECURITY.md).

## Project documents

- [Changelog](CHANGELOG.md)
- [Technical details](docs/TECHNICAL.md)
- [Release process](docs/RELEASING.md)
- [Code of Conduct](CODE_OF_CONDUCT.md)

## License

[MIT](LICENSE). You may use, modify, redistribute, and sell the project. Redistributed copies must retain the license notice.
