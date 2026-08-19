# HiLight Studio

Custom control of the Pixel 11 Pro / Pro XL / Pro Fold **HiLight** LED array: any colour, any
pattern, always-on, random colours, and per-app rules.

Built and verified against a real device: `Pixel 11 Pro XL (kodiak)`, Android 17,
build `CD1A.260714.001.A9`, SDK 37.

> **Experimental hardware project.** This is for the Pixel 11 Pro, Pro XL, and Pro Fold on
> Android 17 only. It relies on shell-level access provided by Shizuku or ADB; it does not bypass
> Android security or work on a stock device by itself. Please read the LED safety limits before
> enabling an always-on pattern.

---

## What HiLight actually is

Findings from the device itself, not from the marketing pages:

| Property | Value |
|---|---|
| Hardware | **8 individually addressable RGB LEDs** in the array around the camera flash |
| Framework type | `Light.LIGHT_TYPE_APPLICATION` (`10`), new in API 37 |
| Light ids / ordinals | ids `1..8`, ordinals `0..7` (id `0` is the display backlight and is not exposed by `LightsManager`) |
| Capabilities | `hasRgbControl() = true`, `hasBrightnessControl() = false`, `hasAnimationControl() = true` |
| Min update period | `33 ms` per LED, i.e. ~30 fps |
| HAL | `android.hardware.light` **AIDL version 3** (`vendor.google.lights-service`), AOSP ships v2 |
| System feature name | `AmbientCue` — `/product/overlay/AmbientCueOverlay.apk`, plus `vendor.google.ambience_hub.*` HAL services |
| Stock features | custom colour per favourite contact (Phone by Google, WhatsApp) and a Gemini listening/thinking/responding indicator |

New public API in Android 17 (API 37), all in `android.hardware.lights`:

- `ColorSequence` + `ColorSequence.Builder` — keyframed colour ramps (`addControlPoint(delayMs, color)`,
  `INTERPOLATION_MODE_NONE` / `INTERPOLATION_MODE_LINEAR`)
- `MultiLightEffect` + `Builder` — one `ColorSequence` per LED, with `setIterations()` and `setPreemptive()`
- `LightsRequest.Builder.setEffect(...)`, `Light.hasAnimationControl()`, `Light.getMinUpdatePeriodMillis()`

Underlying binder interface (`ILightsManager`): `getLights()`, `openSession(IBinder, int priority)`,
`setLightStates(token, int[] ids, LightState[])`, `setLightEffect(token, MultiLightEffect)`,
`getLightState(id)`, `getLightSequence(id)`, `closeSession(token)`.

### Why a privileged helper process is required

`android.permission.CONTROL_DEVICE_LIGHTS` is `signature|privileged` on this build, and
`LightsService` enforces it on every call:

```
java.lang.SecurityException: Access denied, requires: android.permission.CONTROL_DEVICE_LIGHTS
  at android.hardware.lights.ILightsManager$Stub.getLights_enforcePermission
```

It is not a changeable permission, so `pm grant` refuses it, and the device is a retail unit with a
locked bootloader (`ro.boot.flash.locked=1`, `verifiedbootstate=green`, no root) — there is no way to
install an app as privileged.

However `android.uid.shell` (uid 2000) **already holds it** (`granted=true`). So the rendering runs in
a process owned by the shell UID. Everything else — UI, rules, notification listening — is a normal
app.

---

## Architecture

The renderer core (`core/src`) is shared, and there are two ways to get it into a shell-UID process.

```
HiLight Studio (normal app)                    privileged renderer (uid 2000 = shell)
┌─────────────────────────────────┐            ┌────────────────────────────────────┐
│ Compose UI: Live/Ambient/Apps   │  binder    │ Shizuku: HiLightUserService        │
│ NotificationTrigger (listener)  │ ─────────► │   com.hilight.studio:hilight       │
│ ForegroundWatcher (UsageStats)  │            ├────────────────────────────────────┤
│ Store: layering + rules         │  2 JSON    │ ADB: com.hilight.core.AdbHelper    │
│ Transport: Auto/Shizuku/ADB     │ ◄────────► │   run from the installed APK       │
└─────────────────────────────────┘  files     └────────────────────────────────────┘
                                                shared core: Engine + Renderer + LightsBackend
```

**Shizuku transport (preferred, no computer).** Shizuku launches `HiLightUserService` into a shell-UID
process (`daemon(true)`, so it outlives the UI) and the app holds a real binder to it — state is
pushed straight in, no polling. Verified running as `shell` uid 2000.

**ADB transport (fallback).** `AdbHelper` ships inside the APK, so a single command starts it with no
file to push. Cross-UID binder is not usable there: a shell-UID process that touches a
`ContentProvider` is killed by ActivityManager (verified), which rules out both a provider bridge and
`ContentObserver` push. So that transport exchanges two small JSON files instead.

