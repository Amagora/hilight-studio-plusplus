# Changelog

All notable changes to HiLight Studio are documented here.

## [Unreleased]

## [1.0.3-experimental] - 2026-08-20

- Fixed notification alerts that could leave the LEDs lit indefinitely, end early after an
  unrelated settings update, or continue after the phone was unlocked.
- Added a **Pause in Battery Saver** option and changed the default low-battery pause from 20% to
  10%.
- Reset the brightness taper after the array has been dark, so a newly armed effect starts at full
  brightness.
- Made renderer handoff explicit so only one renderer drives the array at a time.
- Changed ADB setup to a two-line reset-then-start flow, with separate commands for PowerShell and
  Windows Command Prompt.

## [1.0.2-experimental] - 2026-08-19

- Corrected the ADB command shown in the app's setup screen.
- Added automated tests for LED duty-cycle, taper, rest, and quiet-hours safety behavior.
- Hardened the release workflow, build verification, and contributor resources.

## [1.0.1-experimental]

- Added the unified HiLight Studio logo across the app and repository.

## [1.0.0-experimental]

- First experimental GitHub release.
