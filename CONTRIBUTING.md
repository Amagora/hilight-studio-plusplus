# Contributing

Thanks for improving HiLight Studio. The project is experimental and targets only the Pixel 11 Pro,
Pro XL, and Pro Fold on Android 17.

## Before opening a pull request

1. Keep changes focused and explain the user-visible effect.
2. Run `./gradlew :app:build :app:lint`.
3. For renderer, transport, or timing changes, test on a supported physical device and state the
   model, Android build, transport, and observed result in the pull request.
4. Do not weaken the limits in `Engine` without an explicit safety rationale and device evidence.

## Reporting bugs

Include the Pixel model, Android build, whether Shizuku or ADB is used, the selected pattern, and
steps to reproduce. Remove notification contents and other personal data from logs and screenshots.

By contributing, you agree that your contribution is provided under the MIT License.