**File ownership rule that matters** for the ADB transport: on external storage a file keeps the UID
of whoever created it. A file created by the shell is unreadable by the app, but the shell *can* write
into a file the app owns. So the app creates the directory and both files, and the helper only ever
overwrites in place.

Only one renderer may drive the array at a time. When Shizuku is active the app writes
`enabled:false` to the ADB state file so any leftover helper releases the session, and if Shizuku goes
away the app re-pushes so the ADB helper takes over.

Output layering, highest first:

1. a finite notification alert
2. an infinite "while this app is open" override
3. the always-on ambient look

Turning control off blanks the LEDs and closes the session, handing HiLight back to Android.

---

## Install and run

```bash
./gradlew :app:installDebug
```

Then pick one of the two ways to start the renderer. The **Setup** tab shows the state of both and
lets you choose Auto / Shizuku / ADB.

### Option A — Shizuku, no computer

1. Install [Shizuku](https://shizuku.rikka.app/).
2. In Shizuku, use **Start via Wireless debugging** (pair once with the code from Developer options).
3. In HiLight Studio's Setup tab, tap **Request access** and allow it in Shizuku's dialog.

Shizuku has to be started again after each reboot — that is a property of adb access, not of this app.
Everything after that is on-device.

Order matters: **start Shizuku first, then open HiLight Studio.** Shizuku hands access to an app when
that app's process starts, so a Shizuku started afterwards stays invisible until the app is reopened.
(`ShizukuProvider.requestBinderForNonProviderProcess()` does not help — it only talks to the app's own
provider.) The Setup card says as much when it finds Shizuku missing.

### Option B — ADB, one command

```bash
adb shell "CLASSPATH=$(pm path com.hilight.studio | head -1 | cut -d: -f2) nohup app_process / com.hilight.core.AdbHelper > /data/local/tmp/hilight.log 2>&1 &"
```

Nothing is pushed — this runs the renderer classes out of the installed APK, so it keeps working after
app updates. The Setup tab shows the same command with a copy button. `scripts/start-helper.sh` is the
same thing with a liveness check. Re-run after a reboot.

### Finally

Grant notification access from the Setup tab (needed for per-app rules), optionally Usage access (only
for "while this app is open" rules), then flip the **Live** switch to take over the LEDs.

### Requirements

- Pixel 11 Pro / Pro XL / Pro Fold on Android 17
- USB debugging enabled
- Android SDK with **platform 37** installed (the helper compiles against `android-37.1/android.jar`)

---

## The device illustration

The Live tab draws the phone's own back with HiLight lit by the same pattern maths the hardware runs.
It is a vector reconstruction, not a bundled press image: Google's product renders are copyrighted, so
shipping them in an app is not an option, and a drawing can be animated by the live frame data anyway.

It follows `Build.MODEL`:

| Model | Layout |
|---|---|
| Pixel 11 Pro / Pro XL | full-width camera bar, three lenses, HiLight at the right-hand end |
| Pixel 11 Pro Fold | unfolded rear panel with the hinge seam, compact camera block top-left, HiLight inside it |
| Pixel 11 (non-Pro) | camera bar with a plain flash, and the card says HiLight is Pro-only |
| anything else | generic Pro-style layout |

The framing is a close crop on the camera bar — only the top of the device is shown, running off the
bottom of the card — which is how Google frames the feature in its own material.

The array is drawn as one diffused disc rather than eight pinpoints, because the eight LEDs sit behind
a single flash window. Each LED still contributes its own colour from its position inside the window,
clipped to the window so the light keeps a crisp edge, so a chase or a rainbow visibly travels around
the lamp.

## Features

**Ambient (always-on)** — Off, Solid, Gradient, Breathe, Blink, Pulse, Chase, Comet, Wave, Rainbow,
Random colours, and Per-LED custom, plus brightness, rainbow spread, random interval / per-LED / fade /
saturation, and per-LED colours with optional rotation.

**Time per cycle** is how long one repetition of the animation takes, which means something different
per pattern — one breath, one on-off pair, one flash and fade, one lap of the array, one trip through
every hue. The app states the meaning under the slider, and hides the slider entirely for the patterns
whose maths ignore it: Solid, Gradient, Off, Random colours (it has its own "change every" interval)
and Per-LED custom (it has its own rotate control).

**Per-app rules** — pick any installed app, or the **Any app** catch-all that covers everything without
a rule of its own, then choose:
- trigger: on notification, or while the app is open
- pattern, fixed colour or a fresh random colour each time
- an optional keyword, so only notifications mentioning it light the array
- duration, time per cycle, brightness, and "only when the screen is off"

Do Not Disturb is respected by default (read through the notification listener, which needs no extra
permission).

**Presets** — save the current look under a name, apply it with a tap, and move looks between devices:
Export shares the whole set as JSON, Import merges a pasted document.

**Wallpaper palette** — one tap fills the eight LEDs from the current Material You scheme, hues pushed
to full saturation because container tones are too pale to read on an LED.

**Random colours** — either one colour for the whole array or a different colour per LED, with
optional smooth fading between picks.

**Quick Settings tile** — take the array over or hand it back without opening the app; long-press opens
the app. Add it from the Quick Settings edit screen ("From apps that you installed"). The subtitle
reports the current look, or why the array is dark: *No renderer*, *Off*, *Timed out*, *Resting*.

**Quiet hours and battery guard** — a window when the array stays dark, or optionally **dims** to a low
brightness instead (crossing midnight is handled), and a pause below a battery threshold that is
ignored while charging. There is also a global **only while the screen is off** switch, which suits the
face-down case and saves power; the screen going off starts a fresh auto-off window, still bounded by
the duty guard. Both override the master switch
and hand the array back to the system rather than merely blanking it, so the system's own call and
Gemini effects still work. The reason is shown in Live, in Setup, and in the tile's subtitle.

**Safety state in Live** — a live countdown to auto-off plus duty usage, and a plain explanation when
the array is dark because a limit kicked in, so it never looks like a fault.

Brightness is applied by scaling RGB, because the hardware reports no brightness channel.

---

## Verified on device

- 8 LEDs enumerated with the capabilities in the table above
- solid, per-LED rainbow, comet, wave, breathe, pulse and random rendering on the real hardware
- alert layer expiring back to ambient, and an infinite override being cleared
- UI → hardware: picking Solid violet at 70% produced `ff5635b2` on all 8 LEDs
- notification path: a notification from a rule's package produced a green pulse within one frame
- foreground path: opening Chrome produced solid `ff2979ff`, returning home restored ambient
- animation keeps running with the screen off (`mState=DOZE`) — the face-down case
- turning control off closes the session and blanks the array
- Shizuku transport: user service starts as `shell` uid 2000 with 8 LEDs, binder connects, ambient and
  notification alerts render with no adb helper running at all
- ADB one-liner starts the renderer straight out of the installed APK
- failover: killing the Shizuku server mid-animation is detected, state is re-pushed, and the ADB
  helper picks the array up — never two sessions at once
- Shizuku 13.6.0 (official release, signer `CN=Rikka`) used for all of the above

## LED safety

The array is not built for continuous use — stock HiLight only flashes for a moment — so the limits
live in `Engine`, not in the UI, and no state document can opt out of them:

| Guard | Default | Ceiling |
|---|---|---|
| Ambient auto-off | 30 s | 5 min, behind two warnings |
| Per-app notification | 10 s | 1 min, behind two warnings |
| Alert hard clamp | — | 60 s, whatever the app asks for |
| Open-ended holds ("while open") | — | capped at the auto-off value |
| Duty cycle | — | at most 50% of any 10-minute window |
| Sustained brightness | — | eases to 55% after 10 s of unbroken light |

Two details that matter:

- **Only deliberate user action restarts the auto-off window.** A notification firing, a foreground
  override, or the app being backgrounded all push state with `arm: false`, so the array cannot be
  kept lit indefinitely in 30-second increments.
- **Leaving the app kills a running test.** `onStop` clears the preview immediately and does not hand
  ambient a fresh window on the way out.

Verified on device: brightness taper visible as `ff4d50 → 8c2a2c`; auto-off blanking at exactly 30 s;
duty guard tripping after 10 032 ms lit in a (temporarily shortened) 20 s window, resting, then
resuming when the window rolled over; a notification playing without extending the ambient window; and
a test stopping the moment the app went to the background.

What still cannot be measured here: actual power draw and LED junction temperature. Android does not
attribute either per-LED, so these figures are conservative by design rather than tuned to data.

## Known limits

- Privileged access has to be re-established after every reboot: either restart Shizuku (on-device,
  ~30 s) or re-run the adb command. Nothing an installed app can do avoids this on a locked device.
  A one-time setup would need either root or an unlocked bootloader (app in `/system/priv-app`).
- If Shizuku is (re)started while HiLight Studio is already running, reopen the app so Shizuku can hand
  it access. Shizuku's own "Authorized applications" count also resets when its server restarts, so it
  may ask for approval again.
- While our session is open the system's own HiLight effects (calls, Gemini) may be suppressed. The
  Setup tab exposes the session **priority** so you can bias arbitration either way; the exact
  arbitration rule in `LightsService` was not reverse-engineered.
- Deep sleep suspends the CPU, so animations freeze at the last frame until the device wakes. Static
  colours are unaffected.
- Notification rules ignore ongoing notifications (media, progress) to avoid constant retriggering.

## Contributing

Issues and pull requests are welcome. Please include the exact Pixel model, Android build, renderer
transport (Shizuku or ADB), and steps to reproduce for any hardware issue. Start with
[CONTRIBUTING.md](CONTRIBUTING.md) for the local checks and scope guidelines.

## Security

Do not report a potential security issue in a public issue. See [SECURITY.md](SECURITY.md) for the
private reporting process.

## License

This project is licensed under the [MIT License](LICENSE). You may use, modify, redistribute, and
sell it; keeping the license text with redistributed copies is the only notice requirement.
