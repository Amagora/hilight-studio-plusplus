# Releasing HiLight Studio

Maintainers publish installable APKs through GitHub Releases. The signing key is intentionally kept
outside this repository and must never be committed.

1. Update `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Run `./gradlew --no-daemon :app:testDebugUnitTest :app:build :app:lint`.
3. Build the unsigned release APK with `./gradlew --no-daemon :app:assembleRelease`.
4. Zip-align and sign that APK with the private HiLight Studio release key, then verify it with
   `apksigner verify --verbose --print-certs`.
5. Record the APK SHA-256 in the GitHub release notes, create an annotated version tag, and upload
   the signed APK as a pre-release asset.

Never upload a debug-signed or unsigned APK as an official release asset.
